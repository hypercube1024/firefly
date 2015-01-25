package test.utils.json;

public class SpecialPropertyObject {
	private String iPhone;
	private String iPad;
	private String iOS;
	private boolean iText;
	
	public String aPhone;
	public String aPad;
	public String aOS;
	public boolean aText;
	
	public void init() {
		iPhone = "iPhone";
		iPad = "iPad";
		iOS = "iOS";
		iText = true;
		
		aPhone = "aPhone";
		aPad = "aPad";
		aOS = "aOS";
		aText = true;
	}

	public String getiPhone() {
		return iPhone;
	}

	public void setiPhone(String iPhone) {
		this.iPhone = iPhone;
	}

	public String getiPad() {
		return iPad;
	}

	public void setiPad(String iPad) {
		this.iPad = iPad;
	}

	public String getiOS() {
		return iOS;
	}

	public void setiOS(String iOS) {
		this.iOS = iOS;
	}

	public boolean isiText() {
		return iText;
	}

	public void setiText(boolean iText) {
		this.iText = iText;
	}

	@Override
	public String toString() {
		return "SpecialPropertyObject [iPhone=" + iPhone + ", iPad=" + iPad
				+ ", iOS=" + iOS + ", iText=" + iText + ", aPhone=" + aPhone
				+ ", aPad=" + aPad + ", aOS=" + aOS + ", aText=" + aText + "]";
	}

}
