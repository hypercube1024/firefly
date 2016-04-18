package test.db;

import org.junit.After;
import org.junit.Before;

import com.firefly.db.JDBCHelper;
import com.firefly.utils.io.ClassRelativeResourceLoader;
import com.firefly.utils.io.Resource;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;

import static org.hamcrest.Matchers.*;
import static org.junit.Assert.*;

/**
 * Abstract base class for integration tests involving database initialization.
 */
public abstract class AbstractDatabaseInitializationTests {

	private final ClassRelativeResourceLoader resourceLoader = new ClassRelativeResourceLoader(getClass());

	protected JDBCHelper jdbcHelper;


	@Before
	public void setUp() {
		HikariConfig config = new HikariConfig();
		config.setJdbcUrl("jdbc:h2:mem:testInit");
		config.setDriverClassName("org.h2.Driver");
		HikariDataSource ds = new HikariDataSource(config);
		jdbcHelper = new JDBCHelper(ds);
	}

	@After
	public void shutDown() {
		
	}

	Resource resource(String path) {
		return resourceLoader.getResource(path);
	}

	Resource defaultSchema() {
		return resource("db-schema.sql");
	}

	Resource usersSchema() {
		return resource("users-schema.sql");
	}

	void assertUsersDatabaseCreated(String... lastNames) {
		for (String lastName : lastNames) {
			Long count = jdbcHelper.queryForSingleColumn("select count(0) from users where last_name = ?", lastName);
			assertThat("Did not find user with last name [" + lastName + "].", count, equalTo(1L));
		}
	}

}
