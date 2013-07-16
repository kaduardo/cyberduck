package ch.cyberduck.core;

import org.junit.Test;

import static org.junit.Assert.assertTrue;

/**
 * @version $Id: PreferencesUseragentProviderTest.java 10501 2012-10-20 21:23:15Z dkocher $
 */
public class PreferencesUseragentProviderTest extends AbstractTestCase {

    @Test
    public void testGet() throws Exception {
        assertTrue(new PreferencesUseragentProvider().get().startsWith("Cyberduck/"));
    }
}
