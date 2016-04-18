package test.db;

import java.sql.SQLException;

import org.junit.Before;
import org.junit.Test;

import static com.firefly.db.init.ScriptUtils.*;

/**
 * Integration tests for {@link ScriptUtils}.
 *
 * @see ScriptUtilsUnitTests
 */
public class ScriptUtilsIntegrationTests extends AbstractDatabaseInitializationTests {

	@Before
	public void setUpSchema() throws SQLException {
		executeSqlScript(jdbcHelper.getDataSource().getConnection(), usersSchema());
	}

	@Test
	public void executeSqlScriptContainingMuliLineComments() throws SQLException {
		executeSqlScript(jdbcHelper.getDataSource().getConnection(), resource("test-data-with-multi-line-comments.sql"));
		assertUsersDatabaseCreated("Hoeller", "Brannen");
	}

	/**
	 * @since 4.2
	 */
	@Test
	public void executeSqlScriptContainingSingleQuotesNestedInsideDoubleQuotes() throws SQLException {
		executeSqlScript(jdbcHelper.getDataSource().getConnection(), resource("users-data-with-single-quotes-nested-in-double-quotes.sql"));
		assertUsersDatabaseCreated("Hoeller", "Brannen");
	}

}
