package test.utils.json;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import com.firefly.utils.json.Json;

public class NodeDemo {

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		Map<String, Object> map = new HashMap<String, Object>();
		Node node = new Node();
		Node node2 = new Node();
		
		node.setNode(node2);
		node.setId(33);
		node.setTimestamp(new Date());
		node.setSex('e');
		node.setText("dfs\t");
		int[] rig = new int[]{1,2,3};
		node.setRig(rig);
		node.setRbool(new boolean[]{true, true, false});
		node.setRlong(new Long[]{});
		
		map.put("hello", "world");
		map.put("node2", node2);
		node.setMap(map);
		
		node2.setNode(node);
		node2.setId(13);
		node2.setSex('f');
		node2.setFlag(true);
		node2.setText("\n\"\b");
		node2.setRlong(new Long[]{33L, 44L, 55L});
		
		System.out.println(Json.toJson(node));
//		System.out.println(Json.toJson(node2));
//		StringWriter writer = new StringWriter();
//		Json.toJson(node, writer);
//		System.out.println(writer.toString());

	}

}
