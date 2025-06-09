package main.java.io.github.adrielamoguis.utils.http_jdbc_driver;

import java.net.Authenticator;
import java.net.PasswordAuthentication;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.security.SecureRandom;
import java.security.cert.X509Certificate;
import java.sql.*;
import java.util.ArrayList;
import java.util.Properties;

import main.java.io.github.adrielamoguis.utils.http_jdbc_driver.dto.Request;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class HttpJdbcDriver implements Driver {

    private static final Logger LOGGER = LoggerFactory.getLogger(HttpJdbcDriver.class);

    private static final String URL_PREFIX = "jdbc:http://";

    private HttpClient httpClient;

    static {
        try {
            DriverManager.registerDriver(new HttpJdbcDriver());
        } catch (SQLException e) {
            throw new RuntimeException("Failed to register HttpJdbcDriver", e);
        }
    }

    /**
     * Attempts to make an HTTP connection to the given URL.
     * The driver should return "null" if it realizes it is the wrong kind
     * of driver to connect to the given URL.  This will be common, as when
     * the JDBC driver manager is asked to connect to a given URL it passes
     * the URL to each loaded driver in turn.
     *
     * <P>The driver should throw an {@code SQLException} if it is the right
     * driver to connect to the given URL but has trouble connecting to
     * the database.
     *
     * <P>The {@code Properties} argument can be used to pass
     * arbitrary string tag/value pairs as connection arguments.
     * Normally at least "user" and "password" properties should be
     * included in the {@code Properties} object.
     * <p>
     * <B>Note:</B> If a property is specified as part of the {@code url} and
     * is also specified in the {@code Properties} object, it is
     * implementation-defined as to which value will take precedence. For
     * maximum portability, an application should only specify a property once.
     *
     * @param url  the URL of the database to which to connect
     * @param info a list of arbitrary string tag/value pairs as
     *             connection arguments. Normally at least a "user" and
     *             "password" property should be included.
     * @return a {@code Connection} object that represents a
     * connection to the URL
     * @throws SQLException if a database access error occurs or the url is
     *                      {@code null}
     */
    @Override
    public Connection connect(String url, Properties info) throws SQLException {
        Matcher matcher = Pattern.compile("^" + URL_PREFIX + "(.*)$").matcher(url);
        if(!this.acceptsURL(url)) {
            return null; // Not the right driver for this URL
        }

        // Ensure all required properties are present
        if (info == null || !info.containsKey("user") || !info.containsKey("password")) {
            throw new SQLException("Missing required properties: 'username' and 'password'");
        }

        // Attempt to establish an HTTP connection
        matcher.matches();
        String endpoint = matcher.group(1);
        String username = info.getProperty("user");
        String password = info.getProperty("password");
        boolean disableSSL = Boolean.parseBoolean(info.getProperty("disableSSL", "false"));
        if(username == null || password == null) {
            throw new SQLException("Username and password must be provided in the properties");
        }
        try {
            HttpClient.Builder builder = HttpClient.newBuilder()
                .version(HttpClient.Version.HTTP_1_1)
                .authenticator(new Authenticator() {
                    @Override
                    protected PasswordAuthentication getPasswordAuthentication() {
                        return new PasswordAuthentication(username, password.toCharArray());
                    }
                });

            if (disableSSL) {
                builder.sslContext(createTrustAllSSLContext());
            }

            httpClient = builder.build();
            Request req = new Request("SELECT 1", new ArrayList<>());
            ObjectMapper mapper = new ObjectMapper();
            String jsonRequest = mapper.writeValueAsString(req);

            LOGGER.debug("Prepared test JSON request: {}", jsonRequest);

            LOGGER.info("Attempting to connect to HTTP endpoint: {}", endpoint);
            String endpointPrefix = disableSSL ? "https://" : "http://";
            HttpRequest testRequest = HttpRequest.newBuilder()
                .uri(java.net.URI.create(endpointPrefix + endpoint))
                .POST(HttpRequest.BodyPublishers.ofString(jsonRequest))
                .build();

            LOGGER.debug("Sending test request to endpoint: {}", testRequest.uri());
            httpClient.send(testRequest, java.net.http.HttpResponse.BodyHandlers.ofString());

            // Simulate a successful connection (you would replace this with actual logic)
            return new HttpJdbcConnection(httpClient, endpoint, disableSSL);
        } catch(Exception e) {
            throw new SQLException("Failed to connect to the database at " + endpoint, e);
        }
    }

    private static SSLContext createTrustAllSSLContext() throws Exception {
        SSLContext sslContext = SSLContext.getInstance("TLS");
        sslContext.init(null, new TrustManager[] {
                new X509TrustManager() {
                    public void checkClientTrusted(X509Certificate[] chain, String authType) {}
                    public void checkServerTrusted(X509Certificate[] chain, String authType) {}
                    public X509Certificate[] getAcceptedIssuers() { return new X509Certificate[0]; }
                }
        }, new SecureRandom());
        return sslContext;
    }

    /**
     * Retrieves whether the driver thinks that it can open a connection
     * to the given URL.  Typically drivers will return {@code true} if they
     * understand the sub-protocol specified in the URL and {@code false} if
     * they do not.
     *
     * @param url the URL of the database
     * @return {@code true} if this driver understands the given URL;
     * {@code false} otherwise
     * @throws SQLException if a database access error occurs or the url is
     *                      {@code null}
     */
    @Override
    public boolean acceptsURL(String url) throws SQLException {
        Matcher matcher = Pattern.compile("^" + URL_PREFIX + "(.*)$").matcher(url);
        return matcher.matches();
    }

    /**
     * Gets information about the possible properties for this driver.
     * <p>
     * The {@code getPropertyInfo} method is intended to allow a generic
     *
     * GUI tool to discover what properties it should prompt
     * a human for in order to get
     * enough information to connect to a database.  Note that depending on
     * the values the human has supplied so far, additional values may become
     * necessary, so it may be necessary to iterate though several calls
     * to the {@code getPropertyInfo} method.
     *
     * @param url  the URL of the database to which to connect
     * @param info a proposed list of tag/value pairs that will be sent on
     *             connect open
     * @return an array of {@code DriverPropertyInfo} objects describing
     * possible properties.  This array may be an empty array if
     * no properties are required.
     * @throws SQLException if a database access error occurs
     */
    @Override
    public DriverPropertyInfo[] getPropertyInfo(String url, Properties info) throws SQLException {
        DriverPropertyInfo[] propertyInfos = new DriverPropertyInfo[3];

        propertyInfos[0] = new DriverPropertyInfo("user", null);
        propertyInfos[0].description = "Database username";
        propertyInfos[0].required = true;

        propertyInfos[1] = new DriverPropertyInfo("password", null);
        propertyInfos[1].description = "Database password";
        propertyInfos[1].required = true;

        propertyInfos[2] = new DriverPropertyInfo("disableSSL", "false");
        propertyInfos[2].description = "Disable SSL verification (true/false)";
        propertyInfos[2].required = false;

        return propertyInfos;
    }

    /**
     * Retrieves the driver's major version number. Initially this should be 1.
     *
     * @return this driver's major version number
     */
    @Override
    public int getMajorVersion() {
        return 1;
    }

    /**
     * Gets the driver's minor version number. Initially this should be 0.
     *
     * @return this driver's minor version number
     */
    @Override
    public int getMinorVersion() {
        return 0;
    }

    /**
     * Reports whether this driver is a genuine JDBC
     * Compliant driver.
     * A driver may only report {@code true} here if it passes the JDBC
     * compliance tests; otherwise it is required to return {@code false}.
     * <p>
     * JDBC compliance requires full support for the JDBC API and full support
     * for SQL 92 Entry Level.  It is expected that JDBC compliant drivers will
     * be available for all the major commercial databases.
     * <p>
     * This method is not intended to encourage the development of non-JDBC
     * compliant drivers, but is a recognition of the fact that some vendors
     * are interested in using the JDBC API and framework for lightweight
     * databases that do not support full database functionality, or for
     * special databases such as document information retrieval where a SQL
     * implementation may not be feasible.
     *
     * @return {@code true} if this driver is JDBC Compliant; {@code false}
     * otherwise
     */
    @Override
    public boolean jdbcCompliant() {
        return false;
    }

    /**
     * Return the parent Logger of all the Loggers used by this driver. This
     * should be the Logger farthest from the root Logger that is
     * still an ancestor of all of the Loggers used by this driver. Configuring
     * this Logger will affect all of the log messages generated by the driver.
     * In the worst case, this may be the root Logger.
     *
     * @return the parent Logger for this driver
     * @throws SQLFeatureNotSupportedException if the driver does not use
     *                                         {@code java.util.logging}.
     * @since 1.7
     */
    @Override
    public java.util.logging.Logger getParentLogger() throws SQLFeatureNotSupportedException {
        throw new SQLFeatureNotSupportedException("java.util.logging not supported; use SLF4J instead.");
    }
}
