package com.firefly.db;

import java.sql.SQLException;
import java.util.List;

import junit.framework.Assert;

import org.junit.Test;

public class QueryHelperTest {

	@Test
	public void testUpdate(){
		Object[] params = new Object[]{"JJ","123"};
		try {
			int result = QueryHelper.update("INSERT INTO user (name,password) values (?,?)", params);
			Assert.assertEquals(1, result);
		} catch (SQLException e) {
			e.printStackTrace();
		} finally{
			DBManager.commit();
		}
	}
	
	@Test
	public void testRead(){
		Object[] params = new Object[]{3};
		try {
			User user = QueryHelper.read(User.class, "SELECT id,name,password FROM user WHERE id = ?", params);
			Assert.assertEquals("JJ", user.getName());
		} catch (SQLException e) {
			e.printStackTrace();
		}
	}
	
	@Test
	public void testQuery(){
		Object[] params = new Object[]{};
		try {
			List<User> list = QueryHelper.query(User.class, "SELECT id,name,password FROM user", params);
			Assert.assertNotNull(list);
		} catch (SQLException e) {
			e.printStackTrace();
		}
	}
}
