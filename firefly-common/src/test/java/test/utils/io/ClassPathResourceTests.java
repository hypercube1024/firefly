package test.utils.io;

import com.firefly.utils.io.ClassPathResource;
import org.junit.Test;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.instanceOf;
import static org.junit.Assert.*;

/**
 * Unit tests that serve as regression tests for the bugs described in SPR-6888
 * and SPR-9413.
 */
public class ClassPathResourceTests {

    private static final String PACKAGE_PATH = "test/utils/io";
    private static final String NONEXISTENT_RESOURCE_NAME = "nonexistent.xml";
    private static final String FQ_RESOURCE_PATH = PACKAGE_PATH + '/' + NONEXISTENT_RESOURCE_NAME;

    /**
     * Absolute path version of {@link #FQ_RESOURCE_PATH}.
     */
    private static final String FQ_RESOURCE_PATH_WITH_LEADING_SLASH = '/' + FQ_RESOURCE_PATH;

    private static final Pattern DESCRIPTION_PATTERN = Pattern.compile("^class path resource \\[(.+?)\\]$");


    private void assertDescriptionContainsExpectedPath(ClassPathResource resource, String expectedPath) {
        Matcher matcher = DESCRIPTION_PATTERN.matcher(resource.getDescription());
        assertTrue(matcher.matches());
        assertEquals(1, matcher.groupCount());
        String match = matcher.group(1);

        assertEquals(expectedPath, match);
    }

    private void assertExceptionContainsFullyQualifiedPath(ClassPathResource resource) {
        try {
            resource.getInputStream();
            fail("FileNotFoundException expected for resource: " + resource);
        } catch (IOException ex) {
            assertThat(ex, instanceOf(FileNotFoundException.class));
            assertThat(ex.getMessage(), containsString(FQ_RESOURCE_PATH));
        }
    }

    @Test
    public void stringConstructorRaisesExceptionWithFullyQualifiedPath() {
        assertExceptionContainsFullyQualifiedPath(new ClassPathResource(FQ_RESOURCE_PATH));
    }

    @Test
    public void classLiteralConstructorRaisesExceptionWithFullyQualifiedPath() {
        assertExceptionContainsFullyQualifiedPath(new ClassPathResource(NONEXISTENT_RESOURCE_NAME, this.getClass()));
    }

    @Test
    public void classLoaderConstructorRaisesExceptionWithFullyQualifiedPath() {
        assertExceptionContainsFullyQualifiedPath(new ClassPathResource(FQ_RESOURCE_PATH,
                this.getClass().getClassLoader()));
    }

    @Test
    public void getDescriptionWithStringConstructor() {
        assertDescriptionContainsExpectedPath(new ClassPathResource(FQ_RESOURCE_PATH), FQ_RESOURCE_PATH);
    }

    @Test
    public void getDescriptionWithStringConstructorAndLeadingSlash() {
        assertDescriptionContainsExpectedPath(new ClassPathResource(FQ_RESOURCE_PATH_WITH_LEADING_SLASH),
                FQ_RESOURCE_PATH);
    }

    @Test
    public void getDescriptionWithClassLiteralConstructor() {
        assertDescriptionContainsExpectedPath(new ClassPathResource(NONEXISTENT_RESOURCE_NAME, this.getClass()),
                FQ_RESOURCE_PATH);
    }

    @Test
    public void getDescriptionWithClassLiteralConstructorAndLeadingSlash() {
        assertDescriptionContainsExpectedPath(
                new ClassPathResource(FQ_RESOURCE_PATH_WITH_LEADING_SLASH, this.getClass()), FQ_RESOURCE_PATH);
    }

    @Test
    public void getDescriptionWithClassLoaderConstructor() {
        assertDescriptionContainsExpectedPath(
                new ClassPathResource(FQ_RESOURCE_PATH, this.getClass().getClassLoader()), FQ_RESOURCE_PATH);
    }

    @Test
    public void getDescriptionWithClassLoaderConstructorAndLeadingSlash() {
        assertDescriptionContainsExpectedPath(new ClassPathResource(FQ_RESOURCE_PATH_WITH_LEADING_SLASH,
                this.getClass().getClassLoader()), FQ_RESOURCE_PATH);
    }

}
