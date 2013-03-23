package test.mock.servlet;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import javax.servlet.ServletOutputStream;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletResponse;

public class MockHttpServletResponse implements HttpServletResponse {

	protected ByteArrayOutputStream stream = new ByteArrayOutputStream();

	protected PrintWriter writer;

	protected Map<String, String> headers;

	protected Set<Cookie> cookies;

	protected int status;

	protected String statusMessage;

	protected Locale locale;

	protected String contentType;

	public MockHttpServletResponse() {
		headers = new HashMap<String, String>();
		cookies = new HashSet<Cookie>();
		status = 200;
		statusMessage = "OK";
	}

	public void addCookie(Cookie cookie) {
		cookies.add(cookie);
	}

	public void addDateHeader(String key, long value) {
		headers.put(key, "" + value);
	}

	public void addHeader(String key, String value) {
		headers.put(key, value);
	}

	public void addIntHeader(String key, int value) {
		headers.put(key, "" + value);
	}

	public boolean containsHeader(String key) {
		return headers.containsKey(key);
	}

	public String encodeRedirectURL(String arg0) {
		throw new NoImplException();
	}

	public String encodeRedirectUrl(String arg0) {
		throw new NoImplException();
	}

	public String encodeURL(String arg0) {
		throw new NoImplException();
	}

	public String encodeUrl(String arg0) {
		throw new NoImplException();
	}

	public void sendError(int error) throws IOException {
		throw new NoImplException();
	}

	public void sendError(int arg0, String arg1) throws IOException {
		throw new NoImplException();
	}

	public void sendRedirect(String value) throws IOException {
		headers.put("Location", "" + value);
	}

	public void setDateHeader(String key, long value) {
		headers.put(key, "" + value);
	}

	public void setHeader(String key, String value) {
		headers.put(key, value);
	}

	public void setIntHeader(String key, int value) {
		headers.put(key, "" + value);
	}

	public void setStatus(int status) {
		this.status = status;
	}

	public void setStatus(int status, String statusMessage) {
		this.status = status;
		this.statusMessage = statusMessage;
	}

	public void flushBuffer() throws IOException {
		getWriter().flush();
	}

	public int getBufferSize() {
		return stream.size();
	}

	public String getCharacterEncoding() {
		return characterEncoding;
	}

	public String getContentType() {
		return contentType;
	}

	public Locale getLocale() {
		return locale;
	}

	public ServletOutputStream getOutputStream() throws IOException {
		throw new NoImplException();
	}

	public PrintWriter getWriter() throws IOException {
		if (writer == null) {
			writer = new PrintWriter(new OutputStreamWriter(stream,
					characterEncoding));
		}
		return writer;
	}

	public boolean isCommitted() {
		return false;
	}

	public void reset() {
		stream.reset();
	}

	public void resetBuffer() {
		stream.reset();
	}

	public void setBufferSize(int arg0) {

	}

	protected String characterEncoding = "UTF-8";

	public void setCharacterEncoding(String characterEncoding) {
		this.characterEncoding = characterEncoding;
	}

	public void setContentLength(int arg0) {

	}

	public void setContentType(String contentType) {
		this.contentType = contentType;
	}

	public void setLocale(Locale locale) {
		this.locale = locale;
	}

	public int getStatus() {
		return status;
	}

	public String getStatusMessage() {
		return statusMessage;
	}

	public String getAsString() {
		String ret = null;
		try {
			getWriter().flush();
			ret = stream.toString(characterEncoding);
		} catch (UnsupportedEncodingException e) {

		} catch (IOException e) {

		}
		return ret;
	}

	public int getAsInt() {
		return Integer.parseInt(getAsString());
	}

	public long getAsLong() {
		return Long.parseLong(getAsString());
	}

	// public <T> T getAs(Class<T> type) {
	//
	// }

	public String getHeader(String key) {
		return headers.get(key);
	}

	@Override
	public Collection<String> getHeaders(String name) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Collection<String> getHeaderNames() {
		// TODO Auto-generated method stub
		return null;
	}
}
