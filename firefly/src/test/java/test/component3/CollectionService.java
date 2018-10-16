package test.component3;

import java.util.LinkedList;
import java.util.Set;

public class CollectionService extends ArrayService {
    private LinkedList<Object> list;
    private Set<Integer> set;

    public LinkedList<Object> getList() {
        return list;
    }

    public void setList(LinkedList<Object> list) {
        this.list = list;
    }

    public Set<Integer> getSet() {
        return set;
    }

    public void setSet(Set<Integer> set) {
        this.set = set;
    }

}
