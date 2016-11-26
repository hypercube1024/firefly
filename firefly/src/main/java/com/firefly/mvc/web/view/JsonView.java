package com.firefly.mvc.web.view;

import com.firefly.mvc.web.View;
import com.firefly.mvc.web.servlet.SystemHtmlPage;
import com.firefly.utils.json.Json;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.PrintWriter;

public class JsonView<T> implements View {

    private static String ENCODING;
    private static String CONTENT_TYPE;
    private final T object;

    public static void setEncoding(String encoding) {
        if (ENCODING == null && encoding != null) {
            ENCODING = encoding;
            CONTENT_TYPE = "application/json; charset=" + ENCODING;
        }
    }

    public JsonView(T object) {
        this.object = object;
    }

    public T getObject() {
        return object;
    }

    @Override
    public void render(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        if (object == null) {
            SystemHtmlPage.responseSystemPage(request, response,
                    ENCODING, HttpServletResponse.SC_NOT_FOUND,
                    request.getRequestURI() + " not found");
            return;
        }
        response.setCharacterEncoding(ENCODING);
        response.setHeader("Content-Type", CONTENT_TYPE);
        try (PrintWriter writer = response.getWriter()) {
            writer.print(Json.toJson(object));
        }
    }

}
