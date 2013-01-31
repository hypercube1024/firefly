package test.utils.json;

import com.firefly.utils.json.Json;

public class Bar extends Foo {
	private String bar;
	private int boo;

	public String getBar() {
		return bar;
	}

	public void setBar(String bar) {
		this.bar = bar;
	}

	public int getBoo() {
		return boo;
	}

	public void setBoo(int boo) {
		this.boo = boo;
	}

	public static void main(String[] args) {
		Bar bar = new Bar();
		bar.setText("test");
		bar.setBar("bar");
		
		System.out.println(Json.toJson(bar));
		System.out.println(Json.toJson(bar));
		System.out.println(Json.toJson(bar));
	}
}
