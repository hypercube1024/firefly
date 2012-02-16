package test.controller;

public class Book {
	private String text, title;
	private Integer id;
	private Double price;
	private Boolean sell;

	public String getTitle() {
		return title;
	}

	public void setTitle(String title) {
		this.title = title;
	}

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

	public Double getPrice() {
		return price;
	}

	public void setPrice(Double price) {
		this.price = price;
	}

}
