package test.interceptor;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import com.firefly.annotation.Interceptor;
import com.firefly.utils.log.Log;
import com.firefly.utils.log.LogFactory;

@Interceptor(uri = "/*/view*", order = 2)
public class FoodInterceptor3 {
	private static Log log = LogFactory.getInstance().getLog("firefly-system");

	public void before(HttpServletRequest request, HttpServletResponse response) {
		request.setAttribute("into", "2");
		log.info("before 2 [{}]", request.getRequestURI());
	}

	public void after(HttpServletRequest request, HttpServletResponse response) {
		log.info("after 2 [{}]", request.getRequestURI());
		
	}
}
