package test.utils.json;

import java.util.List;
import java.util.Map;

public class MapObj {
    private Map<String, Integer> map;
    private Map<String, User[]> userMap;
    private Map<String, Book> bookMap;
    private Map<String, List<List<SimpleObj>>> map2;
    public Map<String, int[]> map3;

    public Map<String, Integer> getMap() {
        return map;
    }

    public void setMap(Map<String, Integer> map) {
        this.map = map;
    }

    public Map<String, User[]> getUserMap() {
        return userMap;
    }

    public void setUserMap(Map<String, User[]> userMap) {
        this.userMap = userMap;
    }

    public Map<String, Book> getBookMap() {
        return bookMap;
    }

    public void setBookMap(Map<String, Book> bookMap) {
        this.bookMap = bookMap;
    }

    public Map<String, List<List<SimpleObj>>> getMap2() {
        return map2;
    }

    public void setMap2(Map<String, List<List<SimpleObj>>> map2) {
        this.map2 = map2;
    }

}
