package test.db;

import org.junit.Before;
import org.junit.Test;

import java.sql.SQLException;

import static com.firefly.db.init.ScriptUtils.executeSqlScript;

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
