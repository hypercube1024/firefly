package test.db;

import static org.hamcrest.Matchers.is;

import java.util.Map;

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
		jdbcHelper.update("CREATE TABLE user(id BIGINT AUTO_INCREMENT PRIMARY KEY, pt_name VARCHAR(255), pt_password VARCHAR(255))");

		for (int i = 1; i <= size; i++) {
			Long id = jdbcHelper.insert("insert into user(pt_name, pt_password) values(?,?)", "test" + i,
					"test_pwd" + i);
			System.out.println("id:" + id);
		}
	}

	@After
	public void after() {
		jdbcHelper.update("DROP TABLE IF EXISTS user");
	}

	@Test
	public void test() {
		for (long i = 1; i <= size; i++) {
			User user = jdbcHelper.queryForObject("select * from user where id = ?", User.class, i);
			Assert.assertThat(user.getId(), is(i));
			Assert.assertThat(user.getName(), is("test" + i));
			Assert.assertThat(user.getPassword(), is("test_pwd" + i));
		}

		Map<Long, User> map = jdbcHelper.queryForBeanMap("select * from user", User.class);
		Assert.assertThat(map.size(), is(size));

		for (long i = 1; i <= size; i++) {
			User user = map.get(i);
			Assert.assertThat(user.getId(), is(i));
			Assert.assertThat(user.getName(), is("test" + i));
			Assert.assertThat(user.getPassword(), is("test_pwd" + i));
		}

		Assert.assertThat(jdbcHelper.getIdColumnName(User.class), is("id"));
	}

}
