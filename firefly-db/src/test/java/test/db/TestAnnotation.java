package test.db;

import java.beans.BeanInfo;
import java.beans.IntrospectionException;
import java.beans.Introspector;
import java.beans.PropertyDescriptor;
import java.lang.reflect.Field;
import java.sql.SQLException;
import java.util.Arrays;

public class TestAnnotation {

	public static void main(String[] args) throws SQLException, NoSuchFieldException, SecurityException {
		PropertyDescriptor[] props = propertyDescriptors(User.class);
		for(PropertyDescriptor p : props) {
			if("class".equals(p.getName()))
				continue;

			Field field = User.class.getDeclaredField(p.getName());
			field.setAccessible(true);
			System.out.println(p.getName() + "|" + Arrays.toString(field.getAnnotations()));
		}

	}
	
	public static PropertyDescriptor[] propertyDescriptors(Class<?> c)
	        throws SQLException {
	        // Introspector caches BeanInfo classes for better performance
	        BeanInfo beanInfo = null;
	        try {
	            beanInfo = Introspector.getBeanInfo(c);

	        } catch (IntrospectionException e) {
	            throw new SQLException(
	                "Bean introspection failed: " + e.getMessage());
	        }

	        return beanInfo.getPropertyDescriptors();
	    }

}
