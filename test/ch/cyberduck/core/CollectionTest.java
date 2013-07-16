package ch.cyberduck.core;

import org.junit.Assert;
import org.junit.Test;

/**
 * @version $Id: CollectionTest.java 9514 2012-04-07 14:46:05Z dkocher $
 */
public class CollectionTest {

    @Test
    public void testClear() throws Exception {
        Collection<Object> c = new Collection<Object>();
        c.add(new Object());
        c.clear();
        Assert.assertTrue(c.isEmpty());
    }
}
