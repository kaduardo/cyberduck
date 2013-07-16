package ch.cyberduck.core;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

/**
 * @version $Id: DefaultPathKindDetectorTest.java 10006 2012-10-10 16:44:09Z dkocher $
 */
public class DefaultPathKindDetectorTest {

    @Test
    public void testDetect() throws Exception {
        DefaultPathKindDetector d = new DefaultPathKindDetector();
        assertEquals(Path.DIRECTORY_TYPE, d.detect(null));
        assertEquals(Path.DIRECTORY_TYPE, d.detect("/"));
        assertEquals(Path.DIRECTORY_TYPE, d.detect("/a"));
        assertEquals(Path.DIRECTORY_TYPE, d.detect("/a/"));
        assertEquals(Path.FILE_TYPE, d.detect("/a/b.z"));
        assertEquals(Path.FILE_TYPE, d.detect("/a/b.zip"));
    }
}
