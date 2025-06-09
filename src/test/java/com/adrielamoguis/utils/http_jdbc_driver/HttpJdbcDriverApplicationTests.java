package com.adrielamoguis.utils.http_jdbc_driver;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;

@SpringBootTest
class HttpJdbcDriverApplicationTests {

	@Autowired
	JdbcTemplate jdbcTemplate;

	@Test
	void contextLoads() {
	}

	@Test
	void testCustomHttpDriver() {
		String sql = "SELECT 1";
		Integer result = jdbcTemplate.queryForObject(sql, Integer.class);
		System.out.println("Result: " + result);
		assert result != null && result == 1;
	}

}
