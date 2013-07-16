package ch.cyberduck.core;

import org.junit.Test;

import static org.junit.Assert.assertTrue;

/**
 * @version $Id: DescriptiveUrlTest.java 10503 2012-10-20 21:37:58Z dkocher $
 */
public class DescriptiveUrlTest {

    @Test
    public void testEquals() throws Exception {
        assertTrue(new DescriptiveUrl("http://host.domain", "a").equals(new DescriptiveUrl("http://host.domain", "b")));
    }
}
