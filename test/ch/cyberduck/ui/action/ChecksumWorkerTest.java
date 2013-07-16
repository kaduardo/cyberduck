package ch.cyberduck.ui.action;

import ch.cyberduck.core.AbstractTestCase;
import ch.cyberduck.core.NullPath;
import ch.cyberduck.core.Path;

import org.junit.Test;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static org.junit.Assert.assertEquals;

/**
 * @version $Id: ChecksumWorkerTest.java 9922 2012-10-08 12:30:01Z dkocher $
 */
public class ChecksumWorkerTest extends AbstractTestCase {

    @Test
    public void testRun() throws Exception {
        final String checksum = "c1";
        final List<Path> files = new ArrayList<Path>();
        final NullPath p = new NullPath("a", Path.FILE_TYPE) {
            @Override
            public void readChecksum() {
                this.attributes().setChecksum(checksum);
            }
        };
        files.add(p);
        assertEquals(Collections.singletonList(checksum), new ChecksumWorker(files) {
            @Override
            public void cleanup(final List<String> result) {
                assertEquals(Collections.singletonList(checksum), result);
            }
        }.run());
    }
}