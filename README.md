# Http-JDBC-Driver

This is a side-project I wished I never had to make. TL;DR, corporate firewall blocks our PostgreSQL server and there's so much corporate crap to get it unblocked, but apparently now because our Azure subscription isn't owned by a specific department, we can't do anything about it. Thankfully, HTTP isn't blocked, and I noticed that Azure App Service can reach Azure PostgreSQL with no problems. So here I am, making an HTTP proxy to PostgreSQL. I hate my life.

If you're planning to use this in production, don't. This is in no way production-grade. If you're even considering it-- you need professional help.

## Installation
```bash
<maven here once deployed>
```

## Spring Data Source Configuration
`application.yml`
```yml
spring:
  datasource:
    url: jdbc:http://your-endpoint.com
    username: yourUsername
    password: yourPassword
    driver-class-name: main.java.io.github.adrielamoguis.utils.http_jdbc_driver.HttpJdbcDriver
    
  jpa:
    database-platform: org.hibernate.dialect.PostgreSQLDialect
    hibernate:
      ddl-auto: none
    show-sql: true

  data:
    jdbc:
      dialect: POSTGRESQL
```
Of course, you can customize it from POSTGRESQL to any other dialect/platform you may need. It's just what I have for my application. The `username` and `password` fields above are supposed to be HTTP Basic Authentication, so they are being passed as an HTTP header: `Authorization: Basic base64(username:password)`.

## Proxy Server Request Format
Your proxy server must accept the following JSON body:
```json
{
    "query": "<SQL>",
    "params": [<parameters>]
}
```

Example:
```json
{
    "query": "SELECT * FROM users WHERE user_id = $1",
    "params": [69420]
}
```
For a ready-made proxy server implementation, see [this link](about-blank) (will publish it to GitHub soon).

## Usage

The usage should be the same as your typical SpringBoot pattern with a model and JPA repository. There's at least a basic level of implementation that allows for this, but more complex calls may not be supported. *If you want to contribute, feel free to do so. Check [how to contribute](./CONTRIBUTING.md).* Check the SpringBoot application in this repository for how it's used.