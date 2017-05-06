package test.db;

import com.firefly.db.init.ScriptUtils;
import com.firefly.utils.io.ClassPathResource;
import com.firefly.utils.io.EncodedResource;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

import static com.firefly.db.init.ScriptUtils.*;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class ScriptUtilsUnitTests {

    @Test
    public void splitSqlScriptDelimitedWithSemicolon() {
        String rawStatement1 = "insert into customer (id, name)\nvalues (1, 'Rod ; Johnson'), (2, 'Adrian \n Collier')";
        String cleanedStatement1 = "insert into customer (id, name) values (1, 'Rod ; Johnson'), (2, 'Adrian \n Collier')";
        String rawStatement2 = "insert into orders(id, order_date, customer_id)\nvalues (1, '2008-01-02', 2)";
        String cleanedStatement2 = "insert into orders(id, order_date, customer_id) values (1, '2008-01-02', 2)";
        String rawStatement3 = "insert into orders(id, order_date, customer_id) values (1, '2008-01-02', 2)";
        String cleanedStatement3 = "insert into orders(id, order_date, customer_id) values (1, '2008-01-02', 2)";
        char delim = ';';
        String script = rawStatement1 + delim + rawStatement2 + delim + rawStatement3 + delim;
        List<String> statements = new ArrayList<>();
        splitSqlScript(script, delim, statements);
        assertEquals("wrong number of statements", 3, statements.size());
        assertEquals("statement 1 not split correctly", cleanedStatement1, statements.get(0));
        assertEquals("statement 2 not split correctly", cleanedStatement2, statements.get(1));
        assertEquals("statement 3 not split correctly", cleanedStatement3, statements.get(2));
    }

    @Test
    public void splitSqlScriptDelimitedWithNewLine() {
        String statement1 = "insert into customer (id, name) values (1, 'Rod ; Johnson'), (2, 'Adrian \n Collier')";
        String statement2 = "insert into orders(id, order_date, customer_id) values (1, '2008-01-02', 2)";
        String statement3 = "insert into orders(id, order_date, customer_id) values (1, '2008-01-02', 2)";
        char delim = '\n';
        String script = statement1 + delim + statement2 + delim + statement3 + delim;
        List<String> statements = new ArrayList<>();
        splitSqlScript(script, delim, statements);
        assertEquals("wrong number of statements", 3, statements.size());
        assertEquals("statement 1 not split correctly", statement1, statements.get(0));
        assertEquals("statement 2 not split correctly", statement2, statements.get(1));
        assertEquals("statement 3 not split correctly", statement3, statements.get(2));
    }

    @Test
    public void splitSqlScriptDelimitedWithNewLineButDefaultDelimiterSpecified() {
        String statement1 = "do something";
        String statement2 = "do something else";
        char delim = '\n';
        String script = statement1 + delim + statement2 + delim;
        List<String> statements = new ArrayList<>();
        splitSqlScript(script, DEFAULT_STATEMENT_SEPARATOR, statements);
        assertEquals("wrong number of statements", 1, statements.size());
        assertEquals("script should have been 'stripped' but not actually 'split'", script.replace('\n', ' '),
                statements.get(0));
    }

    /**
     * See <a href="https://jira.spring.io/browse/SPR-13218">SPR-13218</a>
     */
    @Test
    public void splitScriptWithSingleQuotesNestedInsideDoubleQuotes() throws Exception {
        String statement1 = "select '1' as \"Dogbert's owner's\" from dual";
        String statement2 = "select '2' as \"Dilbert's\" from dual";
        char delim = ';';
        String script = statement1 + delim + statement2 + delim;
        List<String> statements = new ArrayList<>();
        splitSqlScript(script, ';', statements);
        assertEquals("wrong number of statements", 2, statements.size());
        assertEquals("statement 1 not split correctly", statement1, statements.get(0));
        assertEquals("statement 2 not split correctly", statement2, statements.get(1));
    }

    /**
     * See <a href="https://jira.spring.io/browse/SPR-11560">SPR-11560</a>
     */
    @Test
    public void readAndSplitScriptWithMultipleNewlinesAsSeparator() throws Exception {
        String script = readScript("db-test-data-multi-newline.sql");
        List<String> statements = new ArrayList<>();
        splitSqlScript(script, "\n\n", statements);

        String statement1 = "insert into T_TEST (NAME) values ('Keith')";
        String statement2 = "insert into T_TEST (NAME) values ('Dave')";

        assertEquals("wrong number of statements", 2, statements.size());
        assertEquals("statement 1 not split correctly", statement1, statements.get(0));
        assertEquals("statement 2 not split correctly", statement2, statements.get(1));
    }

    @Test
    public void readAndSplitScriptContainingComments() throws Exception {
        String script = readScript("test-data-with-comments.sql");
        List<String> statements = new ArrayList<>();
        splitSqlScript(script, ';', statements);

        String statement1 = "insert into customer (id, name) values (1, 'Rod; Johnson'), (2, 'Adrian Collier')";
        String statement2 = "insert into orders(id, order_date, customer_id) values (1, '2008-01-02', 2)";
        String statement3 = "insert into orders(id, order_date, customer_id) values (1, '2008-01-02', 2)";
        // Statement 4 addresses the error described in SPR-9982.
        String statement4 = "INSERT INTO persons( person_id , name) VALUES( 1 , 'Name' )";

        assertEquals("wrong number of statements", 4, statements.size());
        assertEquals("statement 1 not split correctly", statement1, statements.get(0));
        assertEquals("statement 2 not split correctly", statement2, statements.get(1));
        assertEquals("statement 3 not split correctly", statement3, statements.get(2));
        assertEquals("statement 4 not split correctly", statement4, statements.get(3));
    }

    /**
     * See <a href="https://jira.spring.io/browse/SPR-10330">SPR-10330</a>
     */
    @Test
    public void readAndSplitScriptContainingCommentsWithLeadingTabs() throws Exception {
        String script = readScript("test-data-with-comments-and-leading-tabs.sql");
        List<String> statements = new ArrayList<>();
        splitSqlScript(script, ';', statements);

        String statement1 = "insert into customer (id, name) values (1, 'Sam Brannen')";
        String statement2 = "insert into orders(id, order_date, customer_id) values (1, '2013-06-08', 1)";
        String statement3 = "insert into orders(id, order_date, customer_id) values (2, '2013-06-08', 1)";

        assertEquals("wrong number of statements", 3, statements.size());
        assertEquals("statement 1 not split correctly", statement1, statements.get(0));
        assertEquals("statement 2 not split correctly", statement2, statements.get(1));
        assertEquals("statement 3 not split correctly", statement3, statements.get(2));
    }

    /**
     * See <a href="https://jira.spring.io/browse/SPR-9531">SPR-9531</a>
     */
    @Test
    public void readAndSplitScriptContainingMuliLineComments() throws Exception {
        String script = readScript("test-data-with-multi-line-comments.sql");
        List<String> statements = new ArrayList<>();
        splitSqlScript(script, ';', statements);

        String statement1 = "INSERT INTO users(first_name, last_name) VALUES('Juergen', 'Hoeller')";
        String statement2 = "INSERT INTO users(first_name, last_name) VALUES( 'Sam' , 'Brannen' )";

        assertEquals("wrong number of statements", 2, statements.size());
        assertEquals("statement 1 not split correctly", statement1, statements.get(0));
        assertEquals("statement 2 not split correctly", statement2, statements.get(1));
    }

    @Test
    public void containsDelimiters() {
        assertTrue("test with ';' is wrong", !containsSqlScriptDelimiters("select 1\n select ';'", ";"));
        assertTrue("test with delimiter ; is wrong", containsSqlScriptDelimiters("select 1; select 2", ";"));
        assertTrue("test with '\\n' is wrong", !containsSqlScriptDelimiters("select 1; select '\\n\n';", "\n"));
        assertTrue("test with delimiter \\n is wrong", containsSqlScriptDelimiters("select 1\n select 2", "\n"));
    }

    private String readScript(String path) throws Exception {
        EncodedResource resource = new EncodedResource(new ClassPathResource(path, getClass()));
        return ScriptUtils.readScript(resource);
    }
}
