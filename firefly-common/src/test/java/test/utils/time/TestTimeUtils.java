package test.utils.time;

import com.firefly.utils.time.TimeUtils;
import org.junit.Assert;
import org.junit.Test;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.Date;

import static com.firefly.utils.time.TimeUtils.*;
import static org.hamcrest.Matchers.is;

/**
 * @author Pengtao Qiu
 */
public class TestTimeUtils {

    @Test
    public void test() {
        Date start = TimeUtils.parseLocalDate("2017-05-01", DEFAULT_LOCAL_DATE);
        LocalDate localDateStart = LocalDate.parse("2017-05-01", DEFAULT_LOCAL_DATE);
        Assert.assertThat(start, is(TimeUtils.toDate(localDateStart)));
        Assert.assertThat(LocalDate.now().withYear(2017).withMonth(5).withDayOfMonth(1), is(localDateStart));

        start = TimeUtils.parseLocalDateTime("2017-05-01 00:00:00", DEFAULT_LOCAL_DATE_TIME);
        LocalDateTime localDateTime = LocalDateTime.parse("2017-05-01 00:00:00", DEFAULT_LOCAL_DATE_TIME);
        Assert.assertThat(start, is(TimeUtils.toDate(localDateTime)));
        Assert.assertThat(start, is(TimeUtils.toDate(localDateStart)));

        LocalDate localDateEnd = LocalDate.parse("2017-06-01", DEFAULT_LOCAL_DATE);
        long days = TimeUtils.between(ChronoUnit.DAYS, localDateStart, localDateEnd);
        Assert.assertThat(days, is(31L));

        long month = TimeUtils.between(ChronoUnit.MONTHS, localDateStart, localDateEnd);
        Assert.assertThat(month, is(1L));

        localDateEnd = LocalDate.parse("2019-05-01", DEFAULT_LOCAL_DATE);
        long year = TimeUtils.between(ChronoUnit.YEARS, localDateStart, localDateEnd);
        Assert.assertThat(year, is(2L));

        Date current = TimeUtils.parseLocalDateTime("2017-05-01 08:00:00", DEFAULT_LOCAL_DATE_TIME);
        System.out.println(TimeUtils.format(current, ZoneOffset.UTC, ISO_LOCAL_DATE_TIME));

        LocalDate yearMonth = TimeUtils.parseYearMonth("2017-03", DEFAULT_LOCAL_MONTH);
        Assert.assertThat(yearMonth.getMonthValue(), is(3));
        Assert.assertThat(yearMonth.getYear(), is(2017));
        Assert.assertThat(yearMonth.getDayOfMonth(), is(1));
    }
}
