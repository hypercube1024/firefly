package test.component3;

import java.util.List;

public class PersonService {
	private Person person, person2;

	private List<Object> testList;

	public void setPerson(Person person) {
		this.person = person;
	}
	
	public void setPersion2(Person person2) {
		this.person2 = person2;
	}

	public void setTestList(List<Object> testList) {
		this.testList = testList;
	}

	public List<Object> getTestList() {
		return testList;
	}

	public Person getPerson2() {
		return person2;
	}

	public void setPerson2(Person person2) {
		this.person2 = person2;
	}

	public Person getPerson() {
		return person;
	}
}
