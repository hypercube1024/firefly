package com.firefly.server.http;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.nio.ByteBuffer;

import com.firefly.net.Decoder;
import com.firefly.net.Session;
import com.firefly.server.io.ByteArrayPipedStream;
import com.firefly.server.io.FilePipedStream;
import com.firefly.utils.StringUtils;
import com.firefly.utils.VerifyUtils;
import com.firefly.utils.log.Log;
import com.firefly.utils.log.LogFactory;

public class HttpDecoder implements Decoder {
	private static Log log = LogFactory.getInstance().getLog("firefly-system");
	private Config config;
	private AbstractHttpDecoder[] httpDecode = {new RequestLineDecoder(), new HeadDecoder(), new BodyDecoder()};
	public static final String HTTP_REQUEST = "http_req";
	public static final String REMAIN_DATA = "remain_data";
	private static final byte LINE_LIMITOR = '\n';

	public HttpDecoder(Config config) {
		this.config = config;
	}

	@Override
	public void decode(ByteBuffer buf, Session session) throws Throwable {
		ByteBuffer now = getBuffer(buf, session);
		HttpServletRequestImpl req = getHttpServletRequestImpl(session);
		httpDecode[req.status].decode0(now, session, req);
	}

	private ByteBuffer getBuffer(ByteBuffer buf, Session session) {
		ByteBuffer now = buf;
		ByteBuffer prev = (ByteBuffer) session.getAttribute(REMAIN_DATA);

		if (prev != null) {
			session.removeAttribute(REMAIN_DATA);
			now = (ByteBuffer) ByteBuffer
					.allocate(prev.remaining() + buf.remaining()).put(prev)
					.put(buf).flip();
		}
		return now;
	}

	private HttpServletRequestImpl getHttpServletRequestImpl(Session session) {
		HttpServletRequestImpl req = (HttpServletRequestImpl) session.getAttribute(HTTP_REQUEST);
		if (req == null) {
			req = new HttpServletRequestImpl(session, config);
			session.setAttribute(HTTP_REQUEST, req);
		}
		return req;
	}

	abstract private class AbstractHttpDecoder {
		private void decode0(ByteBuffer now, Session session, HttpServletRequestImpl req) throws Throwable {
			DecodeStatus status = decode(now, session, req);
			switch (status) {
			case INIT:
				init(now.slice(), session, req);
				break;
			case NEXT:
				next(now.slice(), session, req);
				break;
			case BUFFER_UNDERFLOW:
				save(now, session);
				break;
			case ERROR:
				responseError(session, req);
				break;
			case COMPLETE:
				decodeComplete(session, req);
				break;
			default:
				break;
			}
		}
		
		private void init(ByteBuffer buf, Session session, HttpServletRequestImpl req) throws Throwable {
			decodeComplete(session, req);
			HttpServletRequestImpl newReq = getHttpServletRequestImpl(session);
			httpDecode[newReq.status].decode0(buf, session, newReq);
		}
		
		private void next(ByteBuffer buf, Session session, HttpServletRequestImpl req) throws Throwable {
			req.status++;
			if (req.status < httpDecode.length) {
				req.offset = 0;
				httpDecode[req.status].decode0(buf, session, req);
			}
		}

		private void save(ByteBuffer buf, Session session) {
			if (buf.hasRemaining())
				session.setAttribute(REMAIN_DATA, buf);
		}

		private void responseError(Session session, HttpServletRequestImpl req) {
			finish(session, req);
			req.response.scheduleSendError();
			req.decodeFinish();
		}

		private void decodeComplete(Session session, HttpServletRequestImpl req) {
			finish(session, req);
			req.decodeFinish();
		}
		
		private void finish(Session session, HttpServletRequestImpl req) {
			session.removeAttribute(REMAIN_DATA);
			session.removeAttribute(HTTP_REQUEST);
			req.status = httpDecode.length;
		}

		abstract protected DecodeStatus decode(ByteBuffer buf, Session session, HttpServletRequestImpl req) throws Throwable;
	}
	
	private enum DecodeStatus {
		NEXT, BUFFER_UNDERFLOW, INIT, COMPLETE, ERROR
	}

	private class RequestLineDecoder extends AbstractHttpDecoder {

		@Override
		public DecodeStatus decode(ByteBuffer buf, Session session, HttpServletRequestImpl req) throws Throwable {

			if (req.offset >= config.getMaxRequestLineLength()) {
				String msg = "request line length is " + req.offset
						+ ", it more than " + config.getMaxRequestLineLength()
						+ "|" + session.getRemoteAddress();
				log.error(msg);
				req.response.setStatus(414);
				req.response.systemResponseContent = msg;
				return DecodeStatus.ERROR;
			}

			int len = buf.remaining();
			for (; req.offset < len; req.offset++) {
				if (buf.get(req.offset) == LINE_LIMITOR) {
					byte[] data = new byte[req.offset + 1];
					buf.get(data);
					String requestLine = new String(data, config.getEncoding()).trim();
					if (VerifyUtils.isEmpty(requestLine)) {
						String msg = "request line length is 0|"
								+ session.getRemoteAddress();
						log.error(msg);
						req.response.setStatus(400);
						req.response.systemResponseContent = msg;
						return DecodeStatus.ERROR;
					}

					String[] reqLine = StringUtils.split(requestLine, ' ');
					if (reqLine.length != 3) {
						String msg = "request line format error: "
								+ requestLine + "|"
								+ session.getRemoteAddress();
						log.error(msg);
						req.response.setStatus(400);
						req.response.systemResponseContent = msg;
						return DecodeStatus.ERROR;
					}

					int s = reqLine[1].indexOf('?');
					req.method = reqLine[0].toUpperCase();
					if (s > 0) {
						req.requestURI = reqLine[1].substring(0, s);
						req.queryString = reqLine[1].substring(s + 1, reqLine[1].length());
					} else {
						req.requestURI = reqLine[1];
					}
					req.protocol = reqLine[2];
					return DecodeStatus.NEXT;
				}
			}
			return DecodeStatus.BUFFER_UNDERFLOW;
		}

	}

	private class HeadDecoder extends AbstractHttpDecoder {

		@Override
		public DecodeStatus decode(ByteBuffer buf, Session session, HttpServletRequestImpl req) throws Throwable {
			int len = buf.remaining();

			for (int i = req.offset, p = 0; i < len; i++) {
				if (buf.get(i) == LINE_LIMITOR) {
					int parseLen = i - p + 1;
					req.headLength += parseLen;

					if (req.headLength >= config.getMaxRequestHeadLength()) {
						String msg = "request head length is " + req.headLength
								+ ", it more than "
								+ config.getMaxRequestHeadLength() + "|"
								+ session.getRemoteAddress() + "|"
								+ req.getRequestURI();
						log.error(msg);
						req.response.setStatus(400);
						req.response.systemResponseContent = msg;
						return DecodeStatus.ERROR;
					}

					byte[] data = new byte[parseLen];
					buf.get(data);
					String headLine = new String(data, config.getEncoding()).trim();
					p = i + 1;

					if (VerifyUtils.isEmpty(headLine)) { // http head decode finish
						if(Monitor.CONN_COUNT.get() > config.getMaxConnections()) {
							String msg = "connections count more than " + config.getMaxConnections();
							log.error(msg);
							req.response.setHeader("Retry-After", "60");
							req.response.setStatus(503);
							req.response.systemResponseContent = msg;
							return DecodeStatus.ERROR;
						}
						
						if (req.getMethod().equals("POST") || req.getMethod().equals("PUT")) {
							return DecodeStatus.NEXT;
						}
						
						return buf.hasRemaining() ? DecodeStatus.INIT : DecodeStatus.COMPLETE;
					}
					
					int h = headLine.indexOf(':');
					if (h <= 0) {
						String msg = "head line format error: " + headLine
								+ "|" + session.getRemoteAddress() + "|"
								+ req.getRequestURI();
						log.error(msg);
						req.response.setStatus(400);
						req.response.systemResponseContent = msg;
						return DecodeStatus.ERROR;
					}

					String name = headLine.substring(0, h).toLowerCase().trim();
					String value = headLine.substring(h + 1).trim();
					req.headMap.put(name, value);
					req.offset = len - i - 1;

					if (name.equals("expect") && value.startsWith("100-") && req.getProtocol().equals("HTTP/1.1"))
						response100Continue(session);
				}
			}
			return DecodeStatus.BUFFER_UNDERFLOW;
		}

		private void response100Continue(Session session) throws UnsupportedEncodingException {
			session.encode(ByteBuffer.wrap("HTTP/1.1 100 Continue\r\n\r\n".getBytes(config.getEncoding())));
		}
	}

	private class BodyDecoder extends AbstractHttpDecoder {

		@Override
		public DecodeStatus decode(ByteBuffer buf, Session session,
				HttpServletRequestImpl req) throws Throwable {
			int contentLength = req.getContentLength();
			if (contentLength > 0) {
				if (contentLength > config.getMaxUploadLength()) {
					String msg = "body length is " + contentLength
							+ " , it more than " + config.getMaxUploadLength()
							+ "|" + session.getRemoteAddress() + "|"
							+ req.getRequestURI();
					log.error(msg);
					req.response.setStatus(400);
					req.response.systemResponseContent = msg;
					return DecodeStatus.ERROR;
				}

				if (req.bodyPipedStream == null) { // the first into body decode status, it neet create piped stream
					if(contentLength >= config.getHttpBodyThreshold()) {
						log.info("content length [{}] more than threshold [{}]", contentLength , config.getHttpBodyThreshold());
						req.bodyPipedStream = new FilePipedStream(config.getTempdir());
					} else {
						req.bodyPipedStream = new ByteArrayPipedStream(contentLength);
					}
				}
				
				req.offset += buf.remaining();
				byte[] data = new byte[buf.remaining()];
				buf.get(data);
				try {
					req.bodyPipedStream.getOutputStream().write(data);
				} catch(IOException e) {
					log.error("receive body data error", e);
					req.bodyPipedStream.getOutputStream().close();
				}

				if (req.offset >= contentLength) {
					req.bodyPipedStream.getOutputStream().close();
					return buf.hasRemaining() ? DecodeStatus.INIT : DecodeStatus.COMPLETE;
				}
			} else {
				return buf.hasRemaining() ? DecodeStatus.INIT : DecodeStatus.COMPLETE;
			}
			return DecodeStatus.BUFFER_UNDERFLOW;
		}

	}

}
