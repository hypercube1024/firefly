package test.mock.servlet;

import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;
import java.util.Vector;
import javax.servlet.ServletContext;
import javax.servlet.http.HttpSession;
import javax.servlet.http.HttpSessionContext;

@SuppressWarnings("deprecation")
public class MockHttpSession implements HttpSession {

	protected ServletContext servletContext;

	public MockHttpSession(MockServletContext servletContext) {
		this.servletContext = servletContext;
	}

	protected Map<String, Object> attributeMap = new HashMap<String, Object>();

	public void removeAttribute(String key) {
		attributeMap.remove(key);
	}

	public void setAttribute(String key, Object value) {
		attributeMap.put(key, value);
	}

	public Object getAttribute(String key) {
		return attributeMap.get(key);
	}

	public long getCreationTime() {
		throw new NoImplException();
	}

	public String getId() {
		throw new NoImplException();
	}

	public long getLastAccessedTime() {
		throw new NoImplException();
	}

	public int getMaxInactiveInterval() {
		throw new NoImplException();
	}

	public ServletContext getServletContext() {
		return servletContext;
	}

	public Object getValue(String arg0) {
		throw new NoImplException();
	}

	public String[] getValueNames() {
		throw new NoImplException();
	}

	public void invalidate() {

	}

	public boolean isNew() {
		throw new NoImplException();
	}

	public void putValue(String arg0, Object arg1) {

	}

	public void removeValue(String arg0) {

	}

	public void setMaxInactiveInterval(int arg0) {

	}

	public Enumeration<String> getAttributeNames() {
		return new Vector<String>(attributeMap.keySet()).elements();
	}

	/**
	 * @deprecated
	 */
	public HttpSessionContext getSessionContext() {
		return null;
	}

}
