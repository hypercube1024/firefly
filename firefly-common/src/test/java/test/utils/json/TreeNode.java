package test.utils.json;

import com.firefly.utils.json.Json;

import java.util.LinkedList;
import java.util.List;

public class TreeNode {
    private transient TreeNode parent;
    private List<TreeNode> children;
    private Integer id;
    private String name;

    public TreeNode getParent() {
        return parent;
    }

    public void setParent(TreeNode parent) {
        this.parent = parent;
    }

    public List<TreeNode> getChildren() {
        return children;
    }

    public void setChildren(List<TreeNode> children) {
        this.children = children;
    }

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public static void main(String[] args) {
        TreeNode root = new TreeNode();
        root.setId(0);
        root.setName("root");

        List<TreeNode> children = new LinkedList<TreeNode>();
        for (int i = 1; i < 10; i++) {
            TreeNode node = new TreeNode();
            node.setId(i);
            node.setName("children_" + i);
            node.setParent(root);
            List<TreeNode> children2 = new LinkedList<TreeNode>();
            for (int j = 11; j < 22; j++) {
                TreeNode node2 = new TreeNode();
                node2.setId(j);
                node2.setName("children_" + j);
                node2.setParent(node);
                node2.setChildren(new LinkedList<TreeNode>());
                children2.add(node2);
            }
            node.setChildren(children2);
            children.add(node);
        }

        root.setChildren(children);

        System.out.println(Json.toJson(root));
    }

}
