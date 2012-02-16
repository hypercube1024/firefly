package test;

import java.util.Map;

public class Foo {
	private int[] numbers = { 3, 4, 5, 6 };
	private Integer[] bags = {1, 2, 3, 4, 5};
	private Bar bar;
	private Map<String, Bar> map;

	public Integer[] getBags() {
		return bags;
	}

	public void setBags(Integer[] bags) {
		this.bags = bags;
	}

	public int[] getNumbers() {
		return numbers;
	}

	public void setNumbers(int[] numbers) {
		this.numbers = numbers;
	}

	public Bar getBar() {
		return bar;
	}

	public void setBar(Bar bar) {
		this.bar = bar;
	}

	public Map<String, Bar> getMap() {
		return map;
	}

	public void setMap(Map<String, Bar> map) {
		this.map = map;
	}

}
