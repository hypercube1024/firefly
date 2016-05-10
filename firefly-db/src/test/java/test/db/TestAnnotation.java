package test.db;

import static org.hamcrest.Matchers.*;

import java.util.Map;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import com.firefly.db.DefaultBeanProcessor;
import com.firefly.db.DefaultBeanProcessor.Mapper;
import com.firefly.db.DefaultBeanProcessor.SQLMapper;
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
		config.setAutoCommit(false);
		HikariDataSource ds = new HikariDataSource(config);
		jdbcHelper = new JDBCHelper(ds);
	}

	@Before
	public void before() {
		jdbcHelper.update(
				"CREATE TABLE user(id BIGINT AUTO_INCREMENT PRIMARY KEY, pt_name VARCHAR(255), pt_password VARCHAR(255), other_info VARCHAR(255))");

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
	}
	
	@Test
	public void testInsertAndUpdateObject() {
		User user = new User();
		user.setName("hello");
		user.setPassword("world");
		user.setOtherInfo("test Other");
		Long id = jdbcHelper.insertObject(user);
		Assert.assertThat(user.getId(), notNullValue());
		
		User user1 = jdbcHelper.queryById(User.class, id);
		Assert.assertThat(user1.getName(), is("hello"));
		Assert.assertThat(user1.getPassword(), is("world"));
		Assert.assertThat(user1.getOtherInfo(), nullValue());
		
		User user2 = new User();
		user2.setId(id);
		user2.setName("hello 2");
		user2.setPassword("pwed");
		user.setOtherInfo("test Other2");
		int e = jdbcHelper.updateObject(user2);
		Assert.assertThat(e, is(1));
		
		User user3 = jdbcHelper.queryById(User.class, id);
		Assert.assertThat(user3.getName(), is("hello 2"));
		Assert.assertThat(user3.getPassword(), is("pwed"));
		Assert.assertThat(user3.getOtherInfo(), nullValue());
		
		int n = jdbcHelper.deleteById(User.class, id);
		Assert.assertThat(n, is(1));
		User user4 = jdbcHelper.queryById(User.class, id);
		Assert.assertThat(user4, nullValue());
		
		Long id2 = jdbcHelper.insert("insert into user(pt_name, pt_password, other_info) values(?,?,?)", "ptTest", "ptTestPwd", "testOtherInfo");
		User otherUser = jdbcHelper.queryForObject("select * from user where id = ?", User.class, id2);
		System.out.println(otherUser);
		Assert.assertThat(otherUser, notNullValue());
		Assert.assertThat(otherUser.getId(), is(id2));
		Assert.assertThat(otherUser.getName(), is("ptTest"));
		Assert.assertThat(otherUser.getPassword(), is("ptTestPwd"));
		Assert.assertThat(otherUser.getOtherInfo(), is("testOtherInfo"));
	}

	@Test
	public void testBeanProcessor() {
		DefaultBeanProcessor beanProcessor = jdbcHelper.getDefaultBeanProcessor();
		Assert.assertThat(beanProcessor.getIdColumnName(User.class), is("id"));

		Map<String, Mapper> mapper = beanProcessor.getMapper(User.class);
		Assert.assertThat(mapper.get("id").annotated, is(true));
		Assert.assertThat(mapper.get("name").annotated, is(true));
		Assert.assertThat(mapper.get("name").columnName, is("pt_name"));

		Assert.assertThat(mapper.get("password").columnName, is("pt_password"));

		Assert.assertThat(beanProcessor.getTableName(User.class), is("user"));
		
		SQLMapper sqlMapper = beanProcessor.generateInsertSQL(User.class);
		System.out.println(sqlMapper);
		
		sqlMapper = beanProcessor.generateQuerySQL(User.class);
		System.out.println(sqlMapper);
	}
}
