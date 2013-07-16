package ch.cyberduck.core;

import org.junit.Test;

import static org.junit.Assert.assertNotNull;

/**
 * @version $Id: PathFactoryTest.java 9950 2012-10-08 17:11:01Z dkocher $
 */
public class PathFactoryTest extends AbstractTestCase {

    @Test
    public void testCreatePath() throws Exception {
        for(Protocol p : ProtocolFactory.getKnownProtocols()) {
            assertNotNull(PathFactory.createPath(SessionFactory.createSession(new Host(Protocol.WEBDAV, "h")), "p", Path.FILE_TYPE));
        }
    }
}
