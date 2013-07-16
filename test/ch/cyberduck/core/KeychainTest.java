package ch.cyberduck.core;

import org.junit.BeforeClass;
import org.junit.Test;

import static org.junit.Assert.assertNull;

/**
 * @version $Id: KeychainTest.java 10335 2012-10-16 20:02:01Z dkocher $
 */
public class KeychainTest extends AbstractTestCase {

    @BeforeClass
    public static void register() {
        Keychain.register();
    }

    @Test
    public void testFind() throws Exception {
        PasswordStore k = KeychainFactory.get();
        assertNull(k.getPassword("cyberduck.ch", "u"));
        assertNull(k.getPassword(Scheme.http, 80, "cyberduck.ch", "u"));
    }
}
