package test;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.util.Arrays;

import com.firefly.template.Model;
import com.firefly.template.TemplateFactory;
import com.firefly.template.View;

public class CharsetDemo {

	/**
	 * @param args
	 */
	public static void main(String[] args) throws Throwable {
		String str = "测试一下页面";
		System.out.println(Arrays.toString(str.getBytes("UTF-8")));
		TemplateFactory t = new TemplateFactory(new File(CharsetDemo.class.getResource("/page").toURI())).init();
		View view = t.getView("/index2.html");
		ByteArrayOutputStream out = new ByteArrayOutputStream();
		Model model = new ModelMock();
		view.render(model, out);
		out.close();
		System.out.println(out.toString());
	}

}
