package audit;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.sql.CallableStatement;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.Statement;

/**
 * Dynamic Proxy bọc java.sql.Connection để:
 * 1. Tự động bọc mọi PreparedStatement, CallableStatement, Statement bằng AuditStatementProxy.
 * 2. Đánh chặn và ghi log commit() / rollback() giao dịch.
 */
public class AuditConnectionProxy implements InvocationHandler {

    private final Connection target;

    public AuditConnectionProxy(Connection target) {
        this.target = target;
    }

    public static Connection wrap(Connection connection) {
        if (connection == null) return null;
        return (Connection) Proxy.newProxyInstance(
                connection.getClass().getClassLoader(),
                new Class<?>[]{Connection.class},
                new AuditConnectionProxy(connection)
        );
    }

    @Override
    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
        String methodName = method.getName();

        // 1. Đánh chặn commit()
        if ("commit".equals(methodName)) {
            try {
                Object res = method.invoke(target, args);
                AuditLogger.logTransaction("COMMIT", true, null);
                return res;
            } catch (Throwable t) {
                Throwable cause = (t instanceof java.lang.reflect.InvocationTargetException ite) ? ite.getTargetException() : t;
                AuditLogger.logTransaction("COMMIT", false, cause.getMessage());
                throw cause;
            }
        }

        // 2. Đánh chặn rollback()
        if ("rollback".equals(methodName)) {
            try {
                Object res = method.invoke(target, args);
                AuditLogger.logTransaction("ROLLBACK", true, null);
                return res;
            } catch (Throwable t) {
                Throwable cause = (t instanceof java.lang.reflect.InvocationTargetException ite) ? ite.getTargetException() : t;
                AuditLogger.logTransaction("ROLLBACK", false, cause.getMessage());
                throw cause;
            }
        }

        // 3. Đánh chặn prepareStatement / prepareCall / createStatement
        if ("prepareStatement".equals(methodName) && args != null && args.length > 0 && args[0] instanceof String sql) {
            try {
                PreparedStatement ps = (PreparedStatement) method.invoke(target, args);
                return AuditStatementProxy.wrap(ps, sql);
            } catch (java.lang.reflect.InvocationTargetException ite) {
                throw ite.getTargetException();
            }
        }

        if ("prepareCall".equals(methodName) && args != null && args.length > 0 && args[0] instanceof String sql) {
            try {
                CallableStatement cs = (CallableStatement) method.invoke(target, args);
                return AuditStatementProxy.wrap(cs, sql);
            } catch (java.lang.reflect.InvocationTargetException ite) {
                throw ite.getTargetException();
            }
        }

        if ("createStatement".equals(methodName)) {
            try {
                Statement st = (Statement) method.invoke(target, args);
                return AuditStatementProxy.wrap(st, null);
            } catch (java.lang.reflect.InvocationTargetException ite) {
                throw ite.getTargetException();
            }
        }

        // Các hàm khác chuyển tiếp nguyên bản
        try {
            return method.invoke(target, args);
        } catch (java.lang.reflect.InvocationTargetException ite) {
            throw ite.getTargetException();
        }
    }
}

