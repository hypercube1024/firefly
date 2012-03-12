package com.firefly.db;

import java.math.BigInteger;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.dbutils.QueryRunner;
import org.apache.commons.dbutils.handlers.BeanHandler;
import org.apache.commons.dbutils.handlers.BeanListHandler;
import org.apache.commons.dbutils.handlers.ColumnListHandler;
import org.apache.commons.dbutils.handlers.ScalarHandler;

/**
 * <p>数据库查询助手</p>
 * 提供数据库基本的增删改查操作
 * 
 * @author 须俊杰
 * @version 1.0 2011-6-22
 */
@SuppressWarnings("unchecked")
public class QueryHelper {

	private final static QueryRunner _runner = new QueryRunner();

	/**
	 * 返回单一列时用到的handler
	 */
	private final static ScalarHandler _scaleHandler = new ScalarHandler() {
		@Override
		public Object handle(ResultSet rs) throws SQLException {
			Object obj = super.handle(rs);
			if (obj instanceof BigInteger)
				return ((BigInteger) obj).longValue();
			return obj;
		}
	};

	private final static ColumnListHandler _columnListHandler = new ColumnListHandler(){
		@Override
		protected Object handleRow(ResultSet rs) throws SQLException {
			Object obj = super.handleRow(rs);
			if(obj instanceof BigInteger)
				return ((BigInteger)obj).longValue();
			return obj;
		}
		
	};
	
	/**
	 * 判断是否为原始类型
	 * @param clazz
	 * @return
	 */
	private final static boolean _IsPrimitive(Class<?> clazz) {
		return clazz.isPrimitive() || PrimitiveClasses.contains(clazz);
	}

	@SuppressWarnings("serial")
	private final static List<Class<?>> PrimitiveClasses = new ArrayList<Class<?>>() {
		{
			add(Long.class);
			add(Integer.class);
			add(String.class);
			add(java.util.Date.class);
			add(java.sql.Date.class);
			add(java.sql.Timestamp.class);
		}
	};

	/**
	 * 获取数据库连接
	 * 
	 * @return 数据连接
	 * @throws SQLException 如果获取数据库连接失败
	 */
	private static Connection getConnection() throws SQLException {
		return DBManager.getConnection();
	}

	/**
	 * 读取某个对象
	 * @param beanClass 需要返回的对象类型
	 * @param sql 数据库sql语句
	 * @param params sql中的参数
	 * @return 查询结果
	 * @throws SQLException 数据库出错时抛出异常
	 */
	public static <T> T read(Class<T> beanClass, String sql, Object... params) throws SQLException {
	    // 判断是否为原始类型，如果是就用_scaleHandler，否则根据beanClass参数生成一个BeanHandler
        return (T) _runner.query(getConnection(), sql,
                _IsPrimitive(beanClass) ? _scaleHandler
                        : new BeanHandler<T>(beanClass), params);
	}

	/**
	 * 读取对象列表
	 * @param beanClass 需要返回的对象类型
	 * @param sql 数据库sql语句
	 * @param params sql中的参数
	 * @return 查询结果(列表)
	 * @throws SQLException 数据库出错时抛出异常
	 */
	@SuppressWarnings("rawtypes")
	public static <T> List<T> query(Class<T> beanClass, String sql,
			Object... params) throws SQLException {
	    return (List<T>) _runner.query(getConnection(), sql,
                _IsPrimitive(beanClass) ? _columnListHandler
                        : new BeanListHandler(beanClass), params);
	}

	/**
	 * 执行统计查询语句
	 * @param sql 数据库sql语句
	 * @param params sql中的参数
	 * @return 统计结果(只返回一个数值)
	 * @throws SQLException 数据库出错时抛出异常
	 */
	public static long stat(String sql, Object... params) throws SQLException {
	    Number num = (Number) _runner.query(getConnection(), sql,
                _scaleHandler, params);
        return (num != null) ? num.longValue() : -1;
	}

	/**
	 * 执行INSERT/UPDATE/DELETE语句
     * @param sql 数据库sql语句
     * @param params sql中的参数
     * @return 成功：返回受影响的行数
	 * @throws SQLException 修改数据库数据出错时抛出异常
	 */
	public static int update(String sql, Object... params) throws SQLException {
		return _runner.update(getConnection(), sql, params);
	}
}
