package com.firefly.mvc.web.servlet;

import com.firefly.mvc.web.AnnotationWebContext;
import com.firefly.mvc.web.DispatcherController;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

public class DispatcherServlet extends HttpServlet {

	private static final long serialVersionUID = -3638120056786910984L;
	private static Logger log = LoggerFactory.getLogger("firefly-system");
	private static final String INIT_PARAM = "contextConfigLocation";
	private DispatcherController dispatcherController;

	@Override
	public void doGet(HttpServletRequest request, HttpServletResponse response)
			throws ServletException, IOException {
		processDispatcher(request, response);
	}

	@Override
	public void doPost(HttpServletRequest request, HttpServletResponse response)
			throws ServletException, IOException {
		processDispatcher(request, response);
	}

	@Override
	public void doDelete(HttpServletRequest request,
			HttpServletResponse response) throws ServletException, IOException {
		processDispatcher(request, response);

	}

	@Override
	public void doPut(HttpServletRequest request, HttpServletResponse response)
			throws ServletException, IOException {
		processDispatcher(request, response);
	}

	protected void processDispatcher(HttpServletRequest request, HttpServletResponse response) {
		dispatcherController.dispatch(request, response);
	}

	@Override
	public void init() {
		String initParam = this.getInitParameter(INIT_PARAM);
		log.info("initParam [{}]", initParam);
		long start = System.currentTimeMillis();
		dispatcherController = new HttpServletDispatcherController(new AnnotationWebContext(initParam, getServletContext()));
		long end = System.currentTimeMillis();
		log.info("firefly startup in {} ms", (end - start));
	}

}
