package test.mock.servlet;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.Vector;
import javax.servlet.RequestDispatcher;
import javax.servlet.Servlet;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import com.firefly.utils.log.Log;
import com.firefly.utils.log.LogFactory;

public class MockServletContext extends MockServletObject implements
		ServletContext {

	private static Log log = LogFactory.getInstance().getLog("firefly-system");

	public int getMajorVersion() {
		throw new NoImplException();
	}

	public String getMimeType(String arg0) {
		throw new NoImplException();
	}

	public int getMinorVersion() {
		throw new NoImplException();
	}

	public RequestDispatcher getNamedDispatcher(String arg0) {
		throw new NoImplException();
	}

	public String getRealPath(String path) {
		if (path.startsWith("/WEB-INF/lib/"))
			return new File(path.substring("/WEB-INF/lib/".length()))
					.getAbsolutePath();
		if (path.startsWith("/WEB-INF/classes/"))
			return new File(path.substring("/WEB-INF/classes/".length()))
					.getAbsolutePath();
		if (path.startsWith("/"))
			return new File("." + path).getAbsolutePath();
		return new File(path).getAbsolutePath();
	}

	public RequestDispatcher getRequestDispatcher(String arg0) {
		throw new NoImplException();
	}

	public URL getResource(String name) throws MalformedURLException {
		return getClass().getResource(name);
	}

	public InputStream getResourceAsStream(String name) {
		return getClass().getResourceAsStream(name);
	}

	public Set<String> getResourcePaths(String name) {
		try {
			HashSet<String> hashSet = new HashSet<String>();
			Enumeration<URL> enumeration;
			enumeration = getClass().getClassLoader().getResources(name);
			while (enumeration.hasMoreElements()) {
				URL url = (URL) enumeration.nextElement();
				hashSet.add(url.toString());
			}
			return hashSet;
		} catch (IOException e) {
			return null;
		}
	}

	public String getServerInfo() {
		throw new NoImplException();
	}

	public Servlet getServlet(String name) throws ServletException {
		throw new NoImplException();
	}

	private String servletContextName;

	public String getServletContextName() {
		return servletContextName;
	}

	public void setServletContextName(String servletContextName) {
		this.servletContextName = servletContextName;
	}

	public Enumeration<String> getServletNames() {
		throw new NoImplException();
	}

	public Enumeration<Servlet> getServlets() {
		throw new NoImplException();
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

	public Enumeration<String> getAttributeNames() {
		return new Vector<String>(attributeMap.keySet()).elements();
	}

	public ServletContext getContext(String arg0) {
		throw new NoImplException();
	}

	public String getContextPath() {
		throw new NoImplException();
	}

	@Override
	public void log(String msg) {
		log.debug(msg);
	}

	@Override
	public void log(Exception exception, String msg) {
		log.debug(msg);
	}

	@Override
	public void log(String message, Throwable throwable) {
		log.debug(message);
	}

}
