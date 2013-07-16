package ch.cyberduck.core.aquaticprime;

import ch.cyberduck.core.AbstractTestCase;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

/**
 * @version $Id: LicenseFactoryTest.java 10248 2012-10-15 18:29:20Z dkocher $
 */
public class LicenseFactoryTest extends AbstractTestCase {

    @Test
    public void testFind() throws Exception {
        Donation.register();
        assertEquals(LicenseFactory.EMPTY_LICENSE, LicenseFactory.find());
//        Receipt.register();
//        assertEquals(LicenseFactory.EMPTY_LICENSE, LicenseFactory.find());
    }
}
