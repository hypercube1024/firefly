package com.firefly.wechat.utils;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.dataformat.xml.XmlMapper;
import com.firefly.wechat.model.message.CommonMessage;
import com.firefly.wechat.model.message.TextMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author Pengtao Qiu
 */
abstract public class MessageUtils {

    private static Logger log = LoggerFactory.getLogger("firefly-system");

    private static XmlMapper mapper = new XmlMapper();
    static {
        mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES,false);
    }

    public static CommonMessage parseCommonMessage(String xml) {
        try {
            return mapper.readValue(xml, CommonMessage.class);
        } catch (Exception e) {
            log.error("parse text message exception", e);
            return null;
        }
    }

    public static TextMessage parseTextMessage(String xml) {
        try {
            return mapper.readValue(xml, TextMessage.class);
        } catch (Exception e) {
            log.error("parse text message exception", e);
            return null;
        }
    }

    public static String textMessageToXml(TextMessage textMessage) {
        try {
            return mapper.writeValueAsString(textMessage);
        } catch (Exception e) {
            log.error("generate text message xml exception", e);
            return null;
        }
    }
}
