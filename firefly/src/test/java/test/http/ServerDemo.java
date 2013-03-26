package test.http;

import java.io.File;

import com.firefly.mvc.web.servlet.SystemHtmlPage;
import com.firefly.server.ServerBootstrap;

public class ServerDemo {

	public static void main(String[] args) throws Throwable {
//		start1();
		start2();
//		System.out.println(ServerBootstrap.class.getResource("/page/template").toURI());
		
//		File file = new File("/Users/qiupengtao", "testUpload1.txt");
//		FileOutputStream out = new FileOutputStream(file);
//		
//		StringBuilder str = new StringBuilder();
//		for (int i = 0; i < 256; i++) {
//			str.append('a');
//		}
//		str.append("\r\n");
//		out.write(str.toString().getBytes("UTF-8"));
//		out.close();
	}
	
	public static void start1() throws Throwable {
		SystemHtmlPage.addErrorPage(404, "/error/err404.html");
		SystemHtmlPage.addErrorPage(500, "/error/err500.html");
		String serverHome = new File(ServerBootstrap.class.getResource("/page").toURI()).getAbsolutePath();
		ServerBootstrap.start("firefly-server.xml", serverHome, "localhost", 6655);
	}
	
	public static void start2() {
		ServerBootstrap.start("firefly-server.xml");
	}

}
