package test;

public class Bar {
	private long serialNumber;
	private String info;
	private double price;

	public long getSerialNumber() {
		return serialNumber;
	}

	public void setSerialNumber(long serialNumber) {
		this.serialNumber = serialNumber;
	}

	public String getInfo() {
		return info;
	}

	public void setInfo(String info) {
		this.info = info;
	}

	public double getPrice() {
		return price;
	}

	public void setPrice(double price) {
		this.price = price;
	}

	@Override
	public String toString() {
		return "Bar [serialNumber=" + serialNumber + ", info=" + info
				+ ", price=" + price + "]";
	}

}
