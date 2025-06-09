package io.github.adrielamoguis.utils.http_jdbc_driver.driver_impl.db;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.sql.*;
import java.util.*;

public class ProxyPreparedStatement implements PreparedStatement {
    private final Connection connection;
    private final HttpClient httpClient;
    private final String endpoint;
    private final String sql;
    private boolean closed = false;
    private final List<Object> params = new ArrayList<>();

    public ProxyPreparedStatement(Connection connection, HttpClient httpClient, String endpoint, String sql) {
        this.connection = connection;
        this.httpClient = httpClient;
        this.endpoint = endpoint;
        this.sql = sql;
    }

    private void checkClosed() throws SQLException {
        if (closed) {
            throw new SQLException("Statement is closed");
        }
    }

    @Override
    public ResultSet executeQuery() throws SQLException {
        checkClosed();
        try {
            String json = buildJson();
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(endpoint))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(json))
                    .build();
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                throw new SQLException("HTTP error: status code " + response.statusCode() + ", body: " + response.body());
            }
            return new ProxyResultSet(response.body());
        } catch (Exception e) {
            throw new SQLException("HTTP error: " + e.getMessage(), e);
        }
    }

    @Override
    public int executeUpdate() throws SQLException {
        checkClosed();
        try {
            String json = buildJson();
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(endpoint))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(json))
                    .build();
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                throw new SQLException("HTTP error: status code " + response.statusCode() + ", body: " + response.body());
            }
            String body = response.body();
            int idx = body.indexOf("updateCount");
            if (idx >= 0) {
                int start = body.indexOf(":", idx) + 1;
                int end = body.indexOf("}", start);
                return Integer.parseInt(body.substring(start, end).trim());
            }
            return 0;
        } catch (Exception e) {
            throw new SQLException("HTTP error: " + e.getMessage(), e);
        }
    }

    @Override
    public boolean execute() throws SQLException {
        checkClosed();
        executeQuery();
        return true;
    }

    private String buildJson() {
        String formattedSql = formatSqlWithPostgresParams(sql, params.size());
        StringBuilder sb = new StringBuilder();
        sb.append("{\"query\": ");
        sb.append(escapeJson(formattedSql));
        sb.append(", \"params\": [");
        for (int i = 0; i < params.size(); i++) {
            Object param = params.get(i);
            if (i > 0) sb.append(", ");
            sb.append(paramToJson(param));
        }
        sb.append("]}");
        return sb.toString();
    }

    /**
     * Replace each '?' in the SQL with $1, $2, ... up to paramCount.
     */
    private String formatSqlWithPostgresParams(String sql, int paramCount) {
        StringBuilder sb = new StringBuilder();
        int paramIndex = 1;
        int last = 0;
        for (int i = 0; i < sql.length(); i++) {
            char c = sql.charAt(i);
            if (c == '?') {
                sb.append(sql, last, i);
                sb.append('$').append(paramIndex++);
                last = i + 1;
            }
        }
        sb.append(sql.substring(last));
        return sb.toString();
    }

    private String escapeJson(String s) {
        return "\"" + s.replace("\\", "\\\\").replace("\"", "\\\"") + "\"";
    }

    private String paramToJson(Object param) {
        if (param == null) return "null";
        if (param instanceof String) return escapeJson((String) param);
        if (param instanceof Number || param instanceof Boolean) return param.toString();
        return escapeJson(param.toString());
    }

    @Override
    public void setObject(int parameterIndex, Object x) throws SQLException {
        ensureParamsSize(parameterIndex);
        params.set(parameterIndex - 1, x);
    }

    @Override
    public void setString(int parameterIndex, String x) throws SQLException {
        setObject(parameterIndex, x);
    }

    @Override
    public void setInt(int parameterIndex, int x) throws SQLException {
        setObject(parameterIndex, x);
    }

    @Override
    public void setLong(int parameterIndex, long x) throws SQLException {
        setObject(parameterIndex, x);
    }

    @Override
    public void setBoolean(int parameterIndex, boolean x) throws SQLException {
        setObject(parameterIndex, x);
    }

    @Override
    public void setNull(int parameterIndex, int sqlType) throws SQLException {
        setObject(parameterIndex, null);
    }

    private void ensureParamsSize(int parameterIndex) {
        while (params.size() < parameterIndex) {
            params.add(null);
        }
    }

    @Override
    public void clearParameters() throws SQLException {
        params.clear();
    }

    @Override
    public void close() throws SQLException {
        closed = true;
    }

    @Override
    public boolean isClosed() throws SQLException {
        return closed;
    }

    // ...existing code for other PreparedStatement methods (throw SQLFeatureNotSupportedException or no-op)...

    @Override public ResultSet executeQuery(String sql) throws SQLException { throw new SQLFeatureNotSupportedException(); }
    @Override public int executeUpdate(String sql) throws SQLException { throw new SQLFeatureNotSupportedException(); }
    @Override public void setByte(int parameterIndex, byte x) throws SQLException { setObject(parameterIndex, x); }
    @Override public void setShort(int parameterIndex, short x) throws SQLException { setObject(parameterIndex, x); }
    @Override public void setFloat(int parameterIndex, float x) throws SQLException { setObject(parameterIndex, x); }
    @Override public void setDouble(int parameterIndex, double x) throws SQLException { setObject(parameterIndex, x); }
    @Override public void setBigDecimal(int parameterIndex, java.math.BigDecimal x) throws SQLException { setObject(parameterIndex, x); }
    @Override public void setBytes(int parameterIndex, byte[] x) throws SQLException { setObject(parameterIndex, Arrays.toString(x)); }
    @Override public void setDate(int parameterIndex, java.sql.Date x) throws SQLException { setObject(parameterIndex, x != null ? x.toString() : null); }
    @Override public void setTime(int parameterIndex, java.sql.Time x) throws SQLException { setObject(parameterIndex, x != null ? x.toString() : null); }
    @Override public void setTimestamp(int parameterIndex, java.sql.Timestamp x) throws SQLException { setObject(parameterIndex, x != null ? x.toString() : null); }
    @Override public void setAsciiStream(int parameterIndex, java.io.InputStream x, int length) throws SQLException { throw new SQLFeatureNotSupportedException(); }
    @Override public void setUnicodeStream(int parameterIndex, java.io.InputStream x, int length) throws SQLException { throw new SQLFeatureNotSupportedException(); }
    @Override public void setBinaryStream(int parameterIndex, java.io.InputStream x, int length) throws SQLException { throw new SQLFeatureNotSupportedException(); }
    @Override public void addBatch() throws SQLException { throw new SQLFeatureNotSupportedException(); }
    @Override public void clearBatch() throws SQLException { throw new SQLFeatureNotSupportedException(); }
    @Override public int[] executeBatch() throws SQLException { throw new SQLFeatureNotSupportedException(); }
    @Override public void setCharacterStream(int parameterIndex, java.io.Reader reader, int length) throws SQLException { throw new SQLFeatureNotSupportedException(); }
    @Override public void setRef(int parameterIndex, Ref x) throws SQLException { throw new SQLFeatureNotSupportedException(); }
    @Override public void setBlob(int parameterIndex, Blob x) throws SQLException { throw new SQLFeatureNotSupportedException(); }
    @Override public void setClob(int parameterIndex, Clob x) throws SQLException { throw new SQLFeatureNotSupportedException(); }
    @Override public void setArray(int parameterIndex, Array x) throws SQLException { throw new SQLFeatureNotSupportedException(); }
    @Override public ResultSetMetaData getMetaData() throws SQLException { throw new SQLFeatureNotSupportedException(); }
    @Override public void setDate(int parameterIndex, java.sql.Date x, Calendar cal) throws SQLException { setDate(parameterIndex, x); }
    @Override public void setTime(int parameterIndex, java.sql.Time x, Calendar cal) throws SQLException { setTime(parameterIndex, x); }
    @Override public void setTimestamp(int parameterIndex, java.sql.Timestamp x, Calendar cal) throws SQLException { setTimestamp(parameterIndex, x); }
    @Override public void setNull(int parameterIndex, int sqlType, String typeName) throws SQLException { setNull(parameterIndex, sqlType); }
    @Override public void setURL(int parameterIndex, java.net.URL x) throws SQLException { setObject(parameterIndex, x != null ? x.toString() : null); }
    @Override public ParameterMetaData getParameterMetaData() throws SQLException { throw new SQLFeatureNotSupportedException(); }
    @Override public void setRowId(int parameterIndex, RowId x) throws SQLException { throw new SQLFeatureNotSupportedException(); }
    @Override public void setNString(int parameterIndex, String value) throws SQLException { setString(parameterIndex, value); }
    @Override public void setNCharacterStream(int parameterIndex, java.io.Reader value, long length) throws SQLException { throw new SQLFeatureNotSupportedException(); }
    @Override public void setNClob(int parameterIndex, NClob value) throws SQLException { throw new SQLFeatureNotSupportedException(); }
    @Override public void setSQLXML(int parameterIndex, SQLXML xmlObject) throws SQLException { throw new SQLFeatureNotSupportedException(); }
    @Override public void setObject(int parameterIndex, Object x, int targetSqlType) throws SQLException { setObject(parameterIndex, x); }
    @Override public void setObject(int parameterIndex, Object x, int targetSqlType, int scaleOrLength) throws SQLException { setObject(parameterIndex, x); }
    @Override public void setAsciiStream(int parameterIndex, java.io.InputStream x, long length) throws SQLException { throw new SQLFeatureNotSupportedException(); }
    @Override public void setBinaryStream(int parameterIndex, java.io.InputStream x, long length) throws SQLException { throw new SQLFeatureNotSupportedException(); }
    @Override public void setCharacterStream(int parameterIndex, java.io.Reader reader, long length) throws SQLException { throw new SQLFeatureNotSupportedException(); }
    @Override public void setAsciiStream(int parameterIndex, java.io.InputStream x) throws SQLException { throw new SQLFeatureNotSupportedException(); }
    @Override public void setBinaryStream(int parameterIndex, java.io.InputStream x) throws SQLException { throw new SQLFeatureNotSupportedException(); }
    @Override public void setCharacterStream(int parameterIndex, java.io.Reader reader) throws SQLException { throw new SQLFeatureNotSupportedException(); }
    @Override public void setNCharacterStream(int parameterIndex, java.io.Reader value) throws SQLException { throw new SQLFeatureNotSupportedException(); }
    @Override public void setClob(int parameterIndex, java.io.Reader reader, long length) throws SQLException { throw new SQLFeatureNotSupportedException(); }
    @Override public void setClob(int parameterIndex, java.io.Reader reader) throws SQLException { throw new SQLFeatureNotSupportedException(); }
    @Override public void setBlob(int parameterIndex, java.io.InputStream inputStream, long length) throws SQLException { throw new SQLFeatureNotSupportedException(); }
    @Override public void setBlob(int parameterIndex, java.io.InputStream inputStream) throws SQLException { throw new SQLFeatureNotSupportedException(); }
    @Override public void setNClob(int parameterIndex, java.io.Reader reader, long length) throws SQLException { throw new SQLFeatureNotSupportedException(); }
    @Override public void setNClob(int parameterIndex, java.io.Reader reader) throws SQLException { throw new SQLFeatureNotSupportedException(); }
    @Override public int executeUpdate(String sql, int autoGeneratedKeys) throws SQLException { throw new SQLFeatureNotSupportedException(); }
    @Override public boolean execute(String sql, int autoGeneratedKeys) throws SQLException { throw new SQLFeatureNotSupportedException(); }
    @Override public int getMaxFieldSize() throws SQLException { return 0; }
    @Override public void setMaxFieldSize(int max) throws SQLException {}
    @Override public int getMaxRows() throws SQLException { return 0; }
    @Override public void setMaxRows(int max) throws SQLException {}
    @Override public void setEscapeProcessing(boolean enable) throws SQLException {}
    @Override public int getQueryTimeout() throws SQLException { return 0; }
    @Override public void setQueryTimeout(int seconds) throws SQLException {}
    @Override public void cancel() throws SQLException {}
    @Override public SQLWarning getWarnings() throws SQLException { return null; }
    @Override public void clearWarnings() throws SQLException {}
    @Override public void setCursorName(String name) throws SQLException {}

    /**
     * Executes the given SQL statement, which may return multiple results.
     * In some (uncommon) situations, a single SQL statement may return
     * multiple result sets and/or update counts.  Normally you can ignore
     * this unless you are (1) executing a stored procedure that you know may
     * return multiple results or (2) you are dynamically executing an
     * unknown SQL string.
     * <p>
     * The {@code execute} method executes an SQL statement and indicates the
     * form of the first result.  You must then use the methods
     * {@code getResultSet} or {@code getUpdateCount}
     * to retrieve the result, and {@code getMoreResults} to
     * move to any subsequent result(s).
     * <p>
     * <strong>Note:</strong>This method cannot be called on a
     * {@code PreparedStatement} or {@code CallableStatement}.
     *
     * @param sql any SQL statement
     * @return {@code true} if the first result is a {@code ResultSet}
     * object; {@code false} if it is an update count or there are
     * no results
     * @throws SQLException        if a database access error occurs,
     *                             this method is called on a closed {@code Statement},
     *                             the method is called on a
     *                             {@code PreparedStatement} or {@code CallableStatement}
     * @throws SQLTimeoutException when the driver has determined that the
     *                             timeout value that was specified by the {@code setQueryTimeout}
     *                             method has been exceeded and has at least attempted to cancel
     *                             the currently running {@code Statement}
     * @see #getResultSet
     * @see #getUpdateCount
     * @see #getMoreResults
     */
    @Override
    public boolean execute(String sql) throws SQLException {
        return false;
    }

    @Override public ResultSet getResultSet() throws SQLException { return null; }
    @Override public int getUpdateCount() throws SQLException { return -1; }
    @Override public boolean getMoreResults() throws SQLException { return false; }
    @Override public void setFetchDirection(int direction) throws SQLException {}
    @Override public int getFetchDirection() throws SQLException { return ResultSet.FETCH_FORWARD; }
    @Override public void setFetchSize(int rows) throws SQLException {}
    @Override public int getFetchSize() throws SQLException { return 0; }
    @Override public int getResultSetConcurrency() throws SQLException { return ResultSet.CONCUR_READ_ONLY; }
    @Override public int getResultSetType() throws SQLException { return ResultSet.TYPE_FORWARD_ONLY; }
    @Override public void addBatch(String sql) throws SQLException { throw new SQLFeatureNotSupportedException(); }
    @Override public Connection getConnection() throws SQLException { return connection; }
    @Override public boolean getMoreResults(int current) throws SQLException { return false; }
    @Override public ResultSet getGeneratedKeys() throws SQLException { throw new SQLFeatureNotSupportedException(); }
    @Override public int executeUpdate(String sql, int[] columnIndexes) throws SQLException { throw new SQLFeatureNotSupportedException(); }
    @Override public int executeUpdate(String sql, String[] columnNames) throws SQLException { throw new SQLFeatureNotSupportedException(); }
    @Override public boolean execute(String sql, int[] columnIndexes) throws SQLException { throw new SQLFeatureNotSupportedException(); }
    @Override public boolean execute(String sql, String[] columnNames) throws SQLException { throw new SQLFeatureNotSupportedException(); }
    @Override public int getResultSetHoldability() throws SQLException { return ResultSet.HOLD_CURSORS_OVER_COMMIT; }
    @Override public boolean isPoolable() throws SQLException { return false; }
    @Override public void setPoolable(boolean poolable) throws SQLException {}
    @Override public void closeOnCompletion() throws SQLException {}
    @Override public boolean isCloseOnCompletion() throws SQLException { return false; }
    @Override public <T> T unwrap(Class<T> iface) throws SQLException { throw new SQLException("Not a wrapper"); }
    @Override public boolean isWrapperFor(Class<?> iface) throws SQLException { return false; }
}