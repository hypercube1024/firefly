package com.firefly.mvc.web.view;

import com.firefly.mvc.web.View;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.PrintWriter;

public class TextView implements View {

    private static String ENCODING;
    private static String CONTENT_TYPE;
    private final String text;

    public static void setEncoding(String encoding) {
        if (ENCODING == null && encoding != null) {
            ENCODING = encoding;
            CONTENT_TYPE = "text/plain; charset=" + ENCODING;
        }
    }

    public TextView(String text) {
        this.text = text;
    }

    public String getText() {
        return text;
    }

    @Override
    public void render(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        response.setCharacterEncoding(ENCODING);
        response.setHeader("Content-Type", CONTENT_TYPE);
        try (PrintWriter writer = response.getWriter()) {
            writer.print(text);
        }
    }

}
