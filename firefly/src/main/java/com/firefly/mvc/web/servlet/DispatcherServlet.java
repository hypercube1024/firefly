package com.firefly.mvc.web.servlet;

import java.io.IOException;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.firefly.mvc.web.AnnotationWebContext;
import com.firefly.mvc.web.DispatcherController;
import com.firefly.utils.log.Log;
import com.firefly.utils.log.LogFactory;

/**
 * mvc前端控制器Servlet
 *
 * @author alvinqiu
 *
 */
public class DispatcherServlet extends HttpServlet {

	private static final long serialVersionUID = -3638120056786910984L;
	private static Log log = LogFactory.getInstance().getLog("firefly-system");
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

	protected void processDispatcher(HttpServletRequest request,
			HttpServletResponse response) {
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
