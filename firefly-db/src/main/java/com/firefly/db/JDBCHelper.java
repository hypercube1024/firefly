package com.firefly.db;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;

import javax.sql.DataSource;

import org.apache.commons.dbutils.BasicRowProcessor;
import org.apache.commons.dbutils.BeanProcessor;
import org.apache.commons.dbutils.GenerousBeanProcessor;
import org.apache.commons.dbutils.QueryRunner;
import org.apache.commons.dbutils.handlers.BeanHandler;
import org.apache.commons.dbutils.handlers.BeanListHandler;
import org.apache.commons.dbutils.handlers.ScalarHandler;

import com.firefly.utils.function.Func2;
import com.firefly.utils.log.Log;
import com.firefly.utils.log.LogFactory;

public class JDBCHelper {

	private final static Log log = LogFactory.getInstance().getLog("firefly-system");

	private final DataSource dataSource;
	private final QueryRunner runner;
	private BeanProcessor defaultBeanProcessor = new GenerousBeanProcessor();

	public JDBCHelper(DataSource dataSource) {
		this.dataSource = dataSource;
		this.runner = new QueryRunner(dataSource);
	}

	public DataSource getDataSource() {
		return dataSource;
	}

	public QueryRunner getRunner() {
		return runner;
	}

	public BeanProcessor getDefaultBeanProcessor() {
		return defaultBeanProcessor;
	}

	public void setDefaultBeanProcessor(BeanProcessor defaultBeanProcessor) {
		this.defaultBeanProcessor = defaultBeanProcessor;
	}

	public <T> T querySingleColumn(String sql, Object... params) {
		try {
			return runner.query(sql, new ScalarHandler<T>(), params);
		} catch (SQLException e) {
			log.error("query exception, sql: {}", e, sql);
			return null;
		}
	}

	public <T> T query(String sql, Class<T> t, Object... params) {
		return this.query(sql, t, defaultBeanProcessor, params);
	}

	public <T> T query(String sql, Class<T> t, BeanProcessor beanProcessor, Object... params) {
		try {
			return runner.query(sql, new BeanHandler<T>(t, new BasicRowProcessor(beanProcessor)), params);
		} catch (SQLException e) {
			log.error("query exception, sql: {}", e, sql);
			return null;
		}
	}

	public <T> List<T> queryForList(String sql, Class<T> t, Object... params) {
		return this.queryForList(sql, t, defaultBeanProcessor, params);
	}

	public <T> List<T> queryForList(String sql, Class<T> t, BeanProcessor beanProcessor, Object... params) {
		try {
			return runner.query(sql, new BeanListHandler<T>(t, new BasicRowProcessor(beanProcessor)), params);
		} catch (SQLException e) {
			log.error("query exception, sql: {}", e, sql);
			return null;
		}
	}

	public <T> T executeTransaction(Func2<Connection, QueryRunner, T> func) {
		try {
			Connection connection = dataSource.getConnection();
			connection.setAutoCommit(false);
			try {
				T ret = func.call(connection, runner);
				connection.commit();
				return ret;
			} catch (Throwable t) {
				connection.rollback();
				log.error("the transaction exception", t);
			} finally {
				connection.close();
			}
		} catch (SQLException e) {
			log.error("get connection exception", e);
		}
		return null;
	}

}
