package test.db;

import static org.hamcrest.Matchers.*;

import java.sql.SQLException;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import com.firefly.db.JDBCHelper;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;

public class TestAnnotation {

	private JDBCHelper jdbcHelper;
	private int size = 10;

	public TestAnnotation() {
		HikariConfig config = new HikariConfig();
		config.setJdbcUrl("jdbc:h2:mem:test");
		config.setDriverClassName("org.h2.Driver");
		HikariDataSource ds = new HikariDataSource(config);
		jdbcHelper = new JDBCHelper(ds);
	}

	@Before
	public void before() {
		try {
			jdbcHelper.getRunner().update(
					"CREATE TABLE user(id BIGINT AUTO_INCREMENT PRIMARY KEY, pt_name VARCHAR(255), pt_password VARCHAR(255))");

			for (int i = 1; i <= size; i++) {
				Long id = jdbcHelper.getRunner().insert("insert into user(pt_name, pt_password) values(?,?)", (rs) -> {
					return rs.next() ? rs.getLong(1) : -1;
				} , "test" + i, "test_pwd" + i);
				System.out.println("id:" + id);
			}

		} catch (SQLException e) {
			e.printStackTrace();
		}
	}

	@After
	public void after() {
		try {
			jdbcHelper.getRunner().update("DROP TABLE IF EXISTS user");
		} catch (SQLException e) {
			e.printStackTrace();
		}
	}

	@Test
	public void test() {
		for (int i = 1; i <= size; i++) {
			User user = jdbcHelper.queryForObject("select * from user where id = ?", User.class, i);
			System.out.println(user);
			Assert.assertThat(user.getName(), is("test" + i));
			Assert.assertThat(user.getPassword(), is("test_pwd" + i));
		}

	}

}
