package test.utils;

import org.junit.Assert;
import org.junit.Test;
import com.firefly.utils.VerifyUtils;

import static org.hamcrest.Matchers.*;

public class TestVerifyUtils {

    @Test
    public void testIsEmpty() {
        String str = "\r\n\t\t";
        Assert.assertThat(VerifyUtils.isEmpty(str), is(true));
        str = null;
        Assert.assertThat(VerifyUtils.isEmpty(str), is(true));
    }

    @Test
    public void testIsFloat() {
        Assert.assertThat(VerifyUtils.isFloat("3f"), is(true));
        Assert.assertThat(VerifyUtils.isFloat("3.3f"), is(true));
        Assert.assertThat(VerifyUtils.isFloat("-.33f"), is(true));
        Assert.assertThat(VerifyUtils.isFloat("-33.00f"), is(true));
        Assert.assertThat(VerifyUtils.isFloat("-33.00"), is(false));
        Assert.assertThat(VerifyUtils.isFloat("ddfffe33"), is(false));
    }

    @Test
    public void testIsInteger() {
        Assert.assertThat(VerifyUtils.isInteger("30"), is(true));
        Assert.assertThat(VerifyUtils.isInteger("-30"), is(true));
        Assert.assertThat(VerifyUtils.isInteger("122312123121"), is(false));
        Assert.assertThat(VerifyUtils.isInteger("1223121sss1"), is(false));
    }

    @Test
    public void testIsLong() {
        Assert.assertThat(VerifyUtils.isLong("30"), is(false));
        Assert.assertThat(VerifyUtils.isLong("-30"), is(false));
        Assert.assertThat(VerifyUtils.isLong("122312123121"), is(true));
        Assert.assertThat(VerifyUtils.isLong("-122312123121"), is(true));
        Assert.assertThat(VerifyUtils.isLong("-122l"), is(true));
        Assert.assertThat(VerifyUtils.isLong("122L"), is(true));
        Assert.assertThat(VerifyUtils.isLong("1223121sss1"), is(false));
    }

    @Test
    public void testIsNumeric() {
        Assert.assertThat(VerifyUtils.isNumeric("13422224343"), is(true));
        Assert.assertThat(VerifyUtils.isNumeric(""), is(false));
        Assert.assertThat(VerifyUtils.isNumeric("134"), is(true));
        Assert.assertThat(VerifyUtils.isNumeric("-13433"), is(true));
        Assert.assertThat(VerifyUtils.isNumeric("134dfdfsfdf"), is(false));
    }

    @Test
    public void testIsDouble() {
        Assert.assertThat(VerifyUtils.isDouble("-30.2222"), is(true));
        Assert.assertThat(VerifyUtils.isDouble("30.2222"), is(true));
        Assert.assertThat(VerifyUtils.isDouble("30"), is(false));
        Assert.assertThat(VerifyUtils.isDouble("30ss"), is(false));
        Assert.assertThat(VerifyUtils.isDouble(""), is(false));
        Assert.assertThat(VerifyUtils.isDouble(".33"), is(true));
        Assert.assertThat(VerifyUtils.isDouble("33."), is(true));
    }
}
