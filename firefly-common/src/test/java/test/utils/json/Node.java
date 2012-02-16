package test.utils.json;

import java.util.Date;
import java.util.Map;

import com.firefly.utils.json.annotation.CircularReferenceCheck;

@CircularReferenceCheck
public class Node {
	private int id;
	private String text;
	private boolean flag;
	private Node node;
	private Date timestamp;
	private char sex;
	private int[] rig;
	private boolean[] rbool;
	private Long[] rlong;
	private Map<String, Object> map;

	public Long[] getRlong() {
		return rlong;
	}

	public void setRlong(Long[] rlong) {
		this.rlong = rlong;
	}

	public boolean[] getRbool() {
		return rbool;
	}

	public void setRbool(boolean[] rbool) {
		this.rbool = rbool;
	}

	public Map<String, Object> getMap() {
		return map;
	}

	public void setMap(Map<String, Object> map) {
		this.map = map;
	}

	public int[] getRig() {
		return rig;
	}

	public void setRig(int[] rig) {
		this.rig = rig;
	}

	public char getSex() {
		return sex;
	}

	public void setSex(char sex) {
		this.sex = sex;
	}

	public Date getTimestamp() {
		return timestamp;
	}

	public void setTimestamp(Date timestamp) {
		this.timestamp = timestamp;
	}

	public int getId() {
		return id;
	}

	public void setId(int id) {
		this.id = id;
	}

	public String getText() {
		return text;
	}

	public void setText(String text) {
		this.text = text;
	}

	public Node getNode() {
		return node;
	}

	public void setNode(Node node) {
		this.node = node;
	}

	public boolean isFlag() {
		return flag;
	}

	public void setFlag(boolean flag) {
		this.flag = flag;
	}

}
