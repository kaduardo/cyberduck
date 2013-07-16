package ch.cyberduck.core;

import ch.cyberduck.core.local.LocalFactory;
import ch.cyberduck.core.serializer.ProfileReaderFactory;

import org.junit.Test;

import static org.junit.Assert.*;

/**
 * @version $Id: ProfileTest.java 10845 2013-04-11 14:58:16Z dkocher $
 */
public class ProfileTest extends AbstractTestCase {

    @Test
    public void testEquals() throws Exception {
        final Profile profile = ProfileReaderFactory.get().read(
                LocalFactory.createLocal("profiles/Eucalyptus Walrus S3.cyberduckprofile")
        );
        assertEquals(Protocol.Type.s3, profile.getType());
        assertEquals(Protocol.S3_SSL, profile.getProtocol());
        assertNotSame(Protocol.S3_SSL.getDefaultHostname(), profile.getDefaultHostname());
        assertEquals(Protocol.S3_SSL.getScheme(), profile.getScheme());
        assertEquals("eucalyptus", profile.getProvider());

    }

    @Test
    public void testProvider() throws Exception {
        final Profile profile = ProfileReaderFactory.get().read(
                LocalFactory.createLocal("profiles/HP Cloud Object Storage (US East).cyberduckprofile")
        );
        assertEquals(Protocol.Type.swift, profile.getType());
        assertEquals(Protocol.SWIFT, profile.getProtocol());
        assertNotSame(Protocol.CLOUDFILES.getDefaultHostname(), profile.getDefaultHostname());
        assertEquals(Scheme.https, profile.getScheme());
        assertNotNull(profile.getProvider());
    }
}