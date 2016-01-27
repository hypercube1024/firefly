package com.firefly.server.http2.servlet;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.nio.charset.Charset;
import java.security.Principal;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

import javax.servlet.AsyncContext;
import javax.servlet.DispatcherType;
import javax.servlet.ReadListener;
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
import javax.servlet.http.HttpUpgradeHandler;
import javax.servlet.http.Part;

import com.firefly.codec.http2.encode.UrlEncoded;
import com.firefly.codec.http2.model.HttpHeader;
import com.firefly.codec.http2.model.MetaData.Request;
import com.firefly.codec.http2.stream.HTTP2Configuration;
import com.firefly.codec.http2.stream.HTTPConnection;
import com.firefly.utils.StringUtils;
import com.firefly.utils.VerifyUtils;
import com.firefly.utils.collection.MultiMap;
import com.firefly.utils.io.ByteArrayPipedStream;
import com.firefly.utils.io.FilePipedStream;
import com.firefly.utils.io.IO;
import com.firefly.utils.io.PipedStream;
import com.firefly.utils.lang.StringParser;
import com.firefly.utils.log.Log;
import com.firefly.utils.log.LogFactory;

public class HTTPServletRequestImpl implements HttpServletRequest {

	private static Log log = LogFactory.getInstance().getLog("firefly-system");

	private final HTTPConnection connection;
	private final Request request;

	private static final Cookie[] EMPTY_COOKIE_ARR = new Cookie[0];
	private Cookie[] cookies;

	private MultiMap<String> parameterMap;
	private Map<String, String[]> _parameterMap;
	private List<Locale> localeList;
	private Collection<Part> parts;

	private final HTTP2Configuration http2Configuration;
	private Charset encoding;
	private String characterEncoding;
	private Map<String, Object> attributeMap = new HashMap<String, Object>();

	HTTPServletResponseImpl response = new HTTPServletResponseImpl();
	private PipedStream bodyPipedStream;
	private ServletInputStream servletInputStream;
	private BufferedReader bufferedReader;

	public HTTPServletRequestImpl(HTTP2Configuration http2Configuration, Request request, HTTPConnection connection) {
		this.request = request;
		this.connection = connection;
		this.http2Configuration = http2Configuration;
		try {
			this.setCharacterEncoding(http2Configuration.getCharacterEncoding());
		} catch (UnsupportedEncodingException e) {
			log.error("set character encoding error", e);
		}
	}

	private static class IteratorWrap<T> implements Enumeration<T> {

		private final Iterator<T> iterator;

		public IteratorWrap(Iterator<T> iterator) {
			this.iterator = iterator;
		}

		@Override
		public boolean hasMoreElements() {
			return iterator.hasNext();
		}

		@Override
		public T nextElement() {
			return iterator.next();
		}

	}

	// set and get request attribute

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
		return new IteratorWrap<String>(attributeMap.keySet().iterator());
	}

	// get the connection attributes

	@Override
	public String getCharacterEncoding() {
		return characterEncoding;
	}

	@Override
	public void setCharacterEncoding(String env) throws UnsupportedEncodingException {
		this.encoding = Charset.forName(env);
		this.characterEncoding = env;
	}

	@Override
	public boolean isSecure() {
		return http2Configuration.isSecureConnectionEnabled();
	}

	@Override
	public String getServerName() {
		return connection.getLocalAddress().getHostName();
	}

	@Override
	public int getServerPort() {
		return connection.getLocalAddress().getPort();
	}

	@Override
	public String getRemoteAddr() {
		return connection.getRemoteAddress().getAddress().getHostAddress();
	}

	@Override
	public String getRemoteHost() {
		return connection.getRemoteAddress().getHostName();
	}

	@Override
	public int getRemotePort() {
		return connection.getRemoteAddress().getPort();
	}

	@Override
	public String getLocalName() {
		return connection.getLocalAddress().getHostName();
	}

	@Override
	public String getLocalAddr() {
		return connection.getLocalAddress().getAddress().getHostAddress();
	}

	@Override
	public int getLocalPort() {
		return connection.getLocalAddress().getPort();
	}

	// get HTTP heads and parameters

	@Override
	public long getDateHeader(String name) {
		return request.getFields().getDateField(name);
	}

	@Override
	public String getHeader(String name) {
		return request.getFields().get(name);
	}

	@Override
	public Enumeration<String> getHeaders(String name) {
		return request.getFields().getValues(name);
	}

	@Override
	public Enumeration<String> getHeaderNames() {
		return request.getFields().getFieldNames();
	}

	@Override
	public int getIntHeader(String name) {
		return (int) request.getFields().getLongField(name);
	}

	@Override
	public String getMethod() {
		return request.getMethod();
	}

	@Override
	public String getProtocol() {
		return request.getVersion().asString();
	}

	@Override
	public String getScheme() {
		return request.getURI().getScheme();
	}

	@Override
	public String getQueryString() {
		return request.getURI().getQuery();
	}

	@Override
	public int getContentLength() {
		return (int) request.getContentLength();
	}

	@Override
	public long getContentLengthLong() {
		return request.getContentLength();
	}

	@Override
	public String getContentType() {
		return request.getFields().get(HttpHeader.CONTENT_TYPE);
	}

	@Override
	public Locale getLocale() {
		parseLocales();
		return localeList.get(0);
	}

	@Override
	public Enumeration<Locale> getLocales() {
		parseLocales();
		return new IteratorWrap<Locale>(localeList.iterator());
	}

	protected void parseLocales() {
		if (localeList == null) {
			localeList = new ArrayList<>();
			Enumeration<String> values = getHeaders("accept-language");
			while (values.hasMoreElements()) {
				parseLocalesHeader(values.nextElement());
			}
			if (localeList.size() == 0) {
				localeList.add(Locale.getDefault());
			}
		}
	}

	/**
	 * Parse accept-language header value.
	 * 
	 * @param value
	 *            The head string
	 */
	protected void parseLocalesHeader(String value) {
		StringParser parser = new StringParser();
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
			if (!StringUtils.isAlpha(language) || !StringUtils.isAlpha(country) || !StringUtils.isAlpha(variant)) {
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
			if (list != null && list.size() > 0) {
				localeList.addAll(list);
			}
		}
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
			return cookies;
		} else {
			return cookies;
		}
	}

	PipedStream getBodyPipedStream() {
		if (bodyPipedStream == null) {
			long contentLength = request.getContentLength();
			if (contentLength > 0) {
				if (contentLength > http2Configuration.getHttpBodyThreshold()) {
					bodyPipedStream = new FilePipedStream(http2Configuration.getTemporaryDirectory());
				} else {
					bodyPipedStream = new ByteArrayPipedStream((int) contentLength);
				}
			} else {
				bodyPipedStream = new FilePipedStream(http2Configuration.getTemporaryDirectory());
			}
			return bodyPipedStream;
		} else {
			return bodyPipedStream;
		}
	}

	boolean hasData() {
		return bodyPipedStream != null;
	}

	private class HttpServletInputStream extends ServletInputStream {

		@Override
		public int available() throws IOException {
			if (hasData()) {
				return getBodyPipedStream().getInputStream().available();
			} else {
				return 0;
			}
		}

		@Override
		public void close() throws IOException {
			if (hasData()) {
				getBodyPipedStream().getInputStream().close();
			}
		}

		@Override
		public boolean isFinished() {
			return hasData();
		}

		@Override
		public boolean isReady() {
			return hasData();
		}

		@Override
		public void setReadListener(ReadListener readListener) {
			if (hasData()) {
				try {
					readListener.onDataAvailable();
					readListener.onAllDataRead();
				} catch (IOException e) {
					readListener.onError(e);
				}
			}
		}

		@Override
		public int read() throws IOException {
			if (hasData()) {
				return getBodyPipedStream().getInputStream().read();
			} else {
				return -1;
			}
		}

		@Override
		public int read(byte[] b, int off, int len) throws IOException {
			if (hasData()) {
				return getBodyPipedStream().getInputStream().read(b, off, len);
			} else {
				return -1;
			}
		}

	}

	@Override
	public ServletInputStream getInputStream() throws IOException {
		if (servletInputStream == null) {
			servletInputStream = new HttpServletInputStream();
			return servletInputStream;
		} else {
			return servletInputStream;
		}
	}

	@Override
	public BufferedReader getReader() throws IOException {
		if (bufferedReader == null) {
			bufferedReader = new BufferedReader(new InputStreamReader(getInputStream(), encoding));
			return bufferedReader;
		} else {
			return bufferedReader;
		}
	}

	protected MultiMap<String> getParameters() {
		if (parameterMap == null) {
			parameterMap = new MultiMap<String>();
			try {
				request.getURI().decodeQueryTo(parameterMap, encoding);
			} catch (UnsupportedEncodingException e) {
				log.error("parse parameters exception", e);
			}

			String contentType = getContentType();
			if (hasData() && "POST".equals(request.getMethod()) && contentType != null
					&& contentType.startsWith("application/x-www-form-urlencoded")) {
				try (BufferedReader in = getReader()) {
					String urlencodedForm = IO.toString(in);
					UrlEncoded.decodeTo(urlencodedForm, parameterMap, encoding);
				} catch (IOException e) {
					log.error("parse urlencoded form exception", e);
				}
			}
			return parameterMap;
		} else {
			return parameterMap;
		}
	}

	@Override
	public String getParameter(String name) {
		List<String> values = getParameters().get(name);
		if (values != null && values.size() > 0) {
			return values.get(0);
		} else {
			return null;
		}
	}

	@Override
	public Enumeration<String> getParameterNames() {
		Set<String> names = getParameters().keySet();
		if (names != null && names.size() > 0) {
			return new IteratorWrap<>(names.iterator());
		} else {
			return Collections.emptyEnumeration();
		}
	}

	@Override
	public String[] getParameterValues(String name) {
		List<String> values = getParameters().getValues(name);
		if (values != null) {
			return values.toArray(StringUtils.EMPTY_STRING_ARRAY);
		} else {
			return null;
		}
	}

	@Override
	public Map<String, String[]> getParameterMap() {
		if (_parameterMap == null) {
			_parameterMap = getParameters().toStringArrayMap();
		}
		return _parameterMap;
	}

	@Override
	public Collection<Part> getParts() throws IOException, ServletException {
		if (parts == null) {
			try (ServletInputStream input = getInputStream()) {
				parts = MultipartFormDataParser.parse(http2Configuration, input, getContentType(), encoding);
			}
			return parts;
		} else {
			return parts;
		}
	}

	@Override
	public Part getPart(String name) throws IOException, ServletException {
		for (Part part : getParts()) {
			if (part.getName().equals(name))
				return part;
		}
		return null;
	}

	@Override
	public RequestDispatcher getRequestDispatcher(String path) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public String getRealPath(String path) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public ServletContext getServletContext() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public AsyncContext startAsync() throws IllegalStateException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public AsyncContext startAsync(ServletRequest servletRequest, ServletResponse servletResponse)
			throws IllegalStateException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public boolean isAsyncStarted() {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public boolean isAsyncSupported() {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public AsyncContext getAsyncContext() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public DispatcherType getDispatcherType() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public String getAuthType() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public String getPathInfo() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public String getPathTranslated() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public String getContextPath() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public String getRemoteUser() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public boolean isUserInRole(String role) {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public Principal getUserPrincipal() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public String getRequestedSessionId() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public String getRequestURI() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public StringBuffer getRequestURL() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public String getServletPath() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public HttpSession getSession(boolean create) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public HttpSession getSession() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public String changeSessionId() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public boolean isRequestedSessionIdValid() {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public boolean isRequestedSessionIdFromCookie() {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public boolean isRequestedSessionIdFromURL() {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public boolean isRequestedSessionIdFromUrl() {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public boolean authenticate(HttpServletResponse response) throws IOException, ServletException {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public void login(String username, String password) throws ServletException {
		// TODO Auto-generated method stub

	}

	@Override
	public void logout() throws ServletException {
		// TODO Auto-generated method stub

	}

	@Override
	public <T extends HttpUpgradeHandler> T upgrade(Class<T> handlerClass) throws IOException, ServletException {
		// TODO Auto-generated method stub
		return null;
	}

}
