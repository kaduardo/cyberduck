package ch.cyberduck.core.identity;

import ch.cyberduck.core.Credentials;

/**
 * @version $Id: IdentityConfiguration.java 9610 2012-06-24 19:32:33Z dkocher $
 */
public interface IdentityConfiguration {

    /**
     * Remove user
     *
     * @param username Username
     */
    void deleteUser(final String username);

    /**
     * Verify user exsits and get credentials from keychain
     *
     * @param username Username
     * @return Access credentials for user
     */
    Credentials getUserCredentials(String username);

    /**
     * Create new user and create access credentials
     *
     * @param username Username
     * @param policy   Policy language document
     */
    void createUser(String username, String policy);
}
