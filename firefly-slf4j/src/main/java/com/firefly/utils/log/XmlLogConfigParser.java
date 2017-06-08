package com.firefly.utils.log;

import com.firefly.utils.ConvertUtils;
import com.firefly.utils.dom.DefaultDom;
import com.firefly.utils.dom.Dom;
import com.firefly.utils.function.Action1;
import com.firefly.utils.log.file.FileLog;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import java.util.List;

public class XmlLogConfigParser extends AbstractLogConfigParser {

    @Override
    public boolean parse(Action1<FileLog> action) {
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
                c.setConsole(ConvertUtils.convert(dom.getTextValueByTagName(e, "enable-console"), DEFAULT_CONSOLE_ENABLED));
                c.setMaxFileSize(ConvertUtils.convert(dom.getTextValueByTagName(e, "max-file-size"), DEFAULT_MAX_FILE_SIZE));
                if (c.getMaxFileSize() < 1024 * 1024 * 10) {
                    System.err.println("the max log file less than 10MB, please set a larger file size");
                }
                c.setCharset(dom.getTextValueByTagName(e, "charset", DEFAULT_CHARSET.name()));
                c.setFormatter(dom.getTextValueByTagName(e, "formatter", DEFAULT_LOG_FORMATTER));
                action.call(createLog(c));
            }
        }
        return true;
    }

}
