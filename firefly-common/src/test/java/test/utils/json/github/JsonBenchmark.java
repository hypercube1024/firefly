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
//        String url = "http://javaone.com/keynote.mpg";
        String url = "testURL";

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
        benchmark(times / 10);
        System.out.println("warm up end");
        System.out.println("=======================");
        System.out.println();
        System.out.println();

        System.out.println("benchmark start");
        benchmark(times);
        System.out.println("benchmark end");
        System.out.println("=======================");
    }

    public static void benchmark(final int times) {
        long fireflyTotal = 0;
        fireflyTotal += fireflyJsonSerializerTest(times);
        fireflyTotal += fireflyJsonParserTest(times);
        long fireflyMapParser = fireflyJsonParserWithoutObjectbindTest(times);

        long fastjsonTotal = 0;
        fastjsonTotal += fastjsonSerializerTest(times);
        fastjsonTotal += fastjsonParserTest(times);
        long fastjsonMapParser = fastjsonParserWithoutObjectbindTest(times);

        long jacksonTotal = 0;
        jacksonTotal += jacksonJsonSerializerTest(times);
        jacksonTotal += jacksonJsonParserTest(times);

        System.out.println();
        System.out.println("total time: ");
        System.out.println("firefly: " + fireflyTotal + "ms");
        System.out.println("fastjson: " + fastjsonTotal + "ms");
        System.out.println("jackson: " + jacksonTotal + "ms");
        System.out.println();
        System.out.println("map parser time:");
        System.out.println("firefly: " + fireflyMapParser + "ms");
        System.out.println("fastjson: " + fastjsonMapParser + "ms");
    }

    public static void printResult(String name, String json, long total, int times) {
        double qps = times / (total / 1000.00);
        System.out.println();
        System.out.println(name + " : ");
        System.out.println(json);
        System.out.println("time: " + total + "ms, qps: " + qps + "op/s");
        System.out.println("====================================");
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
        printResult(name + " serializer", json, total, times);
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
        printResult(name + " parser", json, total, times);
        return total;
    }

    public static long jacksonJsonSerializerTest(final int times) {
        return serializerTest(times, "jackson", record -> {
            try {
                return mapper.writeValueAsString(record);
            } catch (JsonProcessingException e) {
                e.printStackTrace();
                return null;
            }
        });
    }

    public static long jacksonJsonParserTest(final int times) {
        return parserTest(times, "jackson", json -> {
            try {
                return mapper.readValue(json, MediaContent.class);
            } catch (IOException e) {
                e.printStackTrace();
                return null;
            }
        });
    }

    public static long fireflyJsonSerializerTest(final int times) {
        return serializerTest(times, "firefly-json", Json::toJson);
    }

    public static long fireflyJsonParserTest(final int times) {
        return parserTest(times, "firefly-json", json -> Json.toObject(json, MediaContent.class));
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
        long total = end - start;
        printResult("firefly map parser", json, total, times);
        return total;
    }

    public static long fastjsonSerializerTest(final int times) {
        return serializerTest(times, "fastjson", JSON::toJSONString);
    }

    public static long fastjsonParserTest(final int times) {
        return parserTest(times, "fastjson", json -> JSON.parseObject(json, MediaContent.class));
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
        long total = end - start;
        printResult("fastjson map parser", json, total, times);
        return total;
    }

}
