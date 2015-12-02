package com.firefly.server.http;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TimeZone;

import javax.servlet.ServletOutputStream;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletResponse;

import com.firefly.mvc.web.servlet.SystemHtmlPage;
import com.firefly.net.Session;
import com.firefly.server.exception.HttpServerException;
import com.firefly.server.http.io.ChunkedOutputStream;
import com.firefly.server.http.io.HttpServerOutpuStream;
import com.firefly.server.http.io.NetBufferedOutputStream;
import com.firefly.server.http.io.StaticFileOutputStream;
import com.firefly.utils.StringUtils;
import com.firefly.utils.VerifyUtils;
import com.firefly.utils.log.Log;
import com.firefly.utils.log.LogFactory;
import com.firefly.utils.time.SafeSimpleDateFormat;

public class HttpServletResponseImpl implements HttpServletResponse {

	private static Log log = LogFactory.getInstance().getLog("firefly-system");
	public static SafeSimpleDateFormat GMT_FORMAT;
	private boolean committed;
	private HttpServletRequestImpl request;
	private int status, bufferSize;
	private String characterEncoding, shortMessage, contentLanguage;
	private Locale locale;
	private Map<String, String> headMap = new HashMap<String, String>();
	private List<Cookie> cookies = new LinkedList<Cookie>();
	private boolean usingWriter, usingOutputStream, usingFileOutputStream;
	private HttpServerOutpuStream out;
	private StaticFileOutputStream fileOut;
	private PrintWriter writer;
	private NetBufferedOutputStream bufferedOutput;

	boolean system;
	String systemResponseContent;

	static {
		SimpleDateFormat sdf = new SimpleDateFormat(
				"EEE, d MMM yyyy HH:mm:ss z", Locale.ENGLISH);
		sdf.setTimeZone(TimeZone.getTimeZone("GMT"));
		GMT_FORMAT = new SafeSimpleDateFormat(sdf);
	}

	public HttpServletResponseImpl(Session session,
			HttpServletRequestImpl request, String characterEncoding,
			int bufferSize) {
		this.request = request;
		this.characterEncoding = characterEncoding;
		this.bufferSize = bufferSize;

		locale = HttpServletRequestImpl.DEFAULT_LOCALE;
		setStatus(200);
		setHeader("Server", "Firefly/3.0");
	}
	
	//======================= socket output stream =======================
	
	public StaticFileOutputStream getStaticFileOutputStream()
			throws IOException {
		if (usingWriter)
			throw new HttpServerException(
					"getWriter has already been called for this response");
		if (usingOutputStream)
			throw new HttpServerException(
					"getOutputStream has already been called for this response");

		createOutput();
		usingFileOutputStream = true;
		return fileOut;
	}

	@Override
	public ServletOutputStream getOutputStream() throws IOException {
		if (usingWriter)
			throw new HttpServerException(
					"getWriter has already been called for this response");
		if (usingFileOutputStream)
			throw new HttpServerException(
					"getStaticFileOutputStream has already been called for this response");

		createOutput();

		usingOutputStream = true;
		return out;
	}

	@Override
	public PrintWriter getWriter() throws IOException {
		if (usingOutputStream)
			throw new HttpServerException(
					"getOutputStream has already been called for this response");
		if (usingFileOutputStream)
			throw new HttpServerException(
					"getStaticFileOutputStream has already been called for this response");

		createOutput();

		usingWriter = true;
		return writer;
	}
	
	@Override
	public void setBufferSize(int size) {
		bufferSize = size;
	}

	@Override
	public int getBufferSize() {
		return bufferSize;
	}

	@Override
	public void flushBuffer() throws IOException {
		out.flush();
	}

	@Override
	public void resetBuffer() {
		out.resetBuffer();
	}

	@Override
	public boolean isCommitted() {
		return committed;
	}

	public void setCommitted(boolean committed) {
		this.committed = committed;
	}

	@Override
	public void reset() {
		usingWriter = false;
		usingOutputStream = false;
		committed = false;
		bufferedOutput = null;
		out = null;
		writer = null;
	}
	
	

	private void createOutput() throws UnsupportedEncodingException {
		if (bufferedOutput == null) {
			boolean keepAlive = !"close".equals(headMap.get("Connection")) && request.isKeepAlive();
			setHeader("Date", GMT_FORMAT.format(new Date()));
			setHeader("Connection", keepAlive ? "keep-alive" : "close");
			
			bufferedOutput = new NetBufferedOutputStream(request.session, request, this, bufferSize, keepAlive);
			
			if(request.isChunked() && VerifyUtils.isEmpty(headMap.get("Content-Length")))
				out = new ChunkedOutputStream(bufferSize, bufferedOutput, request, this);
			else
				out = new HttpServerOutpuStream(bufferSize, bufferedOutput, request, this);
			
			fileOut = new StaticFileOutputStream(bufferSize, bufferedOutput, request, this);
			writer = new PrintWriter(new BufferedWriter(new OutputStreamWriter(out, characterEncoding)));
		}
	}
	
	//======================= HTTP encode =======================

	public byte[] getHeadData() {
		StringBuilder sb = new StringBuilder();
		sb.append(request.getProtocol()).append(' ').append(status).append(' ')
				.append(shortMessage).append("\r\n");

		for (String name : headMap.keySet())
			sb.append(name).append(": ").append(headMap.get(name))
					.append("\r\n");

		if (contentLanguage != null)
			sb.append("Content-Language: ").append(contentLanguage)
					.append("\r\n");

		for (Cookie cookie : cookies) {
			sb.append("Set-Cookie: ").append(cookie.getName()).append('=')
					.append(cookie.getValue());

			if (VerifyUtils.isNotEmpty(cookie.getComment()))
				sb.append(";Comment=").append(cookie.getComment());

			if (VerifyUtils.isNotEmpty(cookie.getDomain()))
				sb.append(";Domain=").append(cookie.getDomain());

			if (cookie.getMaxAge() >= 0)
				sb.append(";Max-Age=").append(cookie.getMaxAge());

			String path = VerifyUtils.isEmpty(cookie.getPath()) ? "/" : cookie
					.getPath();
			sb.append(";Path=").append(path);

			if (cookie.getSecure())
				sb.append(";Secure");

			sb.append(";Version=").append(cookie.getVersion()).append("\r\n");
		}

		sb.append("\r\n");
		String head = sb.toString();
		// System.out.println(head);
		return stringToByte(head);
	}

	private String toAbsolute(String location) {
		if (location.startsWith("http"))
			return location;

		StringBuilder sb = new StringBuilder();
		sb.append(request.getScheme()).append("://")
				.append(request.getServerName()).append(":")
				.append(request.getServerPort());

		if (location.charAt(0) == '/') {
			sb.append(location);
		} else {
			String URI = request.getRequestURI();
			int last = 0;
			for (int i = URI.length() - 1; i >= 0; i--) {
				if (URI.charAt(i) == '/') {
					last = i + 1;
					break;
				}
			}
			sb.append(URI.substring(0, last)).append(location);
		}
		return sb.toString();
	}

	public byte[] getChunkedSize(int length) {
		return stringToByte(Integer.toHexString(length) + "\r\n");
	}

	public byte[] stringToByte(String str) {
		byte[] ret = null;
		try {
			ret = str.getBytes(characterEncoding);
		} catch (UnsupportedEncodingException e) {
			log.error("string to bytes", e);
		}
		return ret;
	}
	
	public static String toEncoded(String url, String sessionId,
			String sessionIdName) {
		if (url == null || sessionId == null)
			return url;

		String path = url;
		String query = "";
		String anchor = "";
		int question = url.indexOf('?');
		if (question >= 0) {
			path = url.substring(0, question);
			query = url.substring(question);
		}
		int pound = path.indexOf('#');
		if (pound >= 0) {
			anchor = path.substring(pound);
			path = path.substring(0, pound);
		}
		StringBuilder sb = new StringBuilder(path);
		if (sb.length() > 0) { // jsessionid can't be first.
			sb.append(";");
			sb.append(sessionIdName);
			sb.append("=");
			sb.append(sessionId);
		}
		sb.append(anchor);
		sb.append(query);
		return sb.toString();

	}
	
	@Override
	public String encodeURL(String url) {
		if (VerifyUtils.isEmpty(url))
			return null;

		if (url.contains(";" + request.config.getSessionIdName() + "="))
			return url;

		String absoluteURL = toAbsolute(url);

		if (request.isRequestedSessionIdFromCookie()
				|| request.isRequestedSessionIdFromURL())
			return toEncoded(absoluteURL, request.getRequestedSessionId(),
					request.config.getSessionIdName());

		return null;
	}

	@Override
	public String encodeRedirectURL(String url) {
		return encodeURL(url);
	}

	@Override
	public String encodeUrl(String url) {
		return encodeURL(url);
	}

	@Override
	public String encodeRedirectUrl(String url) {
		return encodeRedirectURL(url);
	}
	
	@Override
	public void sendRedirect(String location) throws IOException {
		String absolute = toAbsolute(location);
		setStatus(SC_FOUND);
		setHeader("Location", absolute);
		setHeader("Content-Length", "0");
		outHeadData();
	}
	
	private void outHeadData() throws IOException {
		if (isCommitted())
			throw new IllegalStateException("response is committed");

		createOutput();
		try {
			bufferedOutput.write(getHeadData());
		} finally {
			bufferedOutput.close();
		}
		setCommitted(true);
	}
	
	//======================= set or get HTTP response head =======================
	
	@Override
	public String getCharacterEncoding() {
		return characterEncoding;
	}

	@Override
	public String getContentType() {
		return headMap.get("Content-Type");
	}

	@Override
	public void setCharacterEncoding(String charset) {
		characterEncoding = charset;
	}

	@Override
	public void setContentLength(int len) {
		headMap.put("Content-Length", String.valueOf(len));
	}

	@Override
	public void setContentType(String type) {
		headMap.put("Content-Type", type);
	}
	
	@Override
	public void setLocale(Locale locale) {
		if (locale == null) {
			return;
		}

		this.locale = locale;

		// Set the contentLanguage for header output
		contentLanguage = locale.getLanguage();
		if ((contentLanguage != null) && (contentLanguage.length() > 0)) {
			String country = locale.getCountry();
			StringBuilder value = new StringBuilder(contentLanguage);
			if (country != null && country.length() > 0) {
				value.append('-');
				value.append(country);
			}
			contentLanguage = value.toString();
		}
	}

	@Override
	public Locale getLocale() {
		return locale;
	}

	@Override
	public void addCookie(Cookie cookie) {
		if (cookie == null)
			throw new HttpServerException("cookie is null");

		if (VerifyUtils.isNotEmpty(cookie.getName())
				&& VerifyUtils.isNotEmpty(cookie.getValue())) {
			cookies.add(cookie);
		} else {
			throw new HttpServerException(
					"cookie name or value or domain is null");
		}
	}

	@Override
	public boolean containsHeader(String name) {
		return headMap.containsKey(name);
	}

	@Override
	public void setDateHeader(String name, long date) {
		setHeader(name, GMT_FORMAT.format(new Date(date)));
	}

	@Override
	public void addDateHeader(String name, long date) {
		addHeader(name, GMT_FORMAT.format(new Date(date)));
	}
	
	@Override
	public String getHeader(String name) {
		return headMap.get(name);
	}

	@Override
	public Collection<String> getHeaders(String name) {
		String v = headMap.get(name);
		if(v == null)
			return null;
		
		return Arrays.asList(StringUtils.split(v, ','));
	}

	@Override
	public Collection<String> getHeaderNames() {
		return headMap.keySet();
	}

	@Override
	public void setHeader(String name, String value) {
		headMap.put(name, value);
	}

	@Override
	public void addHeader(String name, String value) {
		String v = headMap.get(name);
		if (v != null) {
			v += "," + value;
			setHeader(name, v);
		} else
			setHeader(name, value);
	}

	@Override
	public void setIntHeader(String name, int value) {
		setHeader(name, String.valueOf(value));
	}

	@Override
	public void addIntHeader(String name, int value) {
		addHeader(name, String.valueOf(value));
	}

	@Override
	public void setStatus(int status) {
		this.status = status;
		this.shortMessage = Constants.STATUS_CODE.get(status);
	}

	@Override
	public void setStatus(int status, String shortMessage) {
		this.status = status;
		this.shortMessage = shortMessage;
	}

	public int getStatus() {
		return status;
	}
	
	//======================= send error response =======================

	@Override
	public void sendError(int sc, String msg) throws IOException {
		setStatus(sc, msg);
		systemResponseContent = shortMessage;
		outSystemData();
	}

	@Override
	public void sendError(int sc) throws IOException {
		setStatus(sc);
		systemResponseContent = shortMessage;
		outSystemData();
	}

	public void outSystemData() throws IOException {
		if (isCommitted())
			throw new IllegalStateException("response is committed");

		createOutput();
		if (status >= 400) {
			try {
				boolean hasContent = VerifyUtils.isNotEmpty(systemResponseContent);
				byte[] b = null;

				if (hasContent) {
					b = SystemHtmlPage.systemPageTemplate(status, systemResponseContent).getBytes(characterEncoding);
					setHeader("Content-Length", String.valueOf(b.length));
				} else {
					setHeader("Content-Length", "0");
				}

				bufferedOutput.write(getHeadData());
				if (hasContent)
					bufferedOutput.write(b);
			} finally {
				bufferedOutput.close();
			}
			setCommitted(true);
		}
	}

	public void scheduleSendError() {
		system = true;
		request.systemReq = true;
	}

	@Override
	public void setContentLengthLong(long len) {
		headMap.put("Content-Length", String.valueOf(len));
	}

}
