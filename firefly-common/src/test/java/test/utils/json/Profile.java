package test.utils.json;

import com.firefly.utils.json.Json;

import java.util.Map;

public class Profile {
    private int readbookcount; //已看过的图书数量
    private Map<String, Integer> readbooktype; //已看过的图书类型 包括"原创图书","出版图书","杂志"三个子类
    private int bookcollect; //用户收藏的图书数量
    private int notecount; //用户做的笔记是多少条
    private int noteshare; //分享笔记
    private int bookshare; //分享图书
    private int screenshotshare; //分享截图的数量
    private int totalreadtime;  //总共合计阅读多长时间
    private int[] timeintervalreadtime; //每个时段阅读的时间分布

    public int getReadbookcount() {
        return readbookcount;
    }

    public void setReadbookcount(int readbookcount) {
        this.readbookcount = readbookcount;
    }

    public int getBookcollect() {
        return bookcollect;
    }

    public void setBookcollect(int bookcollect) {
        this.bookcollect = bookcollect;
    }

    public int getNotecount() {
        return notecount;
    }

    public void setNotecount(int notecount) {
        this.notecount = notecount;
    }

    public int getNoteshare() {
        return noteshare;
    }

    public void setNoteshare(int noteshare) {
        this.noteshare = noteshare;
    }

    public int getBookshare() {
        return bookshare;
    }

    public void setBookshare(int bookshare) {
        this.bookshare = bookshare;
    }

    public int getScreenshotshare() {
        return screenshotshare;
    }

    public void setScreenshotshare(int screenshotshare) {
        this.screenshotshare = screenshotshare;
    }

    public int getTotalreadtime() {
        return totalreadtime;
    }

    public void setTotalreadtime(int totalreadtime) {
        this.totalreadtime = totalreadtime;
    }

    public int[] getTimeintervalreadtime() {
        return timeintervalreadtime;
    }

    public void setTimeintervalreadtime(int[] timeintervalreadtime) {
        this.timeintervalreadtime = timeintervalreadtime;
    }

    public Map<String, Integer> getReadbooktype() {
        return readbooktype;
    }

    public void setReadbooktype(Map<String, Integer> readbooktype) {
        this.readbooktype = readbooktype;
    }

    public static void main(String[] args) {
//		String json = "{\"totalreadtime\":5,\"notecount\":27,\"timeintervalreadtime\":[0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,1,4,0,0,0,0,0,0,0],\"bookcollect\":0,\"screenshotshare\":0,\"readbooktype\":{\"测试\":1},\"bookshare\":0,\"readbookcount\":0,\"noteshare\":0}";
        String json = "{\"totalreadtime\":5,\"notecount\":27,\"timeintervalreadtime\":[0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,1,4,0,0,0,0,0,0,0],\"bookcollect\":0,\"screenshotshare\":0,\"readbooktype\":null,\"bookshare\":0,\"readbookcount\":0,\"noteshare\":0}";
        Profile p = Json.toObject(json, Profile.class);
        System.out.println(p.getTotalreadtime());
        System.out.println(p.getTimeintervalreadtime().length);
//		System.out.println(p.getReadbooktype().get("测试"));
    }

}
