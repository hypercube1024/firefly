package com.firefly.db;

import java.io.InputStream;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.SQLException;
import java.util.Properties;

import javax.sql.DataSource;

import org.apache.commons.beanutils.BeanUtils;

import com.firefly.db.exception.DBException;
import com.firefly.utils.log.Log;
import com.firefly.utils.log.LogFactory;

/**
 * <p>数据库管理</p>
 * <p>维护数据库连接池，提供获取、释放连接方法</p>
 * 
 * @author 须俊杰
 * @version 1.0 2011-7-18
 */
public class DBManager {

	private final static Log log = LogFactory.getInstance().getLog("firefly-system");
    // 线程本地变量，用于存放当前线程获取的数据库连接
	private final static ThreadLocal<Connection> conns = new ThreadLocal<Connection>();
	// 数据源
	private static DataSource dataSource;
	// 是否开启数据库sql打印
	private static boolean show_sql = false;
	// 是否开启事务控制
	private static boolean transaction = true;

	static {
		initDataSource(null);
	}

	/**
	 * 初始化连接池
	 * @param dbProperties 数据库配置文件
	 */
	private final static void initDataSource(Properties dbProperties) {
		try {
			if (dbProperties == null) {
				dbProperties = new Properties();
				InputStream inputStream = DBManager.class.getResourceAsStream("/db.properties");
				dbProperties.load(inputStream);
				inputStream.close();
			}

			Properties cp_props = new Properties();
			for (Object key : dbProperties.keySet()) {
				String skey = (String) key;
				if (skey.startsWith("jdbc.")) {
					String name = skey.substring(5);
					cp_props.put(name, dbProperties.getProperty(skey));
					if ("show_sql".equalsIgnoreCase(name)) {
						show_sql = "true".equalsIgnoreCase(dbProperties
								.getProperty(skey));
					}
					if("transaction".equalsIgnoreCase(name)){
					    transaction = "true".equalsIgnoreCase(dbProperties
                                .getProperty(skey));
					}
				}
			}

			dataSource = (DataSource) Class.forName(
					cp_props.getProperty("datasource")).newInstance();
			log.info("Using DataSource : " + dataSource.getClass().getName());
			BeanUtils.populate(dataSource, cp_props);

			Connection conn = getConnection();
			DatabaseMetaData dmd = conn.getMetaData();
			log.info("Connected to " + dmd.getDatabaseProductName() + " "
					+ dmd.getDatabaseProductVersion());

			closeConnection();
		} catch (Exception e) {
			throw new DBException(e);
		}
	}

	/**
	 * 断开连接池
	 */
	public final static void closeDataSource() {
		try {
			dataSource.getClass().getMethod("close").invoke(dataSource);
		} catch (Exception e) {
			log.error("Unabled to destroy DataSource!!! ", e);
		}
	}

	/**
	 * 关闭连接
	 */
	public final static void closeConnection() {
		Connection conn = conns.get();
		try {
			if (conn != null && !conn.isClosed()) {
				conn.close();
			}
		} catch (SQLException e) {
			log.error("Unabled to close connection!!! ", e);
		}
		conns.set(null);
	}

	/**
	 * <p>获得一个数据库连接</p>
	 * 从数据库连接池中获取一个连接，将其保存在当前线程的“线程本地”变量中
	 * 
	 * @return 数据库连接
	 * @throws SQLException 如果数据库连接获取失败
	 */
	public final static Connection getConnection() throws SQLException {
		Connection conn = conns.get();
		if (conn == null || conn.isClosed()) {
			conn = dataSource.getConnection();
			conn.setAutoCommit(!transaction);
			conns.set(conn);
		}
//		ComboPooledDataSource cpds = (ComboPooledDataSource) dataSource;
//		log.info("datasourcename = " + cpds.getDataSourceName()
//				+ " ; NumConnections = " + cpds.getNumConnections()
//				+ " ; busy NumConnections = " + cpds.getNumBusyConnections());
		return (show_sql && !Proxy.isProxyClass(conn.getClass())) ? new _DebugConnection(
				conn).getConnection() : conn;
	}

	/**
	 * 事务提交
	 */
	public final static void commit(){
	    Connection conn = conns.get();
        try {
            if (conn != null && !conn.isClosed()) {
                conn.commit();
            }
        } catch (SQLException e) {
            log.error("Unabled to commit transaction!!! ", e);
        }
	}
	
	/**
	 * 事务回滚
	 */
	public final static void rollback(){
	    Connection conn = conns.get();
        try {
            if (conn != null && !conn.isClosed()) {
                conn.rollback();
            }
        } catch (SQLException e) {
            log.error("Unabled to rollback transaction!!! ", e);
        }
	}
	
	/**
	 * 用于跟踪执行的SQL语句
	 * 
	 * @author 须俊杰
	 * @version 1.0 2011-10-20
	 */
	static class _DebugConnection implements InvocationHandler {

		private final static Log log = LogFactory.getInstance().getLog("_DebugConnection");

		private Connection conn = null;

		public _DebugConnection(Connection conn) {
			this.conn = conn;
		}

		/**
		 * 获取数据库连接.
		 * 
		 * @return Connection 数据库连接
		 */
		public Connection getConnection() {
			return (Connection) Proxy.newProxyInstance(conn.getClass()
					.getClassLoader(), conn.getClass().getInterfaces(), this);
		}

		public Object invoke(Object proxy, Method m, Object[] args)
				throws Throwable {
			try {
				String method = m.getName();
				if ("prepareStatement".equals(method)
						|| "createStatement".equals(method))
					log.info("[SQL] >>> " + args[0]);
				return m.invoke(conn, args);
			} catch (InvocationTargetException e) {
				throw e.getTargetException();
			}
		}

	}
}
