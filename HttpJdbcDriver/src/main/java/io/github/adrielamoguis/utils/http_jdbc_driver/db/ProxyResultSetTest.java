package main.java.io.github.adrielamoguis.utils.http_jdbc_driver.db;

import org.junit.jupiter.api.Test;
import java.sql.SQLException;

import static org.junit.jupiter.api.Assertions.*;

class ProxyResultSetTest {

    @Test
    void testSimpleJsonArray() throws SQLException {
        String json = "[{\"id\":1,\"name\":\"Alice\"},{\"id\":2,\"name\":\"Bob\"}]";
        ProxyResultSet rs = new ProxyResultSet(json);

        assertTrue(rs.next());
        assertEquals(1, rs.getInt("id"));
        assertEquals("Alice", rs.getString("name"));

        assertTrue(rs.next());
        assertEquals(2, rs.getInt("id"));
        assertEquals("Bob", rs.getString("name"));

        assertFalse(rs.next());
    }

    @Test
    void testJsonArrayWithMissingKeys() throws SQLException {
        String json = "[{\"id\":1,\"name\":\"Alice\"},{\"id\":2}]";
        ProxyResultSet rs = new ProxyResultSet(json);

        assertTrue(rs.next());
        assertEquals(1, rs.getInt("id"));
        assertEquals("Alice", rs.getString("name"));

        assertTrue(rs.next());
        assertEquals(2, rs.getInt("id"));
        assertNull(rs.getString("name"));

        assertFalse(rs.next());
    }

    @Test
    void testEmptyJsonArray() throws SQLException {
        String json = "[]";
        ProxyResultSet rs = new ProxyResultSet(json);
        assertFalse(rs.next());
    }

    @Test
    void testInvalidJsonThrows() {
        String json = "{\"not\":\"an array\"}";
        assertThrows(RuntimeException.class, () -> new ProxyResultSet(json));
    }
}
