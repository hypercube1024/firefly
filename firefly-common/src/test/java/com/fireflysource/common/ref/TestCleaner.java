package com.fireflysource.common.ref;

import com.fireflysource.common.lifecycle.ShutdownTasks;
import org.junit.jupiter.api.Test;

public class TestCleaner {

    private Cleaner cleaner = Cleaner.create();

    public static class AutoCloseableResource {
        private final AutoCloseable closeable;

        public AutoCloseableResource(Cleaner cleaner, AutoCloseable closeable) {
            this.closeable = closeable;
            cleaner.register(this, () -> {
                try {
                    closeable.close();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            });
        }
    }

    public static class Foo implements AutoCloseable {

        private final String text;

        public Foo(String text) {
            this.text = text;
        }

        public String getText() {
            return text;
        }

        @Override
        public void close() throws Exception {
            System.out.println("Clean resource: " + text);
        }
    }

    @Test
    void test() throws InterruptedException {
        ShutdownTasks.register(() -> System.out.println("exit process"));

        Foo foo = new Foo("Foo fu");
        AutoCloseableResource resource = new AutoCloseableResource(cleaner, foo);
        System.out.println(foo.getText());
        resource = null;

        System.gc();
        Thread.sleep(3000);
    }
}
