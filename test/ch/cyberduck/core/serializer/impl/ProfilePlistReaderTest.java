package ch.cyberduck.core.serializer.impl;

import ch.cyberduck.core.Profile;
import ch.cyberduck.core.local.LocalFactory;
import ch.cyberduck.core.serializer.ProfileReaderFactory;

import org.junit.BeforeClass;

import junit.framework.TestCase;

/**
 * @version $Id: ProfilePlistReaderTest.java 10432 2012-10-18 15:34:04Z dkocher $
 */
public class ProfilePlistReaderTest extends TestCase {

    @BeforeClass
    public static void register() {
        ProfilePlistReader.register();
    }

    public void testDeserialize() throws Exception {
        final Profile profile = ProfileReaderFactory.get().read(
                LocalFactory.createLocal("test/ch/cyberduck/core/serializer/impl/Dropbox.cyberduckprofile")
        );
        assertFalse(profile.isEnabled());
    }
}
