package test.db;

import static org.hamcrest.Matchers.is;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import com.firefly.db.TransactionalJDBCHelper;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;

public class TestTransactionalJdbcHelper {

	private TransactionalJDBCHelper jdbcHelper;
	private int size = 10;

	public TestTransactionalJdbcHelper() {
		HikariConfig config = new HikariConfig();
		config.setJdbcUrl("jdbc:h2:mem:test");
		config.setDriverClassName("org.h2.Driver");
		config.setAutoCommit(false);
		HikariDataSource ds = new HikariDataSource(config);
		jdbcHelper = new TransactionalJDBCHelper(ds);
	}

	@Before
	public void before() {
		jdbcHelper.update(
				"CREATE TABLE user(id BIGINT AUTO_INCREMENT PRIMARY KEY, pt_name VARCHAR(255), pt_password VARCHAR(255), other_info VARCHAR(255))");

		Object[][] params = new Object[10][2];
		for (int i = 0; i < size; i++) {
			params[i][0] = "test transaction " + i;
			params[i][1] = "pwd transaction " + i;
		}
		jdbcHelper.batch("insert into user(pt_name, pt_password) values(?,?)", params);
	}

	@Test
	public void test() {
		for (long i = 1; i <= size; i++) {
			User user = jdbcHelper.queryById(User.class, i);
			Assert.assertThat(user.getId(), is(i));
			Assert.assertThat(user.getName(), is("test transaction " + (i - 1)));
			Assert.assertThat(user.getPassword(), is("pwd transaction " + (i - 1)));
		}
	}

	@Test
	public void testRollback() {
		Long id = 1L;
		int r = jdbcHelper.executeTransaction((helper) -> {
			User user = new User();
			user.setId(1L);
			user.setName("apple");
			int row = helper.updateObject(user);
			Assert.assertThat(row, is(1));

			User user1 = helper.queryById(User.class, id);
			Assert.assertThat(user1.getName(), is("apple"));
			helper.rollback();
			return 0;
		});

		Assert.assertThat(r, is(0));
		User user2 = jdbcHelper.queryById(User.class, id);
		Assert.assertThat(user2.getName(), is("test transaction 0"));
	}

	@Test
	public void testRollback2() {
		int ret = jdbcHelper.executeTransaction((helper0) -> {
			User user0 = new User();
			user0.setId(2L);
			user0.setName("orange");
			int row0 = helper0.updateObject(user0);
			Assert.assertThat(row0, is(1));

			user0 = helper0.queryById(User.class, 2L);
			System.out.println(user0);
			Assert.assertThat(user0.getName(), is("orange"));

			int r = jdbcHelper.executeTransaction((helper) -> {
				User user1 = new User();
				user1.setId(1L);
				user1.setName("apple");
				int row = helper.updateObject(user1);
				Assert.assertThat(row, is(1));

				user1 = helper.queryById(User.class, 1L);
				System.out.println(user1);
				Assert.assertThat(user1.getName(), is("apple"));
				helper.rollback();
				return 0;
			});
			Assert.assertThat(r, is(0));
			return 0;
		});

		Assert.assertThat(ret, is(0));
		
		User user1 = jdbcHelper.queryById(User.class, 1L);
		Assert.assertThat(user1.getName(), is("test transaction 0"));

		User user2 = jdbcHelper.queryById(User.class, 2L);
		Assert.assertThat(user2.getName(), is("test transaction 1"));
	}

	@After
	public void after() {
		jdbcHelper.update("DROP TABLE IF EXISTS user");
	}
}
