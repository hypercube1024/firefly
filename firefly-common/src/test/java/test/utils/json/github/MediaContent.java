package test.utils.json.github;

import java.util.Arrays;
import java.util.List;

import com.firefly.utils.json.Json;

public class MediaContent {

    private List<Image> images;

    private Media media;

    public static MediaContent createRecord() {
        MediaContent record = new MediaContent();
        Media media = new Media();
        media.setUri("http://javaone.com/keynote.mpg");
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
        image1.setUri("http://javaone.com/keynote_large.jpg");
        image1.setTitle("Javaone Keynote");
        image1.setWidth(1024);
        image1.setHeight(768);
        image1.setSize(Size.LARGE);

        Image image2 = new Image();
        image2.setUri("http://javaone.com/keynote_small.jpg");
        image2.setTitle("Javaone Keynote");
        image2.setWidth(320);
        image2.setHeight(240);
        image2.setSize(Size.SMALL);
        record.setImages(Arrays.asList(image1, image2));
        return record;
    }

    public static void main(String[] args) throws Throwable {
//    	System.out.println(list.getClass().getGenericInterfaces());
    	
    	
    	MediaContent record = createRecord();
        String json = Json.toJson(record);
        System.out.println(json);
        System.out.println("===========================================");
        MediaContent r = Json.toObject(json, MediaContent.class);
        System.out.println(r.getMedia().getPlayer());
        System.out.println(r.getImages().size());
        System.out.println(r.getMedia().getCopyright());
        System.out.println(r.getMedia().getFormat());
        System.out.println(r.getImages().get(1).getWidth());
        System.out.println(r.getImages().get(1).getSize());
        System.out.println(r.getImages().get(0).getUri());
//        
//        Image image1 = new Image();
//        image1.setUri("http://javaone.com/keynote_large.jpg");
//        image1.setTitle("Javaone Keynote");
//        image1.setWidth(1024);
//        image1.setHeight(768);
//        image1.setSize(Size.LARGE);
//        System.out.println(Json.toJson(image1));
//        Performance.run(20000, record);
    }

    public List<Image> getImages() {
        return images;
    }

    public void setImages(List<Image> images) {
        this.images = images;
    }

    public Media getMedia() {
        return media;
    }

    public void setMedia(Media media) {
        this.media = media;
    }
}
