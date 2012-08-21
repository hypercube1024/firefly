package test.utils.json;

import java.util.List;

import com.firefly.utils.json.annotation.Transient;

public class Book {
	@Transient
	private String text, title;
	private double price;
	private Boolean sell;
	public String author;
	private Integer id;
	public int publishingId;
	public transient Object extInfo;
	public List<SimpleObj> simpleObjs;

	public String getTitle() {
		return title;
	}

	public void setTitle(String title) {
		this.title = title;
	}

	@Transient
	public Boolean getSell() {
		return sell;
	}

	public void setSell(Boolean sell) {
		this.sell = sell;
	}

	public String getText() {
		return text;
	}

	public void setText(String text) {
		this.text = text;
	}

	public Integer getId() {
		return id;
	}

	public void setId(Integer id) {
		this.id = id;
	}

	public double getPrice() {
		return price;
	}

	public void setPrice(double price) {
		this.price = price;
	}

}
