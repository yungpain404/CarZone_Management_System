package controller;

import javax.net.ssl.SSLSocket;
import javax.net.ssl.SSLSocketFactory;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Properties;

/** Sends recovery messages using Gmail SMTP with STARTTLS and an app password. */
final class GmailSmtpSender {
    enum Failure { AUTHENTICATION, CONNECTION, DELIVERY }
    static final class EmailException extends RuntimeException {
        private final Failure failure;
        EmailException(Failure failure) { super(failure.name()); this.failure = failure; }
        Failure failure() { return failure; }
    }
    private static final String HOST = "smtp.gmail.com";
    private final Properties localConfig = loadLocalConfig();
    private final String username = configuredValue("CARZONE_GMAIL_USERNAME");
    private final String appPassword = configuredValue("CARZONE_GMAIL_APP_PASSWORD");

    boolean isConfigured() { return validAddress(username) && nonBlank(appPassword); }

    void sendRecoveryCode(String recipient, String code) {
        if (!isConfigured()) throw new IllegalStateException("Gmail SMTP chưa được cấu hình.");
        if (!validAddress(username) || !validAddress(recipient) || recipient.contains("\r") || recipient.contains("\n"))
            throw new IllegalArgumentException("Địa chỉ email hồ sơ không hợp lệ.");
        try (Socket socket = new Socket()) {
            socket.connect(new InetSocketAddress(HOST, 587), 10_000);
            socket.setSoTimeout(15_000);
            BufferedReader reader = reader(socket);
            BufferedWriter writer = writer(socket);
            expect(readResponse(reader), 220);
            command(reader, writer, "EHLO carzone.local", 250);
            command(reader, writer, "STARTTLS", 220);

            SSLSocketFactory factory = (SSLSocketFactory) SSLSocketFactory.getDefault();
            SSLSocket tls = (SSLSocket) factory.createSocket(socket, HOST, 587, true);
            tls.startHandshake();
            reader = reader(tls);
            writer = writer(tls);
            command(reader, writer, "EHLO carzone.local", 250);
            command(reader, writer, "AUTH LOGIN", 334);
            command(reader, writer, b64(username.strip()), 334);
            command(reader, writer, b64(appPassword.replaceAll("\\s", "")), 235);
            command(reader, writer, "MAIL FROM:<" + username.strip() + ">", 250);
            command(reader, writer, "RCPT TO:<" + recipient.strip() + ">", 250, 251);
            command(reader, writer, "DATA", 354);

            String subject = b64("Mã xác thực khôi phục mật khẩu CarZone");
            String body = "Mã xác thực của bạn là: " + code + "\r\n\r\n"
                    + "Mã có hiệu lực trong 5 phút. Không chia sẻ mã này cho người khác.\r\n"
                    + "Nếu bạn không yêu cầu đặt lại mật khẩu, hãy bỏ qua email này.";
            String message = "From: CarZone <" + username.strip() + ">\r\n"
                    + "To: <" + recipient.strip() + ">\r\n"
                    + "Subject: =?UTF-8?B?" + subject + "?=\r\n"
                    + "MIME-Version: 1.0\r\n"
                    + "Content-Type: text/plain; charset=UTF-8\r\n"
                    + "Content-Transfer-Encoding: base64\r\n\r\n"
                    + wrap(b64(body)) + "\r\n.\r\n";
            writer.write(message); writer.flush(); expect(readResponse(reader), 250);
            try { command(reader, writer, "QUIT", 221); } catch (IOException ignored) { }
        } catch (IOException e) {
            String message = e.getMessage() == null ? "" : e.getMessage();
            Failure failure = message.contains("SMTP response code 535") || message.contains("SMTP response code 534")
                    || message.contains("SMTP response code 530")
                    ? Failure.AUTHENTICATION
                    : message.contains("SMTP response code") ? Failure.DELIVERY : Failure.CONNECTION;
            throw new EmailException(failure);
        }
    }

    private static void command(BufferedReader reader, BufferedWriter writer, String command, int... accepted) throws IOException {
        writer.write(command); writer.write("\r\n"); writer.flush(); expect(readResponse(reader), accepted);
    }
    private static String readResponse(BufferedReader reader) throws IOException {
        String line = reader.readLine();
        if (line == null || line.length() < 3) throw new IOException("SMTP response is incomplete");
        String code = line.substring(0, 3);
        String last = line;
        while (line.length() > 3 && line.charAt(3) == '-') {
            line = reader.readLine();
            if (line == null) throw new IOException("SMTP response is incomplete");
            last = line;
        }
        return code + " " + last;
    }
    private static void expect(String response, int... expected) throws IOException {
        int actual;
        try { actual = Integer.parseInt(response.substring(0,3)); }
        catch (RuntimeException e) { throw new IOException("Unexpected SMTP response"); }
        for (int code : expected) if (actual == code) return;
        throw new IOException("Unexpected SMTP response code " + actual);
    }
    private static BufferedReader reader(Socket socket) throws IOException { return new BufferedReader(new InputStreamReader(socket.getInputStream(), StandardCharsets.US_ASCII)); }
    private static BufferedWriter writer(Socket socket) throws IOException { return new BufferedWriter(new OutputStreamWriter(socket.getOutputStream(), StandardCharsets.US_ASCII)); }
    private static String b64(String value) { return Base64.getEncoder().encodeToString(value.getBytes(StandardCharsets.UTF_8)); }
    private static String wrap(String value) { StringBuilder out=new StringBuilder(); for(int i=0;i<value.length();i+=76){if(i>0)out.append("\r\n");out.append(value,i,Math.min(i+76,value.length()));}return out.toString(); }
    private static boolean nonBlank(String value) { return value != null && !value.isBlank(); }
    private String configuredValue(String key) {
        String environmentValue = System.getenv(key);
        if (nonBlank(environmentValue)) return environmentValue.strip();
        return localConfig.getProperty(key, "").strip();
    }
    private static Properties loadLocalConfig() {
        Properties properties = new Properties();
        Path path = Path.of("infra", "docker", "gmail.local.properties");
        if (!Files.isRegularFile(path)) return properties;
        try (var reader = Files.newBufferedReader(path, StandardCharsets.UTF_8)) {
            properties.load(reader);
        } catch (IOException ignored) {
            // A missing/unreadable local config is reported through isConfigured().
        }
        return properties;
    }
    private static boolean validAddress(String email) { return email != null && email.matches("(?i)^[a-z0-9.!#$%&'*+/=?^_`{|}~-]+@[a-z0-9](?:[a-z0-9-]*[a-z0-9])?(?:\\.[a-z0-9](?:[a-z0-9-]*[a-z0-9])?)+$"); }
}
