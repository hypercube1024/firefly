package test;

import static org.hamcrest.Matchers.is;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.List;

import org.junit.Assert;
import org.junit.Test;

import com.firefly.template.Config;
import com.firefly.template.Function;
import com.firefly.template.FunctionRegistry;
import com.firefly.template.Model;
import com.firefly.template.TemplateFactory;
import com.firefly.template.View;

public class TestConfig {
	
	@Test
	public void test() {
		Config config = new Config();
		config.setViewPath("/page");
		Assert.assertThat(config.getCompiledPath(), is("/page/_compiled_view"));
		
		config.setViewPath("/page2/");
		Assert.assertThat(config.getCompiledPath(), is("/page2/_compiled_view"));
	}
	
	public static void main(String[] args) throws IOException, URISyntaxException {
		User user = new User();
		user.setName("Jim");
		user.setAge(25);
		
		Function function = new Function(){
			@Override
			public void render(Model model, OutputStream out, Object... obj) {
				Integer i = (Integer)obj[0];
				String str = (String)obj[1];
				String o = String.valueOf(obj[2]);
				
				try {
					out.write((i + "|" + str + "|" + o).getBytes("UTF-8"));
				} catch (IOException e) {
					e.printStackTrace();
				}
			}};
		FunctionRegistry.add("testFunction", function);
		
		Function function2 = new Function(){
			@Override
			public void render(Model model, OutputStream out, Object... obj) {
				try {
					out.write("testFunction2".getBytes("UTF-8"));
				} catch (IOException e) {
					e.printStackTrace();
				}
			}};
		FunctionRegistry.add("testFunction2", function2);
		
		// #if #elseif #else
		TemplateFactory t = new TemplateFactory(new File(TestConfig.class.getResource("/page").toURI())).init();
//		System.out.println(t.getConfig().getViewPath());
//		System.out.println(t.getConfig().getCompiledPath());
		View view = t.getView("/testIf.html");
		ByteArrayOutputStream out = new ByteArrayOutputStream();
		Model model = new ModelMock();
		view.render(model, out);
		out.close();
		System.out.println(out.toString());
		System.out.println("=======================================");
		
		out = new ByteArrayOutputStream();
		model.put("user", user);
		model.put("login", true);
		view.render(model, out);
		out.close();
		System.out.println(out.toString());
		System.out.println("=======================================");
		
		// #for
		model = new ModelMock();
		out = new ByteArrayOutputStream();
		view = t.getView("/testFor.html");
		
		List<User> list = new ArrayList<User>();
		user = new User();
		user.setName("Tom");
		user.setAge(20);
		list.add(user);
		
		user = new User();
		user.setName("小明");
		user.setAge(13);
		list.add(user);
		
		user = new User();
		user.setName("小红");
		user.setAge(20);
		list.add(user);
		
		model.put("users", list);
		model.put("intArr", new int[]{1,2,3,4,5});
		view.render(model, out);
		out.close();
		System.out.println(out.toString());
		System.out.println("=======================================");
		
		// #switch #case #default
		model = new ModelMock();
		out = new ByteArrayOutputStream();
		view = t.getView("/testSwitch.html");
		model.put("stage", 2);
		view.render(model, out);
		out.close();
		System.out.println(out.toString());
		System.out.println("=======================================");
		
		// #set
		model = new ModelMock();
		out = new ByteArrayOutputStream();
		view = t.getView("/testSet.html");
		model.put("name", "迈克");
		view.render(model, out);
		out.close();
		System.out.println(out.toString());
		System.out.println("=======================================");
		
		// #include
		model = new ModelMock();
		out = new ByteArrayOutputStream();
		view = t.getView("/testInclude.html");
		view.render(model, out);
		out.close();
		System.out.println(out.toString());
		System.out.println("=======================================");
		
		// #function
		model = new ModelMock();
		model.put("teststr", "好长好长，好大好大，的一个东东！");
		out = new ByteArrayOutputStream();
		view = t.getView("/testFunction.html");
		view.render(model, out);
		out.close();
		System.out.println(out.toString());
//		FunctionRegistry.MAP.get("").render(model, out, obj)
	}
}
