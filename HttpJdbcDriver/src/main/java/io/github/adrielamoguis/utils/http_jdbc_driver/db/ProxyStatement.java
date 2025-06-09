package io.github.adrielamoguis.utils.http_jdbc_driver.db;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.sql.*;

public class ProxyStatement implements Statement {
    private final Connection connection;
    private final HttpClient httpClient;
    private final String endpoint;
    private boolean closed = false;

    public ProxyStatement(Connection connection, HttpClient httpClient, String endpoint) {
        this.connection = connection;
        this.httpClient = httpClient;
        this.endpoint = endpoint;
    }

    @Override
    public ResultSet executeQuery(String sql) throws SQLException {
        checkClosed();
        try {
            String json = "{\"query\": " + escapeJson(sql) + ", \"params\": []}";
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
    public int executeUpdate(String sql) throws SQLException {
        checkClosed();
        try {
            String json = "{\"query\": " + escapeJson(sql) + ", \"params\": []}";
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(endpoint))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(json))
                    .build();
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                throw new SQLException("HTTP error: status code " + response.statusCode() + ", body: " + response.body());
            }
            // For simplicity, assume the response contains {"updateCount": N}
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
    public boolean execute(String sql) throws SQLException {
        checkClosed();
        // For simplicity, treat as query
        executeQuery(sql);
        return true;
    }

    @Override
    public void close() throws SQLException {
        closed = true;
    }

    @Override
    public boolean isClosed() throws SQLException {
        return closed;
    }

    // ...existing code for other Statement methods (throw SQLFeatureNotSupportedException or no-op)...

    private void checkClosed() throws SQLException {
        if (closed) throw new SQLException("Statement is closed");
    }

    private String escapeJson(String sql) {
        return "\"" + sql.replace("\\", "\\\\").replace("\"", "\\\"") + "\"";
    }

    // ...implement or stub other Statement methods as needed...
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
    @Override public void clearBatch() throws SQLException { throw new SQLFeatureNotSupportedException(); }
    @Override public int[] executeBatch() throws SQLException { throw new SQLFeatureNotSupportedException(); }
    @Override public Connection getConnection() throws SQLException { return connection; }
    @Override public boolean getMoreResults(int current) throws SQLException { return false; }
    @Override public ResultSet getGeneratedKeys() throws SQLException { throw new SQLFeatureNotSupportedException(); }
    @Override public int executeUpdate(String sql, int autoGeneratedKeys) throws SQLException { return executeUpdate(sql); }
    @Override public int executeUpdate(String sql, int[] columnIndexes) throws SQLException { return executeUpdate(sql); }
    @Override public int executeUpdate(String sql, String[] columnNames) throws SQLException { return executeUpdate(sql); }
    @Override public boolean execute(String sql, int autoGeneratedKeys) throws SQLException { return execute(sql); }
    @Override public boolean execute(String sql, int[] columnIndexes) throws SQLException { return execute(sql); }
    @Override public boolean execute(String sql, String[] columnNames) throws SQLException { return execute(sql); }
    @Override public int getResultSetHoldability() throws SQLException { return ResultSet.HOLD_CURSORS_OVER_COMMIT; }
    @Override public boolean isPoolable() throws SQLException { return false; }
    @Override public void setPoolable(boolean poolable) throws SQLException {}
    @Override public void closeOnCompletion() throws SQLException {}
    @Override public boolean isCloseOnCompletion() throws SQLException { return false; }
    @Override public <T> T unwrap(Class<T> iface) throws SQLException { throw new SQLException("Not a wrapper"); }
    @Override public boolean isWrapperFor(Class<?> iface) throws SQLException { return false; }
}