package com.firefly.server.http;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.security.Principal;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TreeMap;

import javax.servlet.AsyncContext;
import javax.servlet.DispatcherType;
import javax.servlet.RequestDispatcher;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.ServletInputStream;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import javax.servlet.http.Part;

import com.firefly.net.Session;
import com.firefly.server.exception.HttpServerException;
import com.firefly.server.io.PipedStream;
import com.firefly.server.utils.StringParser;
import com.firefly.utils.StringUtils;
import com.firefly.utils.VerifyUtils;
import com.firefly.utils.log.Log;
import com.firefly.utils.log.LogFactory;

public class HttpServletRequestImpl implements HttpServletRequest {
	
	int status, headLength, offset;
	String method, requestURI, queryString;
	String protocol = "HTTP/1.1";

	PipedStream bodyPipedStream;
	Cookie[] cookies;
	Map<String, String> headMap = new HashMap<String, String>();
	HttpServletResponseImpl response;
	Config config;
	Session session;
	boolean systemReq = false;

	private static Log log = LogFactory.getInstance().getLog("firefly-system");
//	private static final Set<String> IDEMPOTENT_METHODS = new HashSet<String>(Arrays.asList("GET", "HEAD", "OPTIONS", "TRACE", "DELETE"));
	private StringParser parser = new StringParser();
	private static final String[] EMPTY_STR_ARR = new String[0];
	private static final Cookie[] EMPTY_COOKIE_ARR = new Cookie[0];
	
	private String characterEncoding, requestedSessionId;
	private boolean requestedSessionIdFromCookie, requestedSessionIdFromURL;
	private boolean decodeFinish = false;
	private HttpSession httpSession;
	private Map<String, List<String>> parameterMap = new HashMap<String, List<String>>();
	private Map<String, String[]> _parameterMap = null;
	private Map<String, Object> attributeMap = new HashMap<String, Object>();
	private BufferedReader bufferedReader;
	private ServletInputStream servletInputStream;
	private RequestDispatcherImpl requestDispatcher = new RequestDispatcherImpl();
	private MultipartFormData multipartFormData;
	private AsyncContextImpl asyncContext = null;
	

	protected static Locale DEFAULT_LOCALE = Locale.getDefault();
	protected ArrayList<Locale> locales = new ArrayList<Locale>();
	private boolean loadParam, localesParsed;

	public HttpServletRequestImpl(Session session, Config config) {
		this.characterEncoding = config.getEncoding();
		this.session = session;
		this.config = config;
		response = new HttpServletResponseImpl(session, this, characterEncoding, config.getWriteBufferSize());
	}
	
	//======================= IO stream process =======================
	
	@Override
	public BufferedReader getReader() throws IOException {
		if (bufferedReader == null) {
			bufferedReader = new BufferedReader(new InputStreamReader(bodyPipedStream.getInputStream(), characterEncoding));
		}
		return bufferedReader;
	}

	@Override
	public ServletInputStream getInputStream() throws IOException {
		if(servletInputStream == null) {
			final InputStream in = bodyPipedStream.getInputStream();
			servletInputStream = new ServletInputStream() {

				@Override
				public int read() throws IOException {
					return in.read();
				}

				@Override
				public int available() throws IOException {
					return in.available();
				}

				@Override
				public void close() throws IOException {
					in.close();
				}

				public int read(byte[] b, int off, int len) throws IOException {
					return in.read(b, off, len);
				}
			};
		}
		return servletInputStream;
	}
	
	private void loadParam() {
		if (!loadParam) {
			try {
				loadParam(queryString);
				
				String contentType = getContentType();
				if (method.equals("POST") && contentType != null && contentType.startsWith("application/x-www-form-urlencoded")) {
					int contentLength = getContentLength();
					byte[] data = new byte[contentLength];
					byte[] buf = new byte[1024];
					ServletInputStream input = getInputStream();
					try {
						int readBytes = 0;
						for (int len = 0; (len = input.read(buf)) != -1;) {
							System.arraycopy(buf, 0, data, readBytes, len);
							readBytes += len;
							if (readBytes >= contentLength)
								break;
						}
						loadParam(new String(data, characterEncoding));
					} finally {
						input.close();
					}
				}
			} catch (Throwable t) {
				log.error("load param error", t);
			}
			loadParam = true;
		}
	}

	private void loadParam(String str) throws UnsupportedEncodingException {
		if (VerifyUtils.isNotEmpty(str)) {
			String[] p = StringUtils.split(str, '&');
			for (String kv : p) {
				int i = kv.indexOf('=');
				if (i > 0) {
					String name = kv.substring(0, i);
					String value = kv.substring(i + 1);

					List<String> list = parameterMap.get(name);
					if (list == null) {
						list = new ArrayList<String>();
						parameterMap.put(name, list);

					}
					list.add(URLDecoder.decode(value, characterEncoding));
				}

			}
		}
	}
	
	/**
	 * if http method is POST or PUT, when the business process finish, it must close piped stream
	 * @throws IOException
	 */
	void releaseInputStreamData() throws IOException {
		try {
			if(getContentLength() > 0) {
				if(bodyPipedStream != null) {
					bodyPipedStream.close();
				}
				if(multipartFormData != null) {
					multipartFormData.close();
				}
			}
		} catch(Throwable t) {
			log.error("release input stream error", t);
		}
	}
	
	
	//======================= HTTP decode need following method =======================

	boolean isKeepAlive() {
		return !systemReq 
				&& config.isKeepAlive()
				&& ("keep-alive".equalsIgnoreCase(getHeader("Connection")) 
					|| (!getProtocol().equals("HTTP/1.0") && !"close".equalsIgnoreCase(getHeader("Connection"))));
	}
	
	boolean isSupportPipeline() {
		return config.isPipeline() 
				&& config.isKeepAlive() 
//				&& IDEMPOTENT_METHODS.contains(getMethod()) 
				&& ("Keep-Alive".equalsIgnoreCase(getHeader("Connection")) || !getProtocol().equals("HTTP/1.0"));
	}

	boolean isChunked() {
		return !systemReq && !getProtocol().equals("HTTP/1.0");
	}
	
	/**
	 * decode finish, then enter into business process
	 */
	void decodeFinish() {
		if(!decodeFinish) {
			session.fireReceiveMessage(this);
			decodeFinish = true;
		}
	}
	
	
	//======================= set and get request attribute =======================
	
	@Override
	public void setAttribute(String name, Object o) {
		attributeMap.put(name, o);
	}

	@Override
	public void removeAttribute(String name) {
		attributeMap.remove(name);
	}

	@Override
	public Object getAttribute(String name) {
		return attributeMap.get(name);
	}

	@Override
	public Enumeration<String> getAttributeNames() {
		return new Enumeration<String>() {
			private Iterator<String> iterator = attributeMap.keySet()
					.iterator();

			@Override
			public boolean hasMoreElements() {
				return iterator.hasNext();
			}

			@Override
			public String nextElement() {
				return iterator.next();
			}
		};
	}
	
	
	//======================= get HTTP head and parameter =======================
	
	@Override
	public long getDateHeader(String name) {
		String v = getHeader(name);
		return v != null ? Long.parseLong(v) : 0;
	}

	@Override
	public String getHeader(String name) {
		return headMap.get(name.toLowerCase());
	}

	@Override
	public Enumeration<String> getHeaders(String name) {
		String value = getHeader(name);
		final String[] values = StringUtils.split(value, ',');
		return new Enumeration<String>() {
			private int i = 0;

			@Override
			public boolean hasMoreElements() {
				return i < values.length;
			}

			@Override
			public String nextElement() {
				return values[i++];
			}
		};
	}

	@Override
	public Enumeration<String> getHeaderNames() {
		return new Enumeration<String>() {
			private Iterator<String> iterator = headMap.keySet().iterator();

			@Override
			public boolean hasMoreElements() {
				return iterator.hasNext();
			}

			@Override
			public String nextElement() {
				return iterator.next();
			}
		};
	}

	@Override
	public int getIntHeader(String name) {
		String v = getHeader(name);
		return v != null ? Integer.parseInt(v) : 0;
	}

	@Override
	public String getMethod() {
		return method;
	}

	@Override
	public String getQueryString() {
		return queryString;
	}

	@Override
	public String getRequestURI() {
		return requestURI;
	}

	@Override
	public StringBuffer getRequestURL() {
		StringBuffer url = new StringBuffer();
		String scheme = getScheme();
		int port = getServerPort();
		if (port < 0)
			port = 80; // Work around java.net.URL bug

		url.append(scheme);
		url.append("://");
		url.append(getServerName());
		if ((scheme.equals("http") && (port != 80))
				|| (scheme.equals("https") && (port != 443))) {
			url.append(':');
			url.append(port);
		}
		url.append(getRequestURI());
		return url;
	}

	@Override
	public String getCharacterEncoding() {
		return characterEncoding;
	}

	@Override
	public void setCharacterEncoding(String characterEncoding)
			throws UnsupportedEncodingException {
		this.characterEncoding = characterEncoding;
	}

	@Override
	public int getContentLength() {
		return getIntHeader("Content-Length");
	}

	@Override
	public String getContentType() {
		return getHeader("Content-Type");
	}

	@Override
	public String getParameter(String name) {
		loadParam();
		List<String> list = parameterMap.get(name);
		return list != null && list.size() > 0 ? list.get(0) : null;
	}

	@Override
	public Enumeration<String> getParameterNames() {
		loadParam();
		return new Enumeration<String>() {
			private Iterator<String> iterator = parameterMap.keySet()
					.iterator();

			@Override
			public boolean hasMoreElements() {
				return iterator.hasNext();
			}

			@Override
			public String nextElement() {
				return iterator.next();
			}
		};
	}

	@Override
	public String[] getParameterValues(String name) {
		loadParam();
		return parameterMap.get(name).toArray(EMPTY_STR_ARR);
	}

	@Override
	public Map<String, String[]> getParameterMap() {
		loadParam();
		if(_parameterMap == null) {
			_parameterMap = new HashMap<String, String[]>();
			for(Map.Entry<String, List<String>> entry : parameterMap.entrySet()) {
				_parameterMap.put(entry.getKey(), entry.getValue().toArray(EMPTY_STR_ARR));
			}
		}
		return _parameterMap;
	}
	
	@Override
	public Cookie[] getCookies() {
		if (cookies == null) {
			List<Cookie> list = new ArrayList<Cookie>();
			String cookieStr = getHeader("Cookie");
			if (VerifyUtils.isEmpty(cookieStr)) {
				cookies = EMPTY_COOKIE_ARR;
			} else {
				String[] c = StringUtils.split(cookieStr, ';');
				for (String t : c) {
					int j = 0;
					for (int i = 0; i < t.length(); i++) {
						if (t.charAt(i) == '=') {
							j = i;
							break;
						}
					}
					if (j > 1) {
						String name = t.substring(0, j).trim();
						String value = t.substring(j + 1).trim();
						Cookie cookie = new Cookie(name, value);
						list.add(cookie);
					} else
						continue;
				}
				cookies = list.toArray(EMPTY_COOKIE_ARR);
			}
		}
		return cookies;
	}

	@Override
	public String getProtocol() {
		return protocol;
	}

	@Override
	public String getScheme() {
		return "http";
	}
	
	@Override
	public Locale getLocale() {
		if (!localesParsed)
			parseLocales();

		if (locales.size() > 0) {
			return ((Locale) locales.get(0));
		} else {
			return (DEFAULT_LOCALE);
		}
	}

	@Override
	public Enumeration<Locale> getLocales() {
		if (!localesParsed)
			parseLocales();

		if (locales.size() == 0)
			locales.add(DEFAULT_LOCALE);

		return new Enumeration<Locale>() {
			private int i = 0;

			@Override
			public boolean hasMoreElements() {
				return i < locales.size();
			}

			@Override
			public Locale nextElement() {
				return locales.get(i++);
			}
		};
	}
	
	protected void parseLocales() {
		localesParsed = true;
		Enumeration<String> values = getHeaders("accept-language");
		while (values.hasMoreElements()) {
			String value = values.nextElement().toString();
			parseLocalesHeader(value);
		}
	}

	/**
	 * Parse accept-language header value.
	 */
	protected void parseLocalesHeader(String value) {

		// Store the accumulated languages that have been requested in
		// a local collection, sorted by the quality value (so we can
		// add Locales in descending order). The values will be ArrayLists
		// containing the corresponding Locales to be added
		TreeMap<Double, ArrayList<Locale>> locales = new TreeMap<Double, ArrayList<Locale>>();

		// Preprocess the value to remove all whitespace
		int white = value.indexOf(' ');
		if (white < 0)
			white = value.indexOf('\t');
		if (white >= 0) {
			StringBuilder sb = new StringBuilder();
			int len = value.length();
			for (int i = 0; i < len; i++) {
				char ch = value.charAt(i);
				if ((ch != ' ') && (ch != '\t'))
					sb.append(ch);
			}
			value = sb.toString();
		}

		// Process each comma-delimited language specification
		parser.setString(value); // ASSERT: parser is available to us
		int length = parser.getLength();
		while (true) {

			// Extract the next comma-delimited entry
			int start = parser.getIndex();
			if (start >= length)
				break;
			int end = parser.findChar(',');
			String entry = parser.extract(start, end).trim();
			parser.advance(); // For the following entry

			// Extract the quality factor for this entry
			double quality = 1.0;
			int semi = entry.indexOf(";q=");
			if (semi >= 0) {
				try {
					String strQuality = entry.substring(semi + 3);
					if (strQuality.length() <= 5) {
						quality = Double.parseDouble(strQuality);
					} else {
						quality = 0.0;
					}
				} catch (NumberFormatException e) {
					quality = 0.0;
				}
				entry = entry.substring(0, semi);
			}

			// Skip entries we are not going to keep track of
			if (quality < 0.00005)
				continue; // Zero (or effectively zero) quality factors
			if ("*".equals(entry))
				continue; // FIXME - "*" entries are not handled

			// Extract the language and country for this entry
			String language = null;
			String country = null;
			String variant = null;
			int dash = entry.indexOf('-');
			if (dash < 0) {
				language = entry;
				country = "";
				variant = "";
			} else {
				language = entry.substring(0, dash);
				country = entry.substring(dash + 1);
				int vDash = country.indexOf('-');
				if (vDash > 0) {
					String cTemp = country.substring(0, vDash);
					variant = country.substring(vDash + 1);
					country = cTemp;
				} else {
					variant = "";
				}
			}
			if (!isAlpha(language) || !isAlpha(country) || !isAlpha(variant)) {
				continue;
			}

			// Add a new Locale to the list of Locales for this quality level
			Locale locale = new Locale(language, country, variant);
			Double key = new Double(-quality); // Reverse the order
			ArrayList<Locale> values = locales.get(key);
			if (values == null) {
				values = new ArrayList<Locale>();
				locales.put(key, values);
			}
			values.add(locale);

		}

		// Process the quality values in highest->lowest order (due to
		// negating the Double value when creating the key)
		Iterator<Double> keys = locales.keySet().iterator();
		while (keys.hasNext()) {
			Double key = keys.next();
			ArrayList<Locale> list = locales.get(key);
			Iterator<Locale> values = list.iterator();
			while (values.hasNext()) {
				Locale locale = (Locale) values.next();
				addLocale(locale);
			}
		}

	}

	protected static final boolean isAlpha(String value) {
		for (int i = 0; i < value.length(); i++) {
			char c = value.charAt(i);
			if (!((c >= 'a' && c <= 'z') || (c >= 'A' && c <= 'Z'))) {
				return false;
			}
		}
		return true;
	}

	protected void addLocale(Locale locale) {
		locales.add(locale);
	}
	
	
	//======================= get socket info =======================

	/**
	 * @return server binding ip or host name
	 */
	@Override
	public String getServerName() {
		return session.getLocalAddress().getHostName();
	}

	/**
	 * @return server port
	 */
	@Override
	public int getServerPort() {
		return session.getLocalAddress().getPort();
	}

	@Override
	public String getRemoteAddr() {
		return session.getRemoteAddress().toString();
	}

	@Override
	public String getRemoteHost() {
		return session.getRemoteAddress().getHostName();
	}
	
	@Override
	public int getRemotePort() {
		return session.getRemoteAddress().getPort();
	}


	@Override
	public String getLocalName() {
		return session.getLocalAddress().getHostName();
	}

	@Override
	public String getLocalAddr() {
		return session.getLocalAddress().toString();
	}

	@Override
	public int getLocalPort() {
		return session.getLocalAddress().getPort();
	}

	
	//======================= get servlet info =======================

	@Override
	public String getServletPath() {
		return config.getServletPath();
	}
	
	@Override
	public String getContextPath() {
		return config.getContextPath();
	}
	
	@Override
	public RequestDispatcher getRequestDispatcher(String path) {
		requestDispatcher.path = path;
		return requestDispatcher;
	}
	
	@Override
	public ServletContext getServletContext() {
		throw new HttpServerException("no implements this method!");
	}
	
	@Override
	public DispatcherType getDispatcherType() {
		// TODO need implement
		throw new HttpServerException("no implements this method!");
	}
	
	@Override
	public String getRealPath(String path) {
		// TODO need implement
		throw new HttpServerException("no implements this method!");
	}

	@Override
	public String getPathInfo() {
		// TODO need implement
		throw new HttpServerException("no implements this method!");
	}

	@Override
	public String getPathTranslated() {
		return new File(config.getServerHome(), getRequestURI()).getAbsolutePath();
	}

	//======================= authentication =======================
	
	@Override
	public String getRemoteUser() {
		// TODO need implement
		throw new HttpServerException("no implements this method!");
	}

	@Override
	public boolean isUserInRole(String role) {
		// TODO need implement
		throw new HttpServerException("no implements this method!");
	}

	@Override
	public Principal getUserPrincipal() {
		// TODO need implement
		throw new HttpServerException("no implements this method!");
	}

	@Override
	public String getAuthType() {
		// TODO need implement
		throw new HttpServerException("no implements this method!");
	}

	@Override
	public boolean isSecure() {
		// TODO need implement
		throw new HttpServerException("no implements this method!");
	}
	
	@Override
	public boolean authenticate(HttpServletResponse response)
			throws IOException, ServletException {
		// TODO need implement
		throw new HttpServerException("no implements this method!");
	}

	@Override
	public void login(String username, String password) throws ServletException {
		// TODO need implement
		throw new HttpServerException("no implements this method!");
	}

	@Override
	public void logout() throws ServletException {
		// TODO need implement
		throw new HttpServerException("no implements this method!");
	}

	@Override
	public String getRequestedSessionId() {
		return requestedSessionId;
	}

	@Override
	public HttpSession getSession(boolean create) {
		if (create) {
			httpSession = config.getHttpSessionManager().create();
			requestedSessionId = httpSession.getId();
			response.addCookie(new Cookie(config.getSessionIdName(),
					httpSession.getId()));
		} else {
			if (isRequestedSessionIdFromCookie()
					|| isRequestedSessionIdFromURL()) {
				httpSession = config.getHttpSessionManager().get(
						requestedSessionId);
			}
		}

		return httpSession;
	}

	@Override
	public HttpSession getSession() {
		if (httpSession == null) {
			if (isRequestedSessionIdFromCookie()
					|| isRequestedSessionIdFromURL())
				httpSession = config.getHttpSessionManager().get(
						requestedSessionId);

			if (httpSession == null)
				getSession(true);
		}
		return httpSession;
	}

	@Override
	public boolean isRequestedSessionIdValid() {
		return requestedSessionId != null ? config.getHttpSessionManager()
				.containsKey(requestedSessionId) : false;
	}

	@Override
	public boolean isRequestedSessionIdFromCookie() {
		if (requestedSessionId != null)
			return requestedSessionIdFromCookie;

		for (Cookie cookie : getCookies()) {
			if (cookie.getName().equals(config.getSessionIdName())) {
				requestedSessionId = cookie.getValue();
				requestedSessionIdFromCookie = true;
				return true;
			}
		}
		return false;
	}

	@Override
	public boolean isRequestedSessionIdFromURL() {
		if (requestedSessionId != null)
			return requestedSessionIdFromURL;

		String sessionId = getSessionId(requestURI, config.getSessionIdName());
		if (VerifyUtils.isNotEmpty(sessionId)) {
			requestedSessionId = sessionId;
			requestedSessionIdFromURL = true;
			return true;
		}
		return false;
	}

	public static String getSessionId(String uri, String sessionIdName) {
		String sessionId = null;
		int i = uri.indexOf(';');
		int j = uri.indexOf('#');
		if (i > 0) {
			String tmp = j > i ? uri.substring(i + 1, j) : uri.substring(i + 1);
			int m = 0;
			for (int k = 0; k < tmp.length(); k++) {
				if (tmp.charAt(k) == '=') {
					m = k;
					break;
				}
			}
			if (m > 0) {
				String name = tmp.substring(0, m);
				String value = tmp.substring(m + 1);
				if (name.equals(sessionIdName)) {
					sessionId = value;
				}
			}
		}
		return sessionId;
	}

	@Override
	public boolean isRequestedSessionIdFromUrl() {
		return isRequestedSessionIdFromURL();
	}

	@Override
	public String toString() {
		return method + " " + requestURI + queryString + " " + protocol + "\r\n" + headMap.toString();
	}

	

	//======================= asynchronous control =======================
	@Override
	public AsyncContext startAsync() throws IllegalStateException {
		return startAsync(this, response);
	}

	@Override
	public AsyncContext startAsync(ServletRequest servletRequest, ServletResponse servletResponse) throws IllegalStateException {
		if(asyncContext == null) {
			asyncContext = new AsyncContextImpl();
		}
		asyncContext.startAsync(servletRequest, servletResponse, (servletRequest == this && servletResponse == response), config.getMaxConnectionTimeout());
		return asyncContext;
	}

	@Override
	public boolean isAsyncStarted() {
		if(asyncContext == null)
			return false;
		
		return asyncContext.isStartAsync();
	}

	@Override
	public boolean isAsyncSupported() {
		return true;
	}

	@Override
	public AsyncContext getAsyncContext() {
		if(!isAsyncStarted())
			throw new IllegalStateException("async context not start!");
			
		return asyncContext;
	}

	

	//======================= multipart data =======================

	@Override
	public Collection<Part> getParts() throws IOException, ServletException {
		if(multipartFormData == null) {
			ServletInputStream input = null;
			try {
				input = getInputStream();
				PartImpl.tempdir = config.getTempdir();
				multipartFormData = new MultipartFormData(MultipartFormDataParser.parse(input, getHeader("Content-Type"), characterEncoding));
			} finally {
				input.close();
			}
		}
		
		return multipartFormData.getParts();
	}

	@Override
	public Part getPart(String name) throws IOException, ServletException {
		if(multipartFormData == null)
			getParts();
		
		return multipartFormData.getPart(name);
	}

}
