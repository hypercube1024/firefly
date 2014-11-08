package com.fireflysource.demo.controller;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.Writer;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import javax.servlet.AsyncContext;
import javax.servlet.AsyncEvent;
import javax.servlet.AsyncListener;
import javax.servlet.ServletException;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.Part;

import com.firefly.annotation.Controller;
import com.firefly.annotation.HttpParam;
import com.firefly.annotation.RequestMapping;
import com.firefly.mvc.web.HttpMethod;
import com.firefly.mvc.web.View;
import com.firefly.mvc.web.view.JsonView;
import com.firefly.mvc.web.view.StaticFileView;
import com.firefly.mvc.web.view.TemplateView;
import com.firefly.mvc.web.view.TextView;
import com.firefly.utils.log.Log;
import com.firefly.utils.log.LogFactory;
import com.fireflysource.demo.model.GoodsInformation;


@Controller
public class GoodsController {
	
	private static Log log = LogFactory.getInstance().getLog("demo-log");
	
	private Map<String, GoodsInformation> map = new ConcurrentHashMap<String, GoodsInformation>();
	
	@RequestMapping(value = "/goods/edit")
	public View editGoods(HttpServletRequest request) {
		return new TemplateView("/goods/edit.html");
	}

	@RequestMapping(value = "/goods/post", method=HttpMethod.POST)
	public View postGoods(@HttpParam GoodsInformation goodsInformation) {
		map.put(goodsInformation.getTitle(), goodsInformation);
		return new TextView("ok: " + goodsInformation);
	}
	
	@RequestMapping(value = "/goods/get")
	public View getJson(HttpServletRequest request) {
		String title = request.getParameter("title");
		log.info("get json title -> {}", title);
		if(title == null) {
			Map<String, String> ret = new HashMap<String, String>();
			ret.put("result", "error");
			return new JsonView(ret);
		}
		return new JsonView(map.get(title));
	}
	
	@RequestMapping(value = "/goods/introductions")
	public View getFile() {
		return new StaticFileView("/file-test.html");
	}
	
	@RequestMapping(value = "/goods/information")
	public View getGoodsInformation() {
		log.info("goods information controller");
		return new View(){

			@Override
			public void render(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
				GoodsInformation information = new GoodsInformation();
				information.setTitle("Firefly documents");
				information.setPrice(999.9);
				information.setStockNumber(1);
				
				Writer writer = response.getWriter();
				try {
					writer.write(information.toString());
				} finally {
					writer.close();
				}
				
			}};
	}
	
	@RequestMapping(value = "/goods/uploading")
	public View intoUploadingPage() {
		return new TemplateView("/goods/uploading.html");
	}
	
	@RequestMapping(value = "/goods/upload", method=HttpMethod.POST)
	public View uploadGoodsInformation(HttpServletRequest request) {
		try {
			Part part = request.getPart("grid");
			part.write("/Users/qiupengtao/goods/" + part.getName());
		} catch (Throwable e) {
			log.error("uploading failure!", e);
		}
		return new TextView("uploading success!");	
	}
	
	@RequestMapping(value = "/goods/asyncNotice")
	public View getInformationAsynchronously(HttpServletRequest request, HttpServletResponse response) {
		PrintWriter writer = null;
		try {
			writer = response.getWriter();
		} catch (IOException e) {
			log.error("get writer failure", e);
		}
		writer.println("start asynchronous notice");
		writer.flush();
		request.startAsync();
		request.getAsyncContext().addListener(new TestAsyncListener());
		request.getAsyncContext().start(new TestRunnable(request.getAsyncContext(), "asynchronous notice of goods information"));
		return null;
	}
	
	private class TestRunnable implements Runnable {
		private AsyncContext context;
		private String name;

		public TestRunnable(AsyncContext context, String name) {
			this.context = context;
			this.name = name;
		}
		
		@Override
		public void run() {
			
			ServletResponse res = context.getResponse();
			PrintWriter writer = null;
			try {
				writer = res.getWriter();
			} catch (IOException e) {
				e.printStackTrace();
			}
			
			writer.println(name + " ......");
			writer.flush();
			try {
				Thread.sleep(5000);
				
				writer.println(name + ": received a async message");
				writer.flush();
				context.complete();
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
			
		}
		
	}
	
	private class TestAsyncListener implements AsyncListener {

		@Override
		public void onTimeout(AsyncEvent event) throws IOException {
			ServletResponse res = event.getSuppliedResponse();
			PrintWriter writer = null;
			try {
				writer = res.getWriter();
			} catch (IOException e) {
				e.printStackTrace();
			}
			writer.println("async context timeout");
			writer.flush();
			writer.close();
		}
		
		@Override
		public void onStartAsync(AsyncEvent event) throws IOException {
			ServletResponse res = event.getSuppliedResponse();
			PrintWriter writer = null;
			try {
				writer = res.getWriter();
			} catch (IOException e) {
				e.printStackTrace();
			}
			writer.println("start async context");
			writer.flush();
		}
		
		@Override
		public void onError(AsyncEvent event) throws IOException {
			ServletResponse res = event.getSuppliedResponse();
			PrintWriter writer = null;
			try {
				writer = res.getWriter();
			} catch (IOException e) {
				e.printStackTrace();
			}
			writer.println("async context error");
			writer.flush();
			
		}
		
		@Override
		public void onComplete(AsyncEvent event) throws IOException {
			ServletResponse res = event.getSuppliedResponse();
			PrintWriter writer = null;
			try {
				writer = res.getWriter();
			} catch (IOException e) {
				e.printStackTrace();
			}
			writer.println("async context complete");
			writer.close();
			
		}
	}
}
