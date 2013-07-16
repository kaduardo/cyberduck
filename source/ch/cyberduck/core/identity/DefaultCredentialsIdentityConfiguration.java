package ch.cyberduck.core.identity;

import ch.cyberduck.core.Credentials;
import ch.cyberduck.core.Host;

/**
 * @version $Id: DefaultCredentialsIdentityConfiguration.java 9780 2012-10-01 09:19:12Z dkocher $
 */
public class DefaultCredentialsIdentityConfiguration extends AbstractIdentityConfiguration {

    private Host host;

    public DefaultCredentialsIdentityConfiguration(final Host host) {
        this.host = host;
    }

    @Override
    public Credentials getUserCredentials(final String username) {
        return host.getCredentials();
    }
}