package ch.cyberduck.core.identity;

/**
 * @version $Id: AbstractIdentityConfiguration.java 9780 2012-10-01 09:19:12Z dkocher $
 */
public abstract class AbstractIdentityConfiguration implements IdentityConfiguration {

    @Override
    public void deleteUser(final String username) {
        //
    }

    @Override
    public void createUser(final String username, final String policy) {
        //
    }
}
