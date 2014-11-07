package com.fireflysource.demo.model;

public class GoodsInformation {

	private String title;
	private String introduction;
	private double price;
	private int stockNumber;
	private Integer status;

	public String getTitle() {
		return title;
	}

	public void setTitle(String title) {
		this.title = title;
	}

	public String getIntroduction() {
		return introduction;
	}

	public void setIntroduction(String introduction) {
		this.introduction = introduction;
	}

	public double getPrice() {
		return price;
	}

	public void setPrice(double price) {
		this.price = price;
	}

	public int getStockNumber() {
		return stockNumber;
	}

	public void setStockNumber(int stockNumber) {
		this.stockNumber = stockNumber;
	}

	public Integer getStatus() {
		return status;
	}

	public void setStatus(Integer status) {
		this.status = status;
	}

	@Override
	public String toString() {
		return "GoodsInformation [title=" + title + ", introduction="
				+ introduction + ", price=" + price + ", stockNumber="
				+ stockNumber + ", status=" + status + "]";
	}

}
