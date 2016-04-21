package com.firefly.db;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
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

import com.firefly.db.annotation.Id;
import com.firefly.utils.Assert;
import com.firefly.utils.ReflectUtils;
import com.firefly.utils.StringUtils;
import com.firefly.utils.classproxy.ClassProxyFactoryUsingJavassist;
import com.firefly.utils.collection.ConcurrentReferenceHashMap;
import com.firefly.utils.function.Func2;
import com.firefly.utils.log.Log;
import com.firefly.utils.log.LogFactory;

public class JDBCHelper {

	private final static Log log = LogFactory.getInstance().getLog("firefly-system");

	protected final ConcurrentReferenceHashMap<Class<?>, String> idColumnNameCache = new ConcurrentReferenceHashMap<>(
			128);

	private final DataSource dataSource;
	private QueryRunner runner;
	private BeanProcessor defaultBeanProcessor = new DefaultBeanProcessor();

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

	public BeanProcessor getDefaultBeanProcessor() {
		return defaultBeanProcessor;
	}

	public void setDefaultBeanProcessor(BeanProcessor defaultBeanProcessor) {
		this.defaultBeanProcessor = defaultBeanProcessor;
	}

	public <T> T queryForSingleColumn(String sql, Object... params) {
		try (Connection connection = dataSource.getConnection()) {
			return this.queryForSingleColumn(connection, sql, params);
		} catch (SQLException e) {
			log.error("get connection exception", e);
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
			log.error("get connection exception", e);
			throw new DBException(e);
		}
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

	public String getIdColumnName(Class<?> t) {
		return idColumnNameCache.get(t, (key) -> {
			return _getIdColumnName(key);
		});
	}

	protected String _getIdColumnName(Class<?> t) {
		Field[] fields = t.getDeclaredFields();
		for (Field field : fields) {
			Id id = field.getAnnotation(Id.class);
			if (id != null) {
				if (StringUtils.hasText(id.value())) {
					return id.value();
				} else {
					return field.getName();
				}
			}
		}

		Method[] methods = t.getMethods();
		for (Method method : methods) {
			Id id = method.getAnnotation(Id.class);
			if (id != null) {
				if (StringUtils.hasText(id.value())) {
					return id.value();
				} else {
					return ReflectUtils.getPropertyName(method);
				}
			}
		}

		return null;
	}

	public <K, V> Map<K, V> queryForBeanMap(String sql, Class<V> t, Object... params) {
		return this.queryForBeanMap(sql, t, defaultBeanProcessor, params);
	}

	public <K, V> Map<K, V> queryForBeanMap(String sql, Class<V> t, BeanProcessor beanProcessor, Object... params) {
		String columnName = getIdColumnName(t);
		Assert.notNull(columnName);

		try (Connection connection = dataSource.getConnection()) {
			return this.queryForBeanMap(connection, sql, t, columnName, beanProcessor, params);
		} catch (SQLException e) {
			log.error("get connection exception", e);
			throw new DBException(e);
		}
	}

	public <K, V> Map<K, V> queryForBeanMap(Connection connection, String sql, Class<V> t, Object... params) {
		String columnName = getIdColumnName(t);
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
			log.error("get connection exception", e);
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
		try {
			return runner.update(sql, params);
		} catch (SQLException e) {
			log.error("update exception, sql: {}", e, sql);
			throw new DBException(e);
		}
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
		try(Connection connection = dataSource.getConnection()) {
			return this.insert(connection, sql, params);
		} catch (SQLException e) {
			log.error("insert exception, sql: {}", e, sql);
			throw new DBException(e);
		}
	}

	public <T> T insert(Connection connection, String sql, Object... params) {
		try {
			return runner.insert(connection, sql, new ScalarHandler<T>(), params);
		} catch (SQLException e) {
			log.error("insert exception, sql: {}", e, sql);
			throw new DBException(e);
		}
	}

	public <T> T executeTransaction(Func2<Connection, JDBCHelper, T> func) {
		Connection connection;
		try {
			connection = dataSource.getConnection();
			connection.setAutoCommit(false);
		} catch (SQLException e) {
			log.error("get connection exception", e);
			return null;
		}

		try {
			T ret = func.call(connection, this);
			connection.commit();
			return ret;
		} catch (Throwable t) {
			try {
				connection.rollback();
			} catch (SQLException e) {
				log.error("transaction rollback exception", t);
			}
			log.error("the transaction exception", t);
		} finally {
			try {
				connection.setAutoCommit(true);
			} catch (SQLException e) {
				log.error("the connection sets auto commit exception", e);
			}
			try {
				connection.close();
			} catch (SQLException e) {
				log.error("close connection exception", e);
			}
		}
		return null;
	}

}
