package test.utils.json;

import java.lang.reflect.Field;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;

import com.firefly.utils.json.Json;
import com.firefly.utils.json.JsonObject;

public class BookDemo {
    public static void main1(String[] args) throws Throwable {
        Field field = Book.class.getField("simpleObjs");
        Type type = field.getGenericType();
        System.out.println(field);
        System.out.println(type);
        System.out.println(type instanceof ParameterizedType);
    }

    public static void main2(String[] args) {
        Book book = new Book();
        book.setPrice(10.0);
        book.setId(331);
        book.setText("very good");
        book.setSell(true);
        book.setTitle("gook book");
        book.publishingId = 44342;
        book.author = "Gould";
        String str = Json.toJson(book);
        System.out.println(str);

        Book book2 = Json.toObject(str, Book.class);
        System.out.println(book2.getId());
        System.out.println(book2.getPrice());
        System.out.println(book2.publishingId);
        System.out.println(book2.author);

    }

    public static void main3(String[] args) {
        Book book = new Book();
        book.setPrice(10.0);
        book.setId(331);
        book.setText("very good");
        book.setSell(true);
        book.setTitle("gook book");
        System.out.println(Json.toJson(book));

        TestBook t = new TestBook();
        t.setObj(new Object());
        t.setBook(book);
        String t0 = Json.toJson(t);
        System.out.println("t0: " + t0);
        JsonObject o = Json.toJsonObject(t0);
        JsonObject obj1 = o.getJsonObject("obj");
        System.out.println(obj1 == null);
        System.out.println(obj1);

        t = new TestBook();
        t.setObj(book);
        t.setBook(book);
        System.out.println("t1: " + Json.toJson(t));
        t.setObj(new Object());
        System.out.println("t2: " + Json.toJson(t));
        t.setObj(book);
        System.out.println("t3: " + Json.toJson(t));

        TestBook2<Book> t2 = new TestBook2<>();
        t2.setObj(book);
        t2.setBook(null);
        System.out.println("t4: " + Json.toJson(t2));
        t2.setObj(book);
        System.out.println("t5: " + Json.toJson(t2));

        TestBook2<Object> t3 = new TestBook2<>();
        t3.setObj(book);
        t3.setBook(book);
        System.out.println(Json.toJson(t3));

//		Field field = TestBook2.class.getDeclaredField("obj");
//		System.out.println(field.getName());
//		System.out.println(field.getType());
    }

    public static void main(String[] args) {
        Book book = new Book();
        book.setPrice(10.0);
        book.setId(331);
        book.setText("very good");
        book.setSell(true);

        int charSize = 1024 * 4;
        StringBuilder builder = new StringBuilder(charSize);
        for (int i = 0; i < charSize; i++) {
            if (i % 13 == 0) {
                builder.append("\n");
            } else {
                builder.append("a");
            }
        }
        book.author = builder.toString();
        for (int i = 0; i < 10; i++) {
            System.out.println(Json.toJson(book));
        }

    }

    public static class TestBook {
        private Object obj;
        private Book book;

        public Object getObj() {
            return obj;
        }

        public void setObj(Object obj) {
            this.obj = obj;
        }

        public Book getBook() {
            return book;
        }

        public void setBook(Book book) {
            this.book = book;
        }
    }

    public static class TestBook2<T> {
        private T obj;
        private Book book;

        public T getObj() {
            return obj;
        }

        public void setObj(T obj) {
            this.obj = obj;
        }

        public Book getBook() {
            return book;
        }

        public void setBook(Book book) {
            this.book = book;
        }

    }
}
