package com.firefly.mvc.web.view;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.firefly.mvc.web.View;
import com.firefly.mvc.web.servlet.SystemHtmlPage;
import com.firefly.server.exception.HttpServerException;
import com.firefly.server.http.Config;
import com.firefly.server.http.Constants;
import com.firefly.server.http.HttpServletResponseImpl;
import com.firefly.server.io.StaticFileOutputStream;
import com.firefly.utils.RandomUtils;
import com.firefly.utils.StringUtils;
import com.firefly.utils.VerifyUtils;
import com.firefly.utils.log.Log;
import com.firefly.utils.log.LogFactory;

public class StaticFileView implements View {
	
	private static Log log = LogFactory.getInstance().getLog("firefly-system");
	public static final String CRLF = "\r\n";
	private static Set<String> ALLOW_METHODS = new HashSet<String>(Arrays.asList("GET", "POST", "HEAD"));
	private static String RANGE_ERROR_HTML = SystemHtmlPage.systemPageTemplate(416,
		"None of the range-specifier values in the Range request-header field overlap the current extent of the selected resource.");
	private static Config CONFIG;
	private static String TEMPLATE_PATH;
	private final String inputPath;
	
	public StaticFileView(String path) {
		this.inputPath = path;
	}
	
	public static void init(Config serverConfig, String tempPath) {
		if(CONFIG == null && serverConfig != null)
			CONFIG = serverConfig;
		
		if(TEMPLATE_PATH == null && tempPath != null)
			TEMPLATE_PATH = tempPath;
	}
	
	/**
	 * It checks input path, if this method returns true, the path is legal. 
	 * The client can only visit all the subdirectories of server root directory. 
	 * @param path The file path
	 * @return Return true, if the path is legal, else return false. 
	 */
	public static boolean checkPath(String path) {
		if(path.length() < 3)
			return true;

		if(path.charAt(0) == '/') {
			for (int i = 1; i < path.length(); i++) {
				char ch = path.charAt(i);
			    if(ch == '/')
			    	continue;
			    
			    if(ch != '.')
			    	continue;
			   
			    if(i + 1 < path.length()) {
			    	char next = path.charAt(i + 1);
				    if(ch == '.' && next == '/') {
				    	 return false;
				    }
			     
			    	if(i + 2 < path.length()) {
			    		if(ch == '.' && next == '.' && path.charAt(i + 2) == '/')
			    			return false;
			    	}
			    }
			}
		}
		return true;
	}

	@Override
	public void render(HttpServletRequest request, HttpServletResponse response)
			throws ServletException, IOException {
		if(!checkPath(inputPath) || inputPath.startsWith(TEMPLATE_PATH)) {
			SystemHtmlPage.responseSystemPage(request, response,
					CONFIG.getEncoding(), HttpServletResponse.SC_NOT_FOUND,
					request.getRequestURI() + " not found");
			return;
		}
		
		if(!ALLOW_METHODS.contains(request.getMethod())) {
			response.setHeader("Allow", "GET,POST,HEAD");
			SystemHtmlPage.responseSystemPage(request, response, CONFIG.getEncoding(), 
					HttpServletResponse.SC_METHOD_NOT_ALLOWED, "Only support GET, POST or HEAD method");
			return;
		}
		
		String path = CONFIG.getFileAccessFilter().doFilter(request, response, inputPath);
		if (VerifyUtils.isEmpty(path))
			return;
		
		File file = new File(CONFIG.getServerHome(), path);
		if (!file.exists() || file.isDirectory()) {
			SystemHtmlPage.responseSystemPage(request, response,
					CONFIG.getEncoding(), HttpServletResponse.SC_NOT_FOUND,
					request.getRequestURI() + " not found");
			return;
		}

		String fileSuffix = getFileSuffix(file.getName()).toLowerCase();
		String contentType = Constants.MIME.get(fileSuffix);
		if (contentType == null) {
			response.setContentType("application/octet-stream");
			response.setHeader("Content-Disposition", "attachment; filename="
					+ file.getName());
		} else {
			String[] type = StringUtils.split(contentType, '/');
			if ("application".equals(type[0])) {
				response.setHeader("Content-Disposition",
						"attachment; filename=" + file.getName());
			} else if ("text".equals(type[0])) {
				contentType += "; charset=" + CONFIG.getEncoding();
			}
			response.setContentType(contentType);
		}

		StaticFileOutputStream out = null;
		try {
			out = ((HttpServletResponseImpl) response)
					.getStaticFileOutputStream();
			long fileLen = file.length();
			String range = request.getHeader("Range");
			if (range == null) {
				out.write(file);
			} else {
				String[] rangesSpecifier = StringUtils.split(range, '=');
				if (rangesSpecifier.length != 2) {
					response.setStatus(416);
					out.write(RANGE_ERROR_HTML.getBytes(CONFIG.getEncoding()));
					return;
				}

				String byteRangeSet = rangesSpecifier[1].trim();
				String[] byteRangeSets = StringUtils.split(byteRangeSet, ',');
				if (byteRangeSets.length > 1) { // multipart/byteranges
					String boundary = "ff10" + RandomUtils.randomString(13); 
					if (byteRangeSets.length > CONFIG.getMaxRangeNum()) {
						log.error("multipart range more than {}",
								CONFIG.getMaxRangeNum());
						response.setStatus(416);
						out.write(RANGE_ERROR_HTML.getBytes(CONFIG
								.getEncoding()));
						return;
					}
					// multipart output
					List<MultipartByteranges> tmpByteRangeSets = new ArrayList<MultipartByteranges>(
							CONFIG.getMaxRangeNum());
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

								MultipartByteranges multipartByteranges = getMultipartByteranges(
										contentType, firstBytePos, lastBytePos,
										fileLen, boundary);
								tmpByteRangeSets.add(multipartByteranges);
							} else if (tmp.charAt(tmp.length() - 1) == '-') {
								long firstBytePos = pos;
								long lastBytePos = fileLen - 1;
								if (firstBytePos > lastBytePos)
									continue;

								MultipartByteranges multipartByteranges = getMultipartByteranges(
										contentType, firstBytePos, lastBytePos,
										fileLen, boundary);
								tmpByteRangeSets.add(multipartByteranges);
							}
						} else {
							long firstBytePos = Long.parseLong(byteRange[0]
									.trim());
							long lastBytePos = Long.parseLong(byteRange[1]
									.trim());
							if (firstBytePos > fileLen
									|| firstBytePos >= lastBytePos)
								continue;

							MultipartByteranges multipartByteranges = getMultipartByteranges(
									contentType, firstBytePos, lastBytePos,
									fileLen, boundary);
							tmpByteRangeSets.add(multipartByteranges);
						}
					}

					if (tmpByteRangeSets.size() > 0) {
						response.setStatus(206);
						response.setHeader("Accept-Ranges", "bytes");
						response.setHeader("Content-Type",
								"multipart/byteranges; boundary=" + boundary);

						for (MultipartByteranges m : tmpByteRangeSets) {
							long length = m.lastBytePos - m.firstBytePos + 1;
							out.write(m.head.getBytes(CONFIG.getEncoding()));
							out.write(file, m.firstBytePos, length);
						}

						out.write((CRLF + "--" + boundary + "--" + CRLF)
								.getBytes(CONFIG.getEncoding()));
						log.debug("multipart download|{}", range);
					} else {
						response.setStatus(416);
						out.write(RANGE_ERROR_HTML.getBytes(CONFIG
								.getEncoding()));
						return;
					}
				} else {
					String tmp = byteRangeSets[0].trim();
					String[] byteRange = StringUtils.split(tmp, '-');
					if (byteRange.length == 1) {
						long pos = Long.parseLong(byteRange[0].trim());
						if (pos == 0) {
							response.setStatus(416);
							out.write(RANGE_ERROR_HTML.getBytes(CONFIG
									.getEncoding()));
							return;
						}

						if (tmp.charAt(0) == '-') {
							long lastBytePos = fileLen - 1;
							long firstBytePos = lastBytePos - pos + 1;
							writePartialFile(request, response, out, file,
									firstBytePos, lastBytePos, fileLen);
						} else if (tmp.charAt(tmp.length() - 1) == '-') {
							writePartialFile(request, response, out, file, pos,
									fileLen - 1, fileLen);
						} else {
							response.setStatus(416);
							out.write(RANGE_ERROR_HTML.getBytes(CONFIG
									.getEncoding()));
							return;
						}
					} else {
						long firstBytePos = Long.parseLong(byteRange[0].trim());
						long lastBytePos = Long.parseLong(byteRange[1].trim());
						if (firstBytePos > fileLen
								|| firstBytePos >= lastBytePos) {
							response.setStatus(416);
							out.write(RANGE_ERROR_HTML.getBytes(CONFIG
									.getEncoding()));
							return;
						}

						if (lastBytePos >= fileLen)
							lastBytePos = fileLen - 1;

						writePartialFile(request, response, out, file,
								firstBytePos, lastBytePos, fileLen);
					}
					log.debug("single range download|{}", range);
				}
			}
		} catch (Throwable e) {
			throw new HttpServerException("get static file output stream error");
		} finally {
			if (out != null)
				try {
					// System.out.println("close~~");
					out.close();
				} catch (IOException e) {
					throw new HttpServerException(
							"static file output stream close error");
				}
		}

	}
	
	private void writePartialFile(HttpServletRequest request,
			HttpServletResponse response, StaticFileOutputStream out,
			File file, long firstBytePos, long lastBytePos, long fileLen)
			throws Throwable {

		long length = lastBytePos - firstBytePos + 1;
		if (length <= 0) {
			response.setStatus(416);
			out.write(RANGE_ERROR_HTML.getBytes(CONFIG.getEncoding()));
			return;
		}
		response.setStatus(206);
		response.setHeader("Accept-Ranges", "bytes");
		response.setHeader("Content-Range", "bytes " + firstBytePos + "-"
				+ lastBytePos + "/" + fileLen);
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

	private MultipartByteranges getMultipartByteranges(String contentType,
			long firstBytePos, long lastBytePos, long fileLen, String boundary) {
		MultipartByteranges ret = new MultipartByteranges();
		ret.firstBytePos = firstBytePos;
		ret.lastBytePos = lastBytePos;
		ret.head = CRLF + "--" + boundary + CRLF + "Content-Type: "
				+ contentType + CRLF + "Content-range: bytes " + firstBytePos
				+ "-" + lastBytePos + "/" + fileLen + CRLF + CRLF;
		return ret;
	}

}
