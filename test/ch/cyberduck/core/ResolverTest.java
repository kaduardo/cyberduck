package ch.cyberduck.core;

import org.junit.Assert;
import org.junit.Test;

import java.net.UnknownHostException;

/**
 * @version $Id: ResolverTest.java 10759 2013-03-16 14:48:36Z dkocher $
 */
public class ResolverTest {

    @Test
    public void testResolve() throws Exception {
        Resolver resolver = new Resolver("cyberduck.ch");
        Assert.assertEquals("54.228.253.92", resolver.resolve().getHostAddress());
    }

    @Test(expected = UnknownHostException.class)
    public void testFailure() throws Exception {
        Resolver resolver = new Resolver("non.cyberduck.ch");
        Assert.assertNull(resolver.resolve().getHostAddress());
    }
}
