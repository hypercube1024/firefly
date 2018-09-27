package test.utils.json;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.firefly.utils.json.annotation.DateFormat;
import com.firefly.utils.json.annotation.DateFormatType;

public class DateFormatObject {

    private Date dateDefault;

    @DateFormat
    private Date dateFieldDefaultFormat;

    @DateFormat("yyyy/MM/dd HH:mm:ss")
    public Date dateFieldFormat1;

    @DateFormat(type = DateFormatType.TIMESTAMP)
    private Date dateFieldTimestamp;

    private Date dateMethodFormat;

    @DateFormat("yyyy-MM-dd")
    private Date[] dateArray;

    @DateFormat("yyyy-MM-dd")
    private List<Date> dateList;

    @DateFormat("yyyy-MM-dd")
    private Map<String, Date> dateMap;

    public String title;

    public void init(Calendar cal) {
        dateDefault = cal.getTime();
        dateFieldDefaultFormat = cal.getTime();
        dateFieldFormat1 = cal.getTime();
        dateFieldTimestamp = cal.getTime();
        dateMethodFormat = cal.getTime();

        dateArray = new Date[]{cal.getTime(), cal.getTime(), cal.getTime()};
        dateList = new ArrayList<Date>();
        dateList.add(cal.getTime());
        dateList.add(cal.getTime());

        dateMap = new HashMap<String, Date>();
        dateMap.put("mapdate1", cal.getTime());

        title = "testDateFormat";
    }

    public Date getDateDefault() {
        return dateDefault;
    }

    public void setDateDefault(Date dateDefault) {
        this.dateDefault = dateDefault;
    }

    public Date getDateFieldDefaultFormat() {
        return dateFieldDefaultFormat;
    }

    public void setDateFieldDefaultFormat(Date dateFieldDefaultFormat) {
        this.dateFieldDefaultFormat = dateFieldDefaultFormat;
    }

    public Date getDateFieldTimestamp() {
        return dateFieldTimestamp;
    }

    public void setDateFieldTimestamp(Date dateFieldTimestamp) {
        this.dateFieldTimestamp = dateFieldTimestamp;
    }

    @DateFormat("yyyy/MM/dd")
    public Date getDateMethodFormat() {
        return dateMethodFormat;
    }

    @DateFormat("yyyy/MM/dd")
    public void setDateMethodFormat(Date dateMethodFormat) {
        this.dateMethodFormat = dateMethodFormat;
    }

    public Date[] getDateArray() {
        return dateArray;
    }

    public void setDateArray(Date[] dateArray) {
        this.dateArray = dateArray;
    }

    public List<Date> getDateList() {
        return dateList;
    }

    public void setDateList(List<Date> dateList) {
        this.dateList = dateList;
    }

    public Map<String, Date> getDateMap() {
        return dateMap;
    }

    public void setDateMap(Map<String, Date> dateMap) {
        this.dateMap = dateMap;
    }

    @Override
    public String toString() {
        return "DateFormatObject [dateDefault=" + dateDefault
                + ", dateFieldDefaultFormat=" + dateFieldDefaultFormat
                + ", dateFieldFormat1=" + dateFieldFormat1
                + ", dateFieldTimestamp=" + dateFieldTimestamp
                + ", dateMethodFormat=" + dateMethodFormat + ", dateArray="
                + Arrays.toString(dateArray) + ", dateList=" + dateList
                + ", dateMap=" + dateMap + "]";
    }

}
