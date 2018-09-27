package test.utils.json;

import com.firefly.utils.json.Json;

public class JsonDemo {

    /**
     * @param args
     */
    public static void main(String[] args) {
        Group group = new Group();
        group.setId(0L);
        group.setName("admin");
        group.setTypes(new String[]{"typeA", "typeA", "typeA", "typeA",
                "typeA", "typeA", "typeA", "typeA", "typeA", "typeA"});

        User guestUser = new User();
        guestUser.setId(2L);
        guestUser.setName("guest");

        User rootUser = new User();
        rootUser.setId(3L);
        rootUser.setName("root");

        group.getUsers().add(guestUser);
        group.getUsers().add(rootUser);

        String jsonString = Json.toJson(group);
        long start = System.currentTimeMillis();
        for (int i = 0; i < 1000 * 1000; i++) {
            jsonString = Json.toJson(group);
        }
        long end = System.currentTimeMillis();
        System.out.println(end - start);
        System.out.println(jsonString);
        // System.out.println(Json.toJson(group));

    }

}
