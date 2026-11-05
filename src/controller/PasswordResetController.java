package controller;

import dao.PasswordResetDAO;
import entity.TaiKhoan;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/** Coordinates one-time email codes and password recovery. Codes remain in process memory. */
public final class PasswordResetController {
    public enum SendResult { SENT, INVALID_USERNAME, MISSING_EMAIL, TOO_SOON, UNAVAILABLE, AUTHENTICATION_FAILED, CONNECTION_FAILED, DELIVERY_FAILED, FAILED }
    public enum ResetResult { VALIDATED, SUCCESS, INVALID_CODE, EXPIRED, LOCKED, FAILED }
    public record SendOutcome(SendResult result, String emailDaChe, boolean maHienTaiConDung) {}

    private record PendingCode(String maNV, String hash, Instant expiresAt, int attempts, boolean verified) {
        PendingCode withAttempt() { return new PendingCode(maNV, hash, expiresAt, attempts + 1, false); }
        PendingCode markVerified() { return new PendingCode(maNV, hash, expiresAt, attempts, true); }
    }
    private static final Map<String, PendingCode> CODES = new ConcurrentHashMap<>();
    private static final Map<String, Instant> LAST_SEND_ATTEMPT = new ConcurrentHashMap<>();
    private static final Object CODE_LOCK = new Object();
    private static final SecureRandom RANDOM = new SecureRandom();

    private final PasswordResetDAO dao = new PasswordResetDAO();
    private final GmailSmtpSender mailer = new GmailSmtpSender();

    public synchronized SendOutcome guiMaXacThuc(String tenDangNhapInput) {
        String tenDangNhap = chuanHoaTenDangNhap(tenDangNhapInput);
        if (tenDangNhap == null) return new SendOutcome(SendResult.INVALID_USERNAME, null, false);
        PasswordResetDAO.RecoveryTarget target = dao.timDichDenTheoMaNV(tenDangNhap);
        if (target == null) return new SendOutcome(SendResult.FAILED, null, false);
        String email = target.email();
        String masked = cheEmail(email);
        if (email == null || email.isBlank()) return new SendOutcome(SendResult.MISSING_EMAIL, masked, false);
        if (!mailer.isConfigured()) return new SendOutcome(SendResult.UNAVAILABLE, masked, false);

        Instant now = Instant.now();
        synchronized (CODE_LOCK) {
            Instant previous = LAST_SEND_ATTEMPT.get(target.maNV());
            if (previous != null && previous.plusSeconds(60).isAfter(now)) {
                PendingCode current=CODES.get(target.maNV());
                return new SendOutcome(SendResult.TOO_SOON, masked, current != null && current.expiresAt().isAfter(now));
            }
            LAST_SEND_ATTEMPT.put(target.maNV(), now);
        }
        String code = String.format("%06d", RANDOM.nextInt(1_000_000));
        PendingCode pending = new PendingCode(target.maNV(), bam(code), now.plusSeconds(300), 0, false);
        try {
            mailer.sendRecoveryCode(email, code);
            CODES.put(target.maNV(), pending);
            return new SendOutcome(SendResult.SENT, masked, true);
        } catch (RuntimeException e) {
            CODES.remove(target.maNV());
            synchronized (CODE_LOCK) { LAST_SEND_ATTEMPT.remove(target.maNV()); }
            if (e instanceof GmailSmtpSender.EmailException emailError) {
                return new SendOutcome(switch (emailError.failure()) {
                    case AUTHENTICATION -> SendResult.AUTHENTICATION_FAILED;
                    case CONNECTION -> SendResult.CONNECTION_FAILED;
                    case DELIVERY -> SendResult.DELIVERY_FAILED;
                }, masked, false);
            }
            return new SendOutcome(SendResult.FAILED, masked, false);
        }
    }

    public synchronized ResetResult xacThucMa(String tenDangNhapInput, String codeInput) {
        String tenDangNhap = chuanHoaTenDangNhap(tenDangNhapInput);
        if (tenDangNhap == null || codeInput == null || !codeInput.matches("\\d{6}")) return ResetResult.INVALID_CODE;
        PasswordResetDAO.RecoveryTarget target = dao.timDichDenTheoMaNV(tenDangNhap);
        if (target == null) return ResetResult.FAILED;
        synchronized (CODE_LOCK) {
            PendingCode pending=CODES.get(target.maNV());
            if (pending == null) return ResetResult.FAILED;
            Instant now=Instant.now();
            if (!pending.expiresAt().isAfter(now)) { CODES.remove(target.maNV()); return ResetResult.EXPIRED; }
            if (pending.attempts() >= 5) return ResetResult.LOCKED;
            if (!pending.hash().equalsIgnoreCase(bam(codeInput))) {
                PendingCode updated=pending.withAttempt();
                CODES.put(target.maNV(),updated);
                return updated.attempts() >= 5 ? ResetResult.LOCKED : ResetResult.INVALID_CODE;
            }
            CODES.put(target.maNV(), pending.markVerified());
            return ResetResult.VALIDATED;
        }
    }

    public synchronized ResetResult datLaiMatKhau(String tenDangNhapInput, String newPassword, String confirmPassword) {
        String tenDangNhap = chuanHoaTenDangNhap(tenDangNhapInput);
        if (tenDangNhap == null) return ResetResult.FAILED;
        if (!newPassword.equals(confirmPassword)) throw new IllegalArgumentException("Mật khẩu xác nhận không khớp.");
        TaiKhoan validated = new TaiKhoan();
        validated.setMatKhau(newPassword);
        PasswordResetDAO.RecoveryTarget target = dao.timDichDenTheoMaNV(tenDangNhap);
        if (target == null) return ResetResult.FAILED;
        synchronized (CODE_LOCK) {
            PendingCode pending=CODES.get(target.maNV());
            if (pending == null || !pending.verified()) return ResetResult.FAILED;
            Instant now=Instant.now();
            if (!pending.expiresAt().isAfter(now)) { CODES.remove(target.maNV()); return ResetResult.EXPIRED; }
            if (pending.attempts() >= 5) return ResetResult.LOCKED;
            if (!dao.datLaiMatKhau(target.maNV(),validated.getMatKhau())) return ResetResult.FAILED;
            CODES.remove(target.maNV());
            return ResetResult.SUCCESS;
        }
    }

    private static String chuanHoaTenDangNhap(String input) {
        if (input == null) return null;
        String value=input.strip().toUpperCase(java.util.Locale.ROOT);
        return value.matches("(CCH|TNV|KTV)\\d{3}") ? value : null;
    }
    private static String cheEmail(String email) {
        if (email == null || !email.contains("@")) return null;
        int at=email.lastIndexOf('@');
        String local=email.substring(0,at), domain=email.substring(at);
        if (local.length() <= 1) return local + "***" + domain;
        if (local.length() == 2) return local.charAt(0) + "***" + local.charAt(1) + domain;
        return local.charAt(0) + "***" + local.charAt(local.length()-1) + domain;
    }
    private static String bam(String value) {
        try {
            byte[] hash=MessageDigest.getInstance("SHA-256").digest(value.getBytes(StandardCharsets.UTF_8));
            StringBuilder out=new StringBuilder(64); for(byte b:hash)out.append(String.format("%02X",b)); return out.toString();
        } catch(Exception e) { throw new IllegalStateException("Không thể tạo mã xác thực.",e); }
    }
}
