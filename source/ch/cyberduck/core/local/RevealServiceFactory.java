package ch.cyberduck.core.local;

import ch.cyberduck.core.Factory;
import ch.cyberduck.core.FactoryException;

import java.util.HashMap;
import java.util.Map;

/**
 * @version $Id: RevealServiceFactory.java 10545 2012-10-22 15:32:42Z dkocher $
 */
public abstract class RevealServiceFactory extends Factory<RevealService> {

    private static final Map<Factory.Platform, RevealServiceFactory> factories
            = new HashMap<Factory.Platform, RevealServiceFactory>();

    public static void addFactory(Factory.Platform platform, RevealServiceFactory f) {
        factories.put(platform, f);
    }

    public static RevealService get() {
        if(!factories.containsKey(NATIVE_PLATFORM)) {
            throw new FactoryException(String.format("No implementation for %s", NATIVE_PLATFORM));
        }
        return factories.get(NATIVE_PLATFORM).create();
    }
}
