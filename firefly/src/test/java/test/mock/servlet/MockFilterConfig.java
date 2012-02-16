package test.mock.servlet;

import javax.servlet.FilterConfig;

public class MockFilterConfig extends MockServletObject implements FilterConfig {

	private String filterName;

	public String getFilterName() {
		return filterName;
	}

	public void setFilterName(String filterName) {
		this.filterName = filterName;
	}
}
