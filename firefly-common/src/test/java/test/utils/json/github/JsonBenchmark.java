package test.utils.json.github;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.firefly.utils.function.Func1;
import com.firefly.utils.json.Json;
import com.firefly.utils.json.JsonObject;

import java.io.IOException;
import java.util.Arrays;

public class JsonBenchmark {

    public static final ObjectMapper mapper = new ObjectMapper();

    public static MediaContent createRecord() {
        String url = "http://javaone.com/keynote.mpg";
//    	String url = "testURL";

        MediaContent record = new MediaContent();
        Media media = new Media();
        media.setUri(url);
        media.setTitle("Javaone Keynote");
        media.setWidth(640);
        media.setHeight(480);
        media.setFormat("video/mpg4");
        media.setDuration(18000000);
        media.setSize(58982400);
        media.setBitrate(262144);
        media.setPersons(Arrays.asList("Bill Gates", "Steve Jobs"));
        media.setPlayer(Player.JAVA);
        media.setCopyright(null);

        record.setMedia(media);

        Image image1 = new Image();
        image1.setUri(url);
        image1.setTitle("Javaone Keynote");
        image1.setWidth(1024);
        image1.setHeight(768);
        image1.setSize(Size.LARGE);

        Image image2 = new Image();
        image2.setUri(url);
        image2.setTitle("Javaone Keynote");
        image2.setWidth(320);
        image2.setHeight(240);
        image2.setSize(Size.SMALL);
        record.setImages(Arrays.asList(image1, image2));
        return record;
    }

    public static void main(String[] args) throws Throwable {
        final int times = 1000 * 1000 * 2;
        System.out.println("warm up start");
        warmUp(times / 10);
        System.out.println("warm up end");
        System.out.println("=======================");
        System.out.println();
        System.out.println();

        fastjsonParserTest(times);
        fireflyJsonParserTest(times);
        jacksonJsonParserTest(times);

        System.out.println();
        System.out.println();
        fastjsonSerializerTest(times);
        fireflyJsonSerializerTest(times);
        jacksonJsonSerializerTest(times);

    }

    public static void warmUp(final int times) {
        fireflyJsonSerializerTest(times);
        fireflyJsonParserTest(times);
//        fireflyJsonParserWithoutObjectbindTest(times);

        fastjsonSerializerTest(times);
        fastjsonSerializerTest(times);
//        fastjsonParserWithoutObjectbindTest(times);

        jacksonJsonSerializerTest(times);
        jacksonJsonParserTest(times);
    }

    public static long serializerTest(int times, String name, Func1<MediaContent, String> func1) {
        MediaContent record = createRecord();
        String json = null;
        long start = System.currentTimeMillis();
        for (int i = 0; i < times; i++) {
            json = func1.call(record);
        }
        long end = System.currentTimeMillis();
        long total = end - start;
        double qps = times / (total / 1000.00);
        System.out.println(name + " serializer: ");
        System.out.println(json);
        System.out.println("time: " + total + "ms, qps: " + qps + "op/s");
        System.out.println("====================================");
        return total;
    }

    public static long parserTest(int times, String name, Func1<String, MediaContent> func1) {
        MediaContent record = createRecord();
        MediaContent r = null;
        String json = Json.toJson(record);
        long start = System.currentTimeMillis();
        for (int i = 0; i < times; i++) {
            r = func1.call(json);
        }
        long end = System.currentTimeMillis();
        long total = end - start;
        double qps = times / (total / 1000.00);
        System.out.println(name + " parser: ");
        System.out.println(r);
        System.out.println("time: " + total + "ms, qps: " + qps + "op/s");
        System.out.println("====================================");
        return total;
    }

    public static void jacksonJsonSerializerTest(final int times) {
        serializerTest(times, "jackson", record -> {
            try {
                return mapper.writeValueAsString(record);
            } catch (JsonProcessingException e) {
                e.printStackTrace();
                return null;
            }
        });
    }

    public static void jacksonJsonParserTest(final int times) {
        parserTest(times, "jackson", json -> {
            try {
                return mapper.readValue(json, MediaContent.class);
            } catch (IOException e) {
                e.printStackTrace();
                return null;
            }
        });
    }

    public static void fireflyJsonSerializerTest(final int times) {
        serializerTest(times, "firefly-json", Json::toJson);
    }

    public static void fireflyJsonParserTest(final int times) {
        parserTest(times, "firefly-json", json -> Json.toObject(json, MediaContent.class));
    }

    public static long fireflyJsonParserWithoutObjectbindTest(final int times) {
        MediaContent record = createRecord();
        JsonObject r = null;
        String json = Json.toJson(record);
        long start = System.currentTimeMillis();
        for (int i = 0; i < times; i++) {
            r = Json.toJsonObject(json);
        }
        long end = System.currentTimeMillis();
        System.out.println("firefly json parser without object bind: " + (end - start));
        System.out.println(r);
        return end - start;
    }

    public static void fastjsonSerializerTest(final int times) {
        serializerTest(times, "fastjson", JSON::toJSONString);
    }

    public static void fastjsonParserTest(final int times) {
        parserTest(times, "fastjson", json -> JSON.parseObject(json, MediaContent.class));
    }

    public static long fastjsonParserWithoutObjectbindTest(final int times) {
        MediaContent record = createRecord();
        JSONObject r = null;
        String json = JSON.toJSONString(record);
        long start = System.currentTimeMillis();
        for (int i = 0; i < times; i++) {
            r = JSON.parseObject(json);
        }
        long end = System.currentTimeMillis();
        System.out.println("fastjson parser without object bind: " + (end - start));
        System.out.println(r);
        return end - start;
    }

}
