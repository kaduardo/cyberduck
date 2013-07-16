package ch.cyberduck.core;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotSame;

/**
 * @version $Id: ProtocolTest.java 10186 2012-10-15 16:45:21Z dkocher $
 */
public class ProtocolTest extends AbstractTestCase {

    @Test
    public void testEquals() {
        assertNotSame(Protocol.FTP, Protocol.FTP_TLS);
        assertEquals(Protocol.FTP, Protocol.FTP);
    }
}
