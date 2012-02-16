package test.component2.impl;

import test.component.AddService;
import test.component.FieldInject;
import test.component2.MethodInject2;

import com.firefly.annotation.Component;
import com.firefly.annotation.Inject;

@Component("methodInject2")
public class MethodInjectImpl2 implements MethodInject2 {

	@Inject
	private Integer num = 3;
	@Inject
	public AddService addService;
	protected FieldInject fieldInject;

	@Inject
	public void init(AddService addService, FieldInject fieldInject, String str) {
		this.addService = addService;
		this.fieldInject = fieldInject;
		//TODO 此处测试注入对象图是否完整
		fieldInject.add(3, 4);
	}

	@Override
	public int add(int x, int y) {
		return fieldInject.add(x, y);
	}

	public Integer getNum() {
		return num;
	}

}
