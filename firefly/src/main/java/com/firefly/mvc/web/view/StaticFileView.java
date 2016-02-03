package com.firefly.mvc.web.view;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;
import java.util.Set;

import javax.servlet.ServletException;
import javax.servlet.ServletOutputStream;
import javax.servlet.WriteListener;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.firefly.mvc.web.Constants;
import com.firefly.mvc.web.FileAccessFilter;
import com.firefly.mvc.web.View;
import com.firefly.mvc.web.servlet.SystemHtmlPage;
import com.firefly.server.exception.HttpServerException;
import com.firefly.utils.RandomUtils;
import com.firefly.utils.StringUtils;
import com.firefly.utils.VerifyUtils;
import com.firefly.utils.concurrent.Callback;
import com.firefly.utils.concurrent.CountingCallback;
import com.firefly.utils.io.BufferReaderHandler;
import com.firefly.utils.io.BufferUtils;
import com.firefly.utils.io.FileUtils;
import com.firefly.utils.log.Log;
import com.firefly.utils.log.LogFactory;

public class StaticFileView implements View {

	private static Log log = LogFactory.getInstance().getLog("firefly-system");
	public static final String CRLF = "\r\n";
	private static Set<String> ALLOW_METHODS = new HashSet<String>(Arrays.asList("GET", "POST", "HEAD"));
	private static String RANGE_ERROR_HTML = SystemHtmlPage.systemPageTemplate(
			HttpServletResponse.SC_REQUESTED_RANGE_NOT_SATISFIABLE,
			"None of the range-specifier values in the Range request-header field overlap the current extent of the selected resource.");
	private static int MAX_RANGE_NUM;
	private static String SERVER_HOME;
	private static String CHARACTER_ENCODING = "UTF-8";
	private static String TEMPLATE_PATH;
	private static FileAccessFilter FILE_ACCESS_FILTER = new FileAccessFilter() {
		@Override
		public String doFilter(HttpServletRequest request, HttpServletResponse response, String path) {
			return path;
		}
	};
	private final String inputPath;

	public StaticFileView(String path) {
		this.inputPath = path;
	}

	public static void init(String characterEncoding, FileAccessFilter fileAccessFilter, String serverHome,
			int maxRangeNum, String tempPath) {
		if (VerifyUtils.isNotEmpty(characterEncoding)) {
			CHARACTER_ENCODING = characterEncoding;
		}
		if (fileAccessFilter != null) {
			FILE_ACCESS_FILTER = fileAccessFilter;
		}
		SERVER_HOME = serverHome;
		MAX_RANGE_NUM = maxRangeNum;
		if (TEMPLATE_PATH == null && tempPath != null)
			TEMPLATE_PATH = tempPath;
	}

	/**
	 * It checks input path, if this method returns true, the path is legal. The
	 * client can only visit all the subdirectories of server root directory.
	 * 
	 * @param path
	 *            The file path
	 * @return Return true, if the path is legal, else return false.
	 */
	public static boolean checkPath(String path) {
		if (path.length() < 3)
			return true;

		if (path.charAt(0) == '/') {
			for (int i = 1; i < path.length(); i++) {
				char ch = path.charAt(i);
				if (ch == '/')
					continue;

				if (ch != '.')
					continue;

				if (i + 1 < path.length()) {
					char next = path.charAt(i + 1);
					if (ch == '.' && next == '/') {
						return false;
					}

					if (i + 2 < path.length()) {
						if (ch == '.' && next == '.' && path.charAt(i + 2) == '/')
							return false;
					}
				}
			}
		}
		return true;
	}

	@Override
	public void render(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		if (!checkPath(inputPath) || inputPath.startsWith(TEMPLATE_PATH)) {
			SystemHtmlPage.responseSystemPage(request, response, CHARACTER_ENCODING, HttpServletResponse.SC_NOT_FOUND,
					request.getRequestURI() + " not found");
			return;
		}

		if (!ALLOW_METHODS.contains(request.getMethod())) {
			response.setHeader("Allow", "GET,POST,HEAD");
			SystemHtmlPage.responseSystemPage(request, response, CHARACTER_ENCODING,
					HttpServletResponse.SC_METHOD_NOT_ALLOWED, "Only support GET, POST or HEAD method");
			return;
		}

		String path = FILE_ACCESS_FILTER.doFilter(request, response, inputPath);
		if (VerifyUtils.isEmpty(path))
			return;

		File file = new File(SERVER_HOME, path);
		if (!file.exists() || file.isDirectory()) {
			SystemHtmlPage.responseSystemPage(request, response, CHARACTER_ENCODING, HttpServletResponse.SC_NOT_FOUND,
					request.getRequestURI() + " not found");
			return;
		}

		String fileSuffix = getFileSuffix(file.getName()).toLowerCase();
		String contentType = Constants.MIME.get(fileSuffix);
		if (contentType == null) {
			response.setContentType("application/octet-stream");
			response.setHeader("Content-Disposition", "attachment; filename=" + file.getName());
		} else {
			String[] type = StringUtils.split(contentType, '/');
			if ("application".equals(type[0])) {
				response.setHeader("Content-Disposition", "attachment; filename=" + file.getName());
			} else if ("text".equals(type[0])) {
				contentType += "; charset=" + CHARACTER_ENCODING;
			}
			response.setContentType(contentType);
		}

		try (FileServletOutputStream out = new FileServletOutputStream(request, response, response.getOutputStream())) {
			String range = request.getHeader("Range");
			if (range == null) {
				response.setStatus(HttpServletResponse.SC_OK);
				out.write(file);
			} else {
				String[] rangesSpecifier = StringUtils.split(range, '=');
				if (rangesSpecifier.length != 2) {
					response.setStatus(HttpServletResponse.SC_REQUESTED_RANGE_NOT_SATISFIABLE);
					out.write(RANGE_ERROR_HTML.getBytes(CHARACTER_ENCODING));
					return;
				}

				long fileLen = file.length();

				String byteRangeSet = rangesSpecifier[1].trim();
				String[] byteRangeSets = StringUtils.split(byteRangeSet, ',');
				if (byteRangeSets.length > 1) { // multipart/byteranges
					String boundary = "ff10" + RandomUtils.randomString(13);
					if (byteRangeSets.length > MAX_RANGE_NUM) {
						log.error("multipart range more than {}", MAX_RANGE_NUM);
						response.setStatus(HttpServletResponse.SC_REQUESTED_RANGE_NOT_SATISFIABLE);
						out.write(RANGE_ERROR_HTML.getBytes(CHARACTER_ENCODING));
						return;
					}
					// multipart output
					List<MultipartByteranges> tmpByteRangeSets = new ArrayList<MultipartByteranges>(MAX_RANGE_NUM);
					// long otherLen = 0;
					for (String t : byteRangeSets) {
						String tmp = t.trim();
						String[] byteRange = StringUtils.split(tmp, '-');
						if (byteRange.length == 1) {
							long pos = Long.parseLong(byteRange[0].trim());
							if (pos == 0)
								continue;
							if (tmp.charAt(0) == '-') {
								long lastBytePos = fileLen - 1;
								long firstBytePos = lastBytePos - pos + 1;
								if (firstBytePos > lastBytePos)
									continue;

								MultipartByteranges multipartByteranges = getMultipartByteranges(contentType,
										firstBytePos, lastBytePos, fileLen, boundary);
								tmpByteRangeSets.add(multipartByteranges);
							} else if (tmp.charAt(tmp.length() - 1) == '-') {
								long firstBytePos = pos;
								long lastBytePos = fileLen - 1;
								if (firstBytePos > lastBytePos)
									continue;

								MultipartByteranges multipartByteranges = getMultipartByteranges(contentType,
										firstBytePos, lastBytePos, fileLen, boundary);
								tmpByteRangeSets.add(multipartByteranges);
							}
						} else {
							long firstBytePos = Long.parseLong(byteRange[0].trim());
							long lastBytePos = Long.parseLong(byteRange[1].trim());
							if (firstBytePos > fileLen || firstBytePos >= lastBytePos)
								continue;

							MultipartByteranges multipartByteranges = getMultipartByteranges(contentType, firstBytePos,
									lastBytePos, fileLen, boundary);
							tmpByteRangeSets.add(multipartByteranges);
						}
					}

					if (tmpByteRangeSets.size() > 0) {
						response.setStatus(HttpServletResponse.SC_PARTIAL_CONTENT);
						response.setHeader("Accept-Ranges", "bytes");
						response.setHeader("Content-Type", "multipart/byteranges; boundary=" + boundary);

						for (MultipartByteranges m : tmpByteRangeSets) {
							long length = m.lastBytePos - m.firstBytePos + 1;
							out.write(m.head.getBytes(CHARACTER_ENCODING));
							out.write(file, m.firstBytePos, length);
						}

						out.write((CRLF + "--" + boundary + "--" + CRLF).getBytes(CHARACTER_ENCODING));
						log.debug("multipart download|{}", range);
					} else {
						response.setStatus(HttpServletResponse.SC_REQUESTED_RANGE_NOT_SATISFIABLE);
						out.write(RANGE_ERROR_HTML.getBytes(CHARACTER_ENCODING));
						return;
					}
				} else {
					String tmp = byteRangeSets[0].trim();
					String[] byteRange = StringUtils.split(tmp, '-');
					if (byteRange.length == 1) {
						long pos = Long.parseLong(byteRange[0].trim());
						if (pos == 0) {
							response.setStatus(HttpServletResponse.SC_REQUESTED_RANGE_NOT_SATISFIABLE);
							out.write(RANGE_ERROR_HTML.getBytes(CHARACTER_ENCODING));
							return;
						}

						if (tmp.charAt(0) == '-') {
							long lastBytePos = fileLen - 1;
							long firstBytePos = lastBytePos - pos + 1;
							writePartialFile(request, response, out, file, firstBytePos, lastBytePos, fileLen);
						} else if (tmp.charAt(tmp.length() - 1) == '-') {
							writePartialFile(request, response, out, file, pos, fileLen - 1, fileLen);
						} else {
							response.setStatus(HttpServletResponse.SC_REQUESTED_RANGE_NOT_SATISFIABLE);
							out.write(RANGE_ERROR_HTML.getBytes(CHARACTER_ENCODING));
							return;
						}
					} else {
						long firstBytePos = Long.parseLong(byteRange[0].trim());
						long lastBytePos = Long.parseLong(byteRange[1].trim());
						if (firstBytePos > fileLen || firstBytePos >= lastBytePos) {
							response.setStatus(HttpServletResponse.SC_REQUESTED_RANGE_NOT_SATISFIABLE);
							out.write(RANGE_ERROR_HTML.getBytes(CHARACTER_ENCODING));
							return;
						}

						if (lastBytePos >= fileLen)
							lastBytePos = fileLen - 1;

						writePartialFile(request, response, out, file, firstBytePos, lastBytePos, fileLen);
					}
					log.debug("single range download|{}", range);
				}
			}
		} catch (Throwable e) {
			log.error("static file output exception", e);
			throw new HttpServerException("get static file output stream error");
		}

	}

	private void writePartialFile(HttpServletRequest request, HttpServletResponse response, FileServletOutputStream out,
			File file, long firstBytePos, long lastBytePos, long fileLen) throws Throwable {

		long length = lastBytePos - firstBytePos + 1;
		if (length <= 0) {
			response.setStatus(HttpServletResponse.SC_REQUESTED_RANGE_NOT_SATISFIABLE);
			out.write(RANGE_ERROR_HTML.getBytes(CHARACTER_ENCODING));
			return;
		}
		response.setStatus(HttpServletResponse.SC_PARTIAL_CONTENT);
		response.setHeader("Accept-Ranges", "bytes");
		response.setHeader("Content-Range", "bytes " + firstBytePos + "-" + lastBytePos + "/" + fileLen);
		out.write(file, firstBytePos, length);
	}

	public static String getFileSuffix(String name) {
		if (name.charAt(name.length() - 1) == '.')
			return "*";

		for (int i = name.length() - 2; i >= 0; i--) {
			if (name.charAt(i) == '.') {
				return name.substring(i + 1, name.length());
			}
		}
		return "*";
	}

	private class MultipartByteranges {
		public String head;
		public long firstBytePos, lastBytePos;
	}

	private MultipartByteranges getMultipartByteranges(String contentType, long firstBytePos, long lastBytePos,
			long fileLen, String boundary) {
		MultipartByteranges ret = new MultipartByteranges();
		ret.firstBytePos = firstBytePos;
		ret.lastBytePos = lastBytePos;
		ret.head = CRLF + "--" + boundary + CRLF + "Content-Type: " + contentType + CRLF + "Content-range: bytes "
				+ firstBytePos + "-" + lastBytePos + "/" + fileLen + CRLF + CRLF;
		return ret;
	}

	private class FileServletOutputStream extends ServletOutputStream {

		private final HttpServletRequest request;
		private final HttpServletResponse response;
		private final ServletOutputStream out;
		private final Queue<ChunkedData> queue = new LinkedList<ChunkedData>();
		private long size;

		public FileServletOutputStream(HttpServletRequest request, HttpServletResponse response,
				ServletOutputStream out) {
			this.request = request;
			this.response = response;
			this.out = out;
		}

		@Override
		public boolean isReady() {
			return out.isReady();
		}

		@Override
		public void setWriteListener(WriteListener writeListener) {
			out.setWriteListener(writeListener);
		}

		@Override
		public void write(int b) throws IOException {
			queue.offer(new ByteChunkedData((byte) b));
			size++;
		}

		@Override
		public void write(byte[] array, int offset, int length) throws IOException {
			ChunkedData c = new ByteArrayChunkedData(array, offset, length);
			queue.offer(c);
			size += length;
		}

		public void write(File file) throws IOException {
			long len = file.length();
			SequenceAccessFileChunkedData data = new SequenceAccessFileChunkedData(file, len);
			queue.offer(data);
			size += data.getLength();
		}

		public void write(File file, long off, long len) throws IOException {
			queue.offer(new RandomAccessFileChunkedData(file, off, len));
			size += len;
		}

		@Override
		public void print(String string) throws IOException {
			write(string.getBytes(response.getCharacterEncoding()));
		}

		@Override
		public void flush() throws IOException {
			out.flush();
		}

		@Override
		public void close() throws IOException {
			if (!response.isCommitted()) {
				response.setHeader("Content-Length", String.valueOf(size));
			}

			if (size > 0) {
				if (request.getMethod().equals("HEAD"))
					queue.clear();
				else {
					for (ChunkedData d = null; (d = queue.poll()) != null;)
						d.write();
				}

				size = 0;
			}
			out.close();
		}

		private class FileBufferReaderHandler implements BufferReaderHandler {

			private final long len;

			public FileBufferReaderHandler(long len) {
				this.len = len;
			}

			@Override
			public void readBuffer(ByteBuffer buf, CountingCallback countingCallback, long count) throws IOException {
				log.debug("write file,  count: {} , lenth: {}", count, len);
				out.write(BufferUtils.toArray(buf));
			}

		}

		private class SequenceAccessFileChunkedData extends ChunkedData {

			private final File file;
			private final long len;

			public SequenceAccessFileChunkedData(File file, long len) {
				this.file = file;
				this.len = len;
			}

			public long getLength() {
				return len;
			}

			@Override
			public void write() throws IOException {
				try (FileInputStream input = new FileInputStream(file)) {
					try (FileChannel fc = input.getChannel()) {
						FileUtils.transferTo(fc, len, Callback.NOOP, new FileBufferReaderHandler(len));
					}
				}
			}

		}

		private class RandomAccessFileChunkedData extends ChunkedData {

			private final long len;
			private final long off;
			private final File file;

			public RandomAccessFileChunkedData(File file, long off, long len) {
				this.off = off;
				this.len = len;
				this.file = file;
			}

			@Override
			public void write() throws IOException {
				try (RandomAccessFile raf = new RandomAccessFile(file, "r")) {
					try (FileChannel fc = raf.getChannel()) {
						FileUtils.transferTo(fc, off, len, Callback.NOOP, new FileBufferReaderHandler(len));
					}
				}
			}

		}

		private class ByteChunkedData extends ChunkedData {
			private final byte b;

			public ByteChunkedData(byte b) {
				this.b = b;
			}

			@Override
			public void write() throws IOException {
				out.write(b);
			}
		}

		private class ByteArrayChunkedData extends ChunkedData {
			private final byte[] b;
			private final int len;
			private final int off;

			public ByteArrayChunkedData(byte[] b, int off, int len) {
				this.b = b;
				this.off = off;
				this.len = len;
			}

			@Override
			public void write() throws IOException {
				out.write(b, off, len);
			}
		}

		abstract private class ChunkedData {
			abstract public void write() throws IOException;
		}

	}

}
