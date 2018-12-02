package com.fireflysource.log;

import com.fireflysource.log.internal.utils.xml.DefaultDom;
import com.fireflysource.log.internal.utils.xml.Dom;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import java.util.List;
import java.util.function.Consumer;

public class XmlLogConfigParser extends AbstractLogConfigParser {

    @Override
    public boolean parse(Consumer<FileLog> consumer) {
        Dom dom = new DefaultDom();
        Document doc = dom.getDocument(DEFAULT_XML_CONFIG_FILE_NAME);
        if (doc == null) {
            return false;
        }
        Element root = dom.getRoot(doc);
        List<Element> loggerList = dom.elements(root, "logger");
        if (loggerList == null || loggerList.isEmpty()) {
            return false;
        } else {
            for (Element e : loggerList) {
                Configuration c = new Configuration();
                c.setName(dom.getTextValueByTagName(e, "name", DEFAULT_LOG_NAME));
                c.setLevel(dom.getTextValueByTagName(e, "level", DEFAULT_LOG_LEVEL));
                c.setPath(dom.getTextValueByTagName(e, "path", DEFAULT_LOG_DIRECTORY.getAbsolutePath()));
                try {
                    c.setConsole(Boolean.parseBoolean(dom.getTextValueByTagName(e, "enable-console")));
                } catch (Exception ex) {
                    c.setConsole(DEFAULT_CONSOLE_ENABLED);
                }
                try {
                    c.setMaxFileSize(Long.parseLong(dom.getTextValueByTagName(e, "max-file-size")));
                } catch (Exception ex) {
                    c.setMaxFileSize(DEFAULT_MAX_FILE_SIZE);
                }
                if (c.getMaxFileSize() < 1024 * 1024 * 10) {
                    System.err.println("the max log file less than 10MB, please set a larger file size");
                }
                c.setCharset(dom.getTextValueByTagName(e, "charset", DEFAULT_CHARSET.name()));
                c.setFormatter(dom.getTextValueByTagName(e, "formatter", DEFAULT_LOG_FORMATTER));
                c.setLogNameFormatter(dom.getTextValueByTagName(e, "log-name-formatter", DEFAULT_LOG_NAME_FORMATTER));
                c.setLogFilter(dom.getTextValueByTagName(e, "log-filter", DEFAULT_LOG_FILTER));
                c.setMaxSplitTime(dom.getTextValueByTagName(e, "max-split-time", DEFAULT_MAX_SPLIT_TIME.getValue()));
                consumer.accept(createLog(c));
            }
        }
        return true;
    }

}
