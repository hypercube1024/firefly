package com.firefly.db;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;

import javax.sql.DataSource;

import org.apache.commons.dbutils.QueryRunner;
import org.apache.commons.dbutils.handlers.BeanHandler;
import org.apache.commons.dbutils.handlers.BeanListHandler;
import org.apache.commons.dbutils.handlers.ScalarHandler;

import com.firefly.utils.log.Log;
import com.firefly.utils.log.LogFactory;

public class JDBCHelper {

	private final static Log log = LogFactory.getInstance().getLog("firefly-system");

	private DataSource dataSource;
	private final QueryRunner runner = new QueryRunner();

	public JDBCHelper() {
	}

	public JDBCHelper(DataSource dataSource) {
		this.dataSource = dataSource;
	}

	public DataSource getDataSource() {
		return dataSource;
	}

	public void setDataSource(DataSource dataSource) {
		this.dataSource = dataSource;
	}

	public <T> T querySingleColumn(String sql, Object... params) {
		try (Connection connection = dataSource.getConnection()) {
			return runner.query(sql, new ScalarHandler<T>(), params);
		} catch (SQLException e) {
			log.error("query exception, sql: {}", e, sql);
			return null;
		}
	}

	public <T> T query(String sql, Class<T> t, Object... params) {
		try (Connection connection = dataSource.getConnection()) {
			return runner.query(sql, new BeanHandler<T>(t), params);
		} catch (SQLException e) {
			log.error("query exception, sql: {}", e, sql);
			return null;
		}
	}

	public <T> List<T> queryForList(String sql, Class<T> t, Object... params) {
		try (Connection connection = dataSource.getConnection()) {
			return runner.query(sql, new BeanListHandler<T>(t), params);
		} catch (SQLException e) {
			log.error("query exception, sql: {}", e, sql);
			return null;
		}
	}

}
