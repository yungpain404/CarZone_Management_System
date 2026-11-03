package audit;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.sql.CallableStatement;
import java.sql.PreparedStatement;
import java.sql.Statement;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Dynamic Proxy đánh chặn Statement, PreparedStatement và CallableStatement
 * để thu thập tham số, đo thời gian thực thi và ghi audit log tập trung.
 */
public class AuditStatementProxy implements InvocationHandler {

    private final Statement target;
    private final String sql;
    private final Map<Integer, Object> params = Collections.synchronizedMap(new LinkedHashMap<>());

    public AuditStatementProxy(Statement target, String sql) {
        this.target = target;
        this.sql = sql;
    }

    @SuppressWarnings("unchecked")
    public static <T extends Statement> T wrap(T statement, String sql) {
        if (statement == null) return null;
        Class<?>[] interfaces;
        if (statement instanceof CallableStatement) {
            interfaces = new Class<?>[]{CallableStatement.class};
        } else if (statement instanceof PreparedStatement) {
            interfaces = new Class<?>[]{PreparedStatement.class};
        } else {
            interfaces = new Class<?>[]{Statement.class};
        }

        return (T) Proxy.newProxyInstance(
                statement.getClass().getClassLoader(),
                interfaces,
                new AuditStatementProxy(statement, sql)
        );
    }

    @Override
    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
        String methodName = method.getName();

        // 1. Thu thập tham số PreparedStatement / CallableStatement
        if (methodName.startsWith("set") && args != null && args.length >= 2 && args[0] instanceof Integer idx) {
            Object val = args[1];
            params.put(idx, val);
        } else if ("clearParameters".equals(methodName)) {
            params.clear();
        }

        // 2. Đánh chặn các hàm thực thi SQL
        if ("executeUpdate".equals(methodName) || "execute".equals(methodName) || "executeQuery".equals(methodName) || "executeBatch".equals(methodName)) {
            String currentSql = this.sql;
            if (args != null && args.length > 0 && args[0] instanceof String sqlArg) {
                currentSql = sqlArg;
            }

            long start = System.currentTimeMillis();
            try {
                Object result = method.invoke(target, args);
                long duration = System.currentTimeMillis() - start;

                int affected = -1;
                if (result instanceof Integer count) {
                    affected = count;
                } else if (result instanceof int[] batchCounts) {
                    int sum = 0;
                    for (int c : batchCounts) {
                        if (c > 0) sum += c;
                    }
                    affected = sum;
                } else if ("execute".equals(methodName)) {
                    try {
                        affected = target.getUpdateCount();
                    } catch (Throwable ignored) {}
                }

                AuditLogger.logQuery(currentSql, params, affected, duration);
                return result;
            } catch (Throwable t) {
                long duration = System.currentTimeMillis() - start;
                Throwable cause = (t instanceof java.lang.reflect.InvocationTargetException ite) ? ite.getTargetException() : t;
                AuditLogger.logQueryFailure(currentSql, params, duration, cause);
                throw cause;
            }
        }

        // Các hàm thông thường chuyển tiếp trực tiếp
        try {
            return method.invoke(target, args);
        } catch (java.lang.reflect.InvocationTargetException ite) {
            throw ite.getTargetException();
        }
    }
}

