package test.server;

import com.firefly.annotation.*;
import com.firefly.mvc.web.HttpMethod;
import com.firefly.mvc.web.View;
import com.firefly.mvc.web.view.JsonView;
import com.firefly.mvc.web.view.RedirectView;
import com.firefly.mvc.web.view.TemplateView;
import com.firefly.mvc.web.view.TextView;
import com.firefly.utils.io.FileUtils;
import com.firefly.utils.io.IO;
import com.firefly.utils.io.LineReaderHandler;
import test.server.model.Book;

import javax.servlet.ServletException;
import javax.servlet.http.*;
import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Map;

@Controller
public class IndexController {

    @RequestMapping(value = "/index_1")
    public View index_1(HttpServletRequest request, HttpServletResponse response) {
        response.setContentLength(2480);
        response.setHeader("Connection", "keep-alive");
        return new TemplateView("/index_1.html");
    }

    @RequestMapping(value = "/index")
    public View index(HttpServletRequest request, HttpServletResponse response) {
        System.out.println("into /index " + Thread.currentThread().getName());
        HttpSession session = request.getSession();
        request.setAttribute("hello", session.getAttribute("name"));
        response.addCookie(new Cookie("test", "cookie_value"));
        Cookie cookie = new Cookie("myname", "xiaoqiu");
        cookie.setMaxAge(5 * 60);
        response.addCookie(cookie);
        return new TemplateView("/index.html");
    }

    @RequestMapping(value = "/index-close")
    public View indexShort(HttpServletRequest request, HttpServletResponse response) {
        response.setHeader("Connection", "close");
        return new TemplateView("/index.html");
    }

    @RequestMapping(value = "/add", method = HttpMethod.POST)
    public View add(HttpServletRequest request, HttpServletResponse response) {
        System.out.println("into /add");
        return new TextView(request.getParameter("content"));
    }

    @RequestMapping(value = "/add2", method = HttpMethod.POST)
    public View add2(HttpServletRequest request, HttpServletResponse response) {
        System.out.println("into /add2");
        return new TextView("test add 2");
    }

    @RequestMapping(value = "/insertBook", method = HttpMethod.POST)
    public View addJson(@JsonBody Book book) {
        return new JsonView(book);
    }

    @RequestMapping(value = "/put", method = HttpMethod.PUT)
    public View put(HttpServletRequest request, HttpServletResponse response) {
        return new TextView("put -> " + request.getParameter("content"));
    }

    @RequestMapping(value = "/login")
    public View test(HttpServletRequest request, HttpServletResponse response) {
        HttpSession session = request.getSession();
        session.setMaxInactiveInterval(15);
        String name = (String) session.getAttribute("name");
        if (name == null) {
            System.out.println("name is null");
            name = "Qiu Pengtao";
            session.setAttribute("name", name);
        }
        request.setAttribute("name", name);
        return new TemplateView("/test.html");
    }

    @RequestMapping(value = "/exit")
    public View exit(HttpServletRequest request, HttpServletResponse response) {
        request.getSession().invalidate();
        request.setAttribute("name", "exit");
        return new TemplateView("/test.html");
    }

    @RequestMapping(value = "/index2")
    public View index2(HttpServletRequest request, HttpServletResponse response) throws IOException {
        response.sendRedirect("index");
        return null;
    }

    @RequestMapping(value = "/index3")
    public View index3(HttpServletRequest request, HttpServletResponse response) throws IOException {
        response.sendRedirect(request.getContextPath() + request.getServletPath() + "/index");
        return null;
    }

    @RequestMapping(value = "/testc")
    public View testOutContentLength(HttpServletRequest request, HttpServletResponse response) throws IOException {
        String msg = "<html><body>test Content-Length output</body></html>";
        response.setCharacterEncoding("UTF-8");
        response.setHeader("Content-Type", "text/html; charset=UTF-8");
        response.setHeader("Content-Length", String.valueOf(msg.getBytes("UTF-8").length));
        PrintWriter writer = response.getWriter();
        try {
            writer.print(msg);
        } finally {
            writer.close();
        }
        return null;
    }

    @RequestMapping(value = "/index4")
    public View index4(HttpServletRequest request, HttpServletResponse response) throws IOException {
        return new RedirectView("/index");
    }

    @RequestMapping(value = "/document/?/?")
    public View document(HttpServletRequest request, @PathVariable String[] args) {
        System.out.println(Arrays.toString(args));
        request.setAttribute("info", args);
        return new TemplateView("/index.html");
    }

    @RequestMapping(value = "/param")
    public View testParam(HttpServletRequest request) {
        Map<String, String[]> map = request.getParameterMap();
        return new TextView(Arrays.toString(map.get("b")));
    }

    @RequestMapping(value = "/big")
    public View testBigData(HttpServletRequest request) throws InterruptedException {
        final StringBuilder json = new StringBuilder();
        try {
            FileUtils.read(new File("/Users/qiupengtao/develop/jsontest.txt"), new LineReaderHandler() {

                @Override
                public void readline(String text, int num) {
                    json.append(text);

                }
            }, "UTF-8");
        } catch (IOException e) {
            e.printStackTrace();
        }
        String ret = json.toString();
        Thread.sleep(5000);
        return new TextView(ret);
    }

    @RequestMapping(value = "/error")
    public View testError(HttpServletRequest request) throws InterruptedException {
        System.out.println("test error");

        throw new RuntimeException("test error");
    }

    @MultipartSettings(maxFileSize = 50000L)
    @RequestMapping(value = "/upload", method = HttpMethod.POST)
    public View upload(HttpServletRequest request) throws IOException, ServletException {
        System.out.println(">>>>>>>>> upload start ");
        for (Part part : request.getParts()) {
            System.out.println("the part -> " + part);
            System.out.println(IO.toString(part.getInputStream(), StandardCharsets.UTF_8));
            if (part.getName().startsWith("content")) {
                part.write("test" + part.getSubmittedFileName());
            } else {
                part.write(part.getName() + ".txt");
            }
        }
        // throw new RuntimeException("upload error");
        return new TextView("upload ok!");
    }

    @RequestMapping(value = "/testTimeout")
    public View testTimeout() throws InterruptedException {
        Thread.sleep(8000L);
        return new TextView("timeout");
    }

}
