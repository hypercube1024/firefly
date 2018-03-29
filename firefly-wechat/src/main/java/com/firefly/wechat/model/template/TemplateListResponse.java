package com.firefly.wechat.model.template;

import java.io.Serializable;
import java.util.List;

/**
 * @author Pengtao Qiu
 */
public class TemplateListResponse implements Serializable {

    private static final long serialVersionUID = 1L;

    private List<Template> template_list;

    public List<Template> getTemplate_list() {
        return template_list;
    }

    public void setTemplate_list(List<Template> template_list) {
        this.template_list = template_list;
    }

    @Override
    public String toString() {
        return "TemplateListResponse{" +
                "template_list=" + template_list +
                '}';
    }
}
