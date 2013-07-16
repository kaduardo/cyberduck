package ch.cyberduck.core;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

/**
 * @version $Id: MappingMimeTypeServiceTest.java 10471 2012-10-19 12:36:31Z dkocher $
 */
public class MappingMimeTypeServiceTest {

    @Test
    public void testGetMime() throws Exception {
        MappingMimeTypeService s = new MappingMimeTypeService();
        assertEquals("text/plain", s.getMime("f.txt"));
        assertEquals("text/plain", s.getMime("f.TXT"));
    }
}
