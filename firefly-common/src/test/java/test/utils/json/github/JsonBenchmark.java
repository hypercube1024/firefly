package test.utils.json.github;

import java.util.Arrays;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.firefly.utils.json.Json;
import com.firefly.utils.json.JsonObject;

public class JsonBenchmark {
	public static MediaContent createRecord() {
//    	String url = "http://javaone.com/keynote.mpg";
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
        media.setPersons(Arrays.asList(new String[]{"Bill Gates", "Steve Jobs"}));
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
    
    public static void init() {
    	MediaContent record = createRecord();
        String json = Json.toJson(record);
        System.out.println(json);
        MediaContent r = Json.toObject(json, MediaContent.class);
        System.out.println(r);
 
        System.out.println();
        
        json = JSON.toJSONString(record);
        System.out.println(json);
        r = JSON.parseObject(json, MediaContent.class);
        System.out.println(r);
        
        System.out.println();
    }
    
    public static void main(String[] args) throws Throwable {
    	init();
    	final int times = 1000 * 1000 * 1;
    	long fastjsonTotal = 0L;
    	long fireflyTotal = 0L;
    	fastjsonTotal += fastjsonParserTest(times);
    	fireflyTotal += fireflyJsonParserTest(times);
    	
    	System.out.println("======================");
    	fastjsonTotal += fastjsonSerializerTest(times);
    	fireflyTotal += fireflyJsonSerializerTest(times);
    	
    	System.out.println("======================");
    	fastjsonTotal += fastjsonParserWithoutObjectbindTest(times);
    	fireflyTotal += fireflyJsonParserWithoutObjectbindTest(times);
    	
    	System.out.println("======================");
    	System.out.println("fastjson total time: " + fastjsonTotal + ", QPS: " + times * 3 / (fastjsonTotal / 1000.00));
    	System.out.println("firefly total time: " + fireflyTotal + ", QPS: " + times * 3 / (fireflyTotal / 1000.00));
    }
    
    public static long fireflyJsonSerializerTest(final int times) {
    	MediaContent record = createRecord();
    	String json = null;
    	long start = System.currentTimeMillis();
    	for (int i = 0; i < times; i++) {
    		json = Json.toJson(record);
		}
    	long end = System.currentTimeMillis();
    	System.out.println("firefly json serializer: " + (end - start));
    	System.out.println(json);
    	return end - start;
    }
    
    public static long fireflyJsonParserTest(final int times) {
    	MediaContent record = createRecord();
    	MediaContent r = null;
    	String json = Json.toJson(record);
    	long start = System.currentTimeMillis();
    	for (int i = 0; i < times; i++) {
    		r = Json.toObject(json, MediaContent.class);
		}
    	long end = System.currentTimeMillis();
    	System.out.println("firefly json parser: " + (end - start));
    	System.out.println(r);
    	return end - start;
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
    
    public static long fastjsonSerializerTest(final int times) {
    	MediaContent record = createRecord();
    	String json = null;
    	long start = System.currentTimeMillis();
    	for (int i = 0; i < times; i++) {
    		json = JSON.toJSONString(record);
		}
    	long end = System.currentTimeMillis();
    	System.out.println("fastjson serializer: " + (end - start));
    	System.out.println(json);
    	return end - start;
    }
    
    public static long fastjsonParserTest(final int times) {
    	MediaContent record = createRecord();
    	MediaContent r = null;
    	String json = JSON.toJSONString(record);
    	long start = System.currentTimeMillis();
    	for (int i = 0; i < times; i++) {
    		r = JSON.parseObject(json, MediaContent.class);
		}
    	long end = System.currentTimeMillis();
    	System.out.println("fastjson parser: " + (end - start));
    	System.out.println(r);
    	return end - start;
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
