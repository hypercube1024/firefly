package com.firefly.db;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import javax.sql.DataSource;

import org.apache.commons.dbutils.BasicRowProcessor;
import org.apache.commons.dbutils.BeanProcessor;
import org.apache.commons.dbutils.QueryRunner;
import org.apache.commons.dbutils.handlers.BeanHandler;
import org.apache.commons.dbutils.handlers.BeanListHandler;
import org.apache.commons.dbutils.handlers.ScalarHandler;

import com.firefly.db.DefaultBeanProcessor.Mapper;
import com.firefly.db.DefaultBeanProcessor.SQLMapper;
import com.firefly.utils.Assert;
import com.firefly.utils.ReflectUtils;
import com.firefly.utils.classproxy.ClassProxyFactoryUsingJavassist;
import com.firefly.utils.function.Func2;
import com.firefly.utils.log.Log;
import com.firefly.utils.log.LogFactory;

public class JDBCHelper {

	private final static Log log = LogFactory.getInstance().getLog("firefly-system");

	private final DataSource dataSource;
	private QueryRunner runner;
	private DefaultBeanProcessor defaultBeanProcessor = new DefaultBeanProcessor();

	public JDBCHelper(DataSource dataSource) {
		this.dataSource = dataSource;
		if (log.isDebugEnabled()) {
			QueryRunner queryRunner = new QueryRunner(dataSource);
			try {
				this.runner = (QueryRunner) ClassProxyFactoryUsingJavassist.INSTANCE.createProxy(queryRunner,
						(handler, originalInstance, args) -> {

							if (args != null && args.length > 0) {
								String sql = null;
								String params = null;
								for (int i = 0; i < args.length; i++) {
									Object arg = args[i];
									if (arg instanceof String) {
										sql = (String) arg;
									}

									if (arg instanceof Object[]) {
										params = Arrays.toString((Object[]) arg);
									}
								}
								log.debug("the method {} will execute SQL [ {} | {} ]", handler.method().getName(), sql,
										params);
							}

							Object ret = handler.invoke(originalInstance, args);
							return ret;
						} , null);
			} catch (Throwable t) {
				this.runner = new QueryRunner(dataSource);
				log.error("create QueryRunner proxy exception", t);
			}
		} else {
			this.runner = new QueryRunner(dataSource);
		}
	}

	public DataSource getDataSource() {
		return dataSource;
	}

	public QueryRunner getRunner() {
		return runner;
	}

	public DefaultBeanProcessor getDefaultBeanProcessor() {
		return defaultBeanProcessor;
	}

	public void setDefaultBeanProcessor(DefaultBeanProcessor defaultBeanProcessor) {
		this.defaultBeanProcessor = defaultBeanProcessor;
	}

	public <T> T queryForSingleColumn(String sql, Object... params) {
		try (Connection connection = dataSource.getConnection()) {
			return this.queryForSingleColumn(connection, sql, params);
		} catch (SQLException e) {
			log.error("query exception, sql: {}", e, sql);
			throw new DBException(e);
		}
	}

	public <T> T queryForSingleColumn(Connection connection, String sql, Object... params) {
		try {
			return runner.query(connection, sql, new ScalarHandler<T>(), params);
		} catch (SQLException e) {
			log.error("query exception, sql: {}", e, sql);
			throw new DBException(e);
		}
	}

	public <T> T queryForObject(String sql, Class<T> t, Object... params) {
		return this.queryForObject(sql, t, defaultBeanProcessor, params);
	}

	public <T> T queryForObject(String sql, Class<T> t, BeanProcessor beanProcessor, Object... params) {
		try (Connection connection = dataSource.getConnection()) {
			return this.queryForObject(connection, sql, t, beanProcessor, params);
		} catch (SQLException e) {
			log.error("query exception, sql: {}", e, sql);
			throw new DBException(e);
		}
	}

	public <T> T queryById(Class<T> t, Object id) {
		try (Connection connection = dataSource.getConnection()) {
			return queryById(connection, t, id);
		} catch (SQLException e) {
			log.error("query exception", e);
			throw new DBException(e);
		}
	}

	public <T> T queryById(Connection connection, Class<T> t, Object id) {
		SQLMapper sqlMapper = defaultBeanProcessor.generateQuerySQL(t);
		Assert.notNull(sqlMapper, "sql mapper must not be null");
		return this.queryForObject(connection, sqlMapper.sql, t, id);
	}

	public <T> T queryForObject(Connection connection, String sql, Class<T> t, Object... params) {
		return this.queryForObject(connection, sql, t, defaultBeanProcessor, params);
	}

	public <T> T queryForObject(Connection connection, String sql, Class<T> t, BeanProcessor beanProcessor,
			Object... params) {
		try {
			return runner.query(connection, sql, new BeanHandler<T>(t, new BasicRowProcessor(beanProcessor)), params);
		} catch (SQLException e) {
			log.error("query exception, sql: {}", e, sql);
			throw new DBException(e);
		}
	}

	public <K, V> Map<K, V> queryForBeanMap(String sql, Class<V> t, Object... params) {
		return this.queryForBeanMap(sql, t, defaultBeanProcessor, params);
	}

	public <K, V> Map<K, V> queryForBeanMap(String sql, Class<V> t, BeanProcessor beanProcessor, Object... params) {
		String columnName = defaultBeanProcessor.getIdColumnName(t);
		Assert.notNull(columnName);

		try (Connection connection = dataSource.getConnection()) {
			return this.queryForBeanMap(connection, sql, t, columnName, beanProcessor, params);
		} catch (SQLException e) {
			log.error("query exception, sql: {}", e, sql);
			throw new DBException(e);
		}
	}

	public <K, V> Map<K, V> queryForBeanMap(Connection connection, String sql, Class<V> t, Object... params) {
		String columnName = defaultBeanProcessor.getIdColumnName(t);
		Assert.notNull(columnName);

		return this.queryForBeanMap(connection, sql, t, columnName, defaultBeanProcessor, params);
	}

	public <K, V> Map<K, V> queryForBeanMap(Connection connection, String sql, Class<V> t, String columnName,
			BeanProcessor beanProcessor, Object... params) {
		try {
			return runner.query(connection, sql,
					new DefaultBeanMapHandler<K, V>(t, new BasicRowProcessor(beanProcessor), 0, columnName), params);
		} catch (SQLException e) {
			log.error("query exception, sql: {}", e, sql);
			throw new DBException(e);
		}
	}

	public <T> List<T> queryForList(String sql, Class<T> t, Object... params) {
		return this.queryForList(sql, t, defaultBeanProcessor, params);
	}

	public <T> List<T> queryForList(String sql, Class<T> t, BeanProcessor beanProcessor, Object... params) {
		try (Connection connection = dataSource.getConnection()) {
			return this.queryForList(connection, sql, t, beanProcessor, params);
		} catch (SQLException e) {
			log.error("query exception, sql: {}", e, sql);
			throw new DBException(e);
		}
	}

	public <T> List<T> queryForList(Connection connection, String sql, Class<T> t, Object... params) {
		return this.queryForList(connection, sql, t, defaultBeanProcessor, params);
	}

	public <T> List<T> queryForList(Connection connection, String sql, Class<T> t, BeanProcessor beanProcessor,
			Object... params) {
		try {
			return runner.query(connection, sql, new BeanListHandler<T>(t, new BasicRowProcessor(beanProcessor)),
					params);
		} catch (SQLException e) {
			log.error("query exception, sql: {}", e, sql);
			throw new DBException(e);
		}
	}

	public int update(String sql, Object... params) {
		try (Connection connection = dataSource.getConnection()) {
			return this.update(connection, sql, params);
		} catch (SQLException e) {
			log.error("update exception, sql: {}", e, sql);
			throw new DBException(e);
		}
	}

	public int updateObject(Object object) {
		try (Connection connection = dataSource.getConnection()) {
			return this.updateObject(connection, object);
		} catch (SQLException e) {
			log.error("update exception", e);
			throw new DBException(e);
		}
	}

	public int updateObject(Connection connection, Object object) {
		Class<?> t = object.getClass();
		SQLMapper sqlMapper = defaultBeanProcessor.generateUpdateSQL(t, object);
		Assert.notNull(sqlMapper, "the sql mapper must not be null");
		Assert.notEmpty(sqlMapper.propertyMap, "the property map must not be empty");

		Object[] params = new Object[sqlMapper.propertyMap.size()];
		sqlMapper.propertyMap.forEach((property, index) -> {
			try {
				Object value = ReflectUtils.get(object, property);
				params[index] = value;
			} catch (Throwable e) {
			}
		});
		return this.update(connection, sqlMapper.sql, params);
	}

	public int update(Connection connection, String sql, Object... params) {
		try {
			return runner.update(connection, sql, params);
		} catch (SQLException e) {
			log.error("update exception, sql: {}", e, sql);
			throw new DBException(e);
		}
	}

	public <T> T insert(String sql, Object... params) {
		try (Connection connection = dataSource.getConnection()) {
			return this.insert(connection, sql, params);
		} catch (SQLException e) {
			log.error("insert exception, sql: {}", e, sql);
			throw new DBException(e);
		}
	}

	public <T> T insertObject(Object object) {
		try (Connection connection = dataSource.getConnection()) {
			return this.insertObject(connection, object);
		} catch (SQLException e) {
			log.error("insert exception", e);
			throw new DBException(e);
		}

	}

	public <T> T insertObject(Connection connection, Object object) {
		Class<?> t = object.getClass();
		SQLMapper sqlMapper = defaultBeanProcessor.generateInsertSQL(t);
		Assert.notNull(sqlMapper, "the sql mapper must not be null");
		Assert.notEmpty(sqlMapper.propertyMap, "the property map must not be empty");

		Object[] params = new Object[sqlMapper.propertyMap.size()];
		sqlMapper.propertyMap.forEach((property, index) -> {
			try {
				Object value = ReflectUtils.get(object, property);
				params[index] = value;
			} catch (Throwable e) {
			}
		});
		T ret = this.insert(connection, sqlMapper.sql, params);
		if (ret != null) {
			Mapper idMapper = defaultBeanProcessor.getIdMapper(t);
			if (idMapper != null) {
				try {
					ReflectUtils.set(object, idMapper.propertyName, ret);
				} catch (Throwable e) {
				}
			}
		}
		return ret;
	}

	public <T> T insert(Connection connection, String sql, Object... params) {
		try {
			return runner.insert(connection, sql, new ScalarHandler<T>(), params);
		} catch (SQLException e) {
			log.error("insert exception, sql: {}", e, sql);
			throw new DBException(e);
		}
	}

	public Connection getConnection() {
		try {
			return dataSource.getConnection();
		} catch (SQLException e) {
			log.error("get connection exception", e);
			throw new DBException(e);
		}
	}

	public void rollback(Connection connection) {
		try {
			connection.rollback();
		} catch (SQLException e) {
			log.error("transaction rollback exception", e);
			throw new DBException(e);
		}
	}

	public void setAutoCommit(Connection connection, boolean autoCommit) {
		try {
			connection.setAutoCommit(autoCommit);
		} catch (SQLException e) {
			log.error("set auto commit exception", e);
		}
	}

	public void commit(Connection connection) {
		try {
			connection.commit();
		} catch (SQLException e) {
			log.error("commit exception", e);
		}
	}

	public void close(Connection connection) {
		try {
			connection.close();
		} catch (SQLException e) {
			log.error("close connection exception", e);
		}
	}

	public <T> T executeTransaction(Func2<Connection, JDBCHelper, T> func) {
		Connection connection = getConnection();
		setAutoCommit(connection, false);

		try {
			T ret = func.call(connection, this);
			commit(connection);
			return ret;
		} catch (Throwable t) {
			rollback(connection);
			log.error("the transaction exception", t);
		} finally {
			setAutoCommit(connection, true);
			close(connection);
		}
		return null;
	}

}
