package ch.cyberduck.core;

import org.junit.BeforeClass;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

/**
 * @version $Id: PathReferenceTest.java 10130 2012-10-14 16:09:30Z dkocher $
 */
public class PathReferenceTest {

    @BeforeClass
    public static void register() {
        NSObjectPathReference.register();
    }

    @Test
    public void testUnique() throws Exception {
        Path one = new NullPath("a", Path.FILE_TYPE);
        Path second = new NullPath("a", Path.FILE_TYPE);
        assertEquals(one.getReference(), second.getReference());
    }
}
