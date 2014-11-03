import java.io.OutputStream;
import com.firefly.template.support.ObjectNavigator;
import com.firefly.template.Model;
import com.firefly.template.view.AbstractView;
import com.firefly.template.TemplateFactory;
import com.firefly.template.FunctionRegistry;

public class _error_e404 extends AbstractView {

	public _error_e404(TemplateFactory templateFactory){this.templateFactory = templateFactory;}

	@Override
	protected void main(Model model, OutputStream out) throws Throwable {
		ObjectNavigator objNav = ObjectNavigator.getInstance();
		out.write(_TEXT_0);
		out.write(objNav.getValue(model ,"#systemErrorMessage").getBytes("UTF-8"));
		out.write(_TEXT_1);
	}

	private final byte[] _TEXT_0 = str2Byte("<!DOCTYPE html>\n<html>\n<head>\n<title>404错误</title>\n<style type=\"text/css\">\n.title{\noverflow: hidden;\ntext-align: center;\n}\n.content {\nwidth: 50em;\noverflow: hidden;\nmargin: 0 auto;\n}\n</style>\n</head>\n<body>\n<h1 class=\"title\">页面不见了。。 </h1>\n<div class=\"content\">\n", "UTF-8");
	private final byte[] _TEXT_1 = str2Byte("，页面在哪里啊，页面在哪里。页面在哪无尽的深渊里。\n</div>\n</body>\n</html>\n", "UTF-8");
}