package ch.cyberduck.core;

import org.junit.Test;

import static org.junit.Assert.assertFalse;

/**
 * @version $Id: ProtocolFactoryTest.java 9950 2012-10-08 17:11:01Z dkocher $
 */
public class ProtocolFactoryTest extends AbstractTestCase {

    @Test
    public void testRegister() throws Exception {
        assertFalse(ProtocolFactory.getKnownProtocols().isEmpty());
    }
}