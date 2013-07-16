package ch.cyberduck.core.text;

import ch.cyberduck.core.text.NaturalOrderComparator;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

/**
 * @version $Id: NaturalOrderComparatorTest.java 10477 2012-10-19 13:28:31Z dkocher $
 */
public class NaturalOrderComparatorTest {

    @Test
    public void testCompare() throws Exception {
        assertEquals(-1, new NaturalOrderComparator().compare("123a", "a"));
        assertEquals(-1, new NaturalOrderComparator().compare("365", "400"));
    }
}
