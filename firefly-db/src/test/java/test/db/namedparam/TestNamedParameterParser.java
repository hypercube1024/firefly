package test.db.namedparam;

import com.firefly.db.namedparam.NamedParameterParser;
import com.firefly.db.namedparam.ParsedSql;
import com.firefly.db.namedparam.PreparedSqlAndValues;
import org.junit.Assert;
import org.junit.Test;

import java.util.*;

import static org.hamcrest.Matchers.is;

/**
 * @author Pengtao Qiu
 */
public class TestNamedParameterParser {

    @Test
    public void test() {
        String sql = "xxx :a yyyy :b :c :a zzzzz";
        ParsedSql psql = NamedParameterParser.parseSqlStatement(sql);
        Assert.assertThat(4, is(psql.getTotalParameterCount()));
        Assert.assertThat(3, is(psql.getNamedParameterCount()));
        Assert.assertThat("a", is(psql.getParameterList().get(0).getParameterName()));
        Assert.assertThat("c", is(psql.getParameterList().get(2).getParameterName()));
        Assert.assertThat("a", is(psql.getParameterList().get(3).getParameterName()));
        PreparedSqlAndValues preparedSqlAndValues = NamedParameterParser.replaceParsedSql(psql, Collections.emptyMap());
        Assert.assertThat(preparedSqlAndValues.getPreparedSql(), is("xxx ? yyyy ? ? ? zzzzz"));

        String sql2 = "xxx &a yyyy ? zzzzz";
        ParsedSql psql2 = NamedParameterParser.parseSqlStatement(sql2);
        Assert.assertThat("a", is(psql.getParameterList().get(0).getParameterName()));
        Assert.assertThat(2, is(psql2.getTotalParameterCount()));
        Assert.assertThat(1, is(psql2.getNamedParameterCount()));

        String sql3 = "xxx &a+:b" + '\t' + ":c%10 yyyy ? zzzzz";
        ParsedSql psql3 = NamedParameterParser.parseSqlStatement(sql3);
        Assert.assertThat("a", is(psql3.getParameterList().get(0).getParameterName()));
        Assert.assertThat("b", is(psql3.getParameterList().get(1).getParameterName()));
        Assert.assertThat("c", is(psql3.getParameterList().get(2).getParameterName()));
        PreparedSqlAndValues preparedSqlAndValues3 = NamedParameterParser.replaceParsedSql(psql3, Collections.emptyMap());
        Assert.assertThat(preparedSqlAndValues3.getPreparedSql(), is("xxx ?+?\t?%10 yyyy ? zzzzz"));
    }

    @Test
    public void testGetPreparedSqlAndValues() {
        String sql = "select * from test where id in (:idList)";
        Map<String, Object> paramMap = new HashMap<>();
        paramMap.put("idList", Collections.singletonList(1));
        PreparedSqlAndValues preparedSqlAndValues = NamedParameterParser.getPreparedSqlAndValues(sql, paramMap);
        Assert.assertThat(preparedSqlAndValues.getPreparedSql(), is("select * from test where id in (?)"));
        Assert.assertThat(preparedSqlAndValues.getValues().size(), is(1));

        sql = "select * from test where id = :id";
        paramMap = new HashMap<>();
        paramMap.put("id", 1);
        preparedSqlAndValues = NamedParameterParser.getPreparedSqlAndValues(sql, paramMap);
        Assert.assertThat(preparedSqlAndValues.getPreparedSql(), is("select * from test where id = ?"));
        Assert.assertThat(preparedSqlAndValues.getValues().size(), is(1));

        sql = "select * from test where id = :{id}";
        paramMap = new HashMap<>();
        paramMap.put("id", 1);
        preparedSqlAndValues = NamedParameterParser.getPreparedSqlAndValues(sql, paramMap);
        Assert.assertThat(preparedSqlAndValues.getPreparedSql(), is("select * from test where id = ?"));
        Assert.assertThat(preparedSqlAndValues.getValues().size(), is(1));

        sql = "select * from test where id = &id";
        paramMap = new HashMap<>();
        paramMap.put("id", 1);
        preparedSqlAndValues = NamedParameterParser.getPreparedSqlAndValues(sql, paramMap);
        Assert.assertThat(preparedSqlAndValues.getPreparedSql(), is("select * from test where id = ?"));
        Assert.assertThat(preparedSqlAndValues.getValues().size(), is(1));

        sql = "select * from test where id = :id and name = :name and type in (:types)";
        TestParamObject paramObject = new TestParamObject();
        paramObject.setId(1);
        paramObject.setName("hello");
        paramObject.setTypes(Arrays.asList(1, 2, 3));
        preparedSqlAndValues = NamedParameterParser.getPreparedSqlAndValues(sql, paramObject);
        Assert.assertThat(preparedSqlAndValues.getPreparedSql(), is("select * from test where id = ? and name = ? and type in (?,?,?)"));
        Assert.assertThat(preparedSqlAndValues.getValues().size(), is(5));
    }

    public static class TestParamObject {
        private int id;
        private String name;
        private List<Integer> types;

        public int getId() {
            return id;
        }

        public void setId(int id) {
            this.id = id;
        }

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public List<Integer> getTypes() {
            return types;
        }

        public void setTypes(List<Integer> types) {
            this.types = types;
        }
    }
}
