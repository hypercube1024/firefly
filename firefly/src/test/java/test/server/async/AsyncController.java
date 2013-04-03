package test.server.async;

import java.io.IOException;
import java.io.PrintWriter;

import javax.servlet.AsyncContext;
import javax.servlet.AsyncEvent;
import javax.servlet.AsyncListener;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.firefly.annotation.Controller;
import com.firefly.annotation.RequestMapping;
import com.firefly.mvc.web.View;

@Controller
public class AsyncController {
	@RequestMapping(value = "/async/test")
	public View index(HttpServletRequest request, HttpServletResponse response) {
		PrintWriter writer = null;
		try {
			writer = response.getWriter();
		} catch (IOException e) {
			e.printStackTrace();
		}
		
		writer.println("start async controller");
		writer.flush();
		request.startAsync();
		request.getAsyncContext().addListener(new TestAsyncListener());
		request.getAsyncContext().start(new TestRunnable(request.getAsyncContext(), "foo"));
		
		return null;
	}
	
	@RequestMapping(value = "/async/timeout")
	public View timeout(HttpServletRequest request, HttpServletResponse response) {
		PrintWriter writer = null;
		try {
			writer = response.getWriter();
		} catch (IOException e) {
			e.printStackTrace();
		}
		
		writer.println("start async controller");
		writer.flush();
		request.startAsync();
		request.getAsyncContext().addListener(new TestAsyncListener());
		request.getAsyncContext().setTimeout(1000);
		request.getAsyncContext().start(new TestRunnable(request.getAsyncContext(), "jim"));
		request.getAsyncContext().start(new TestRunnable(request.getAsyncContext(), "foo"));
		
		return null;
	}
	
	@RequestMapping(value = "/async2/dispatch")
	public View dispatch(final HttpServletRequest request, HttpServletResponse response) {
		request.startAsync();
		request.getAsyncContext().start(new Runnable(){

			@Override
			public void run() {
				try {
					Thread.sleep(5000);
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
				request.getAsyncContext().dispatch("/index.html");
				
			}});
		
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
