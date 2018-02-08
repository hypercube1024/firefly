package test.wechat.model;

import com.firefly.wechat.model.message.TextMessage;
import com.firefly.wechat.utils.MessageXmlUtils;
import org.junit.Assert;
import org.junit.Test;

import static org.hamcrest.Matchers.is;

/**
 * @author Pengtao Qiu
 */
public class TestTextMessage {

    @Test
    public void test() {
        String xml = "<xml><URL><![CDATA[http://t6kjdq.natappfree.cc]]></URL><ToUserName><![CDATA[Alvin]]></ToUserName><FromUserName><![CDATA[324324]]></FromUserName><CreateTime>55843535</CreateTime><MsgType><![CDATA[text]]></MsgType><Content><![CDATA[aaahello]]></Content><MsgId>12345</MsgId></xml>";

        TextMessage textMessage = MessageXmlUtils.parseXml(xml, TextMessage.class);
        System.out.println(textMessage);
        Assert.assertThat(textMessage.getToUserName(), is("Alvin"));
        Assert.assertThat(textMessage.getFromUserName(), is("324324"));

        System.out.println(MessageXmlUtils.toXml(textMessage));
    }
}
