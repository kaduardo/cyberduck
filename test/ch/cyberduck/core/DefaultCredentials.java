package ch.cyberduck.core;

/**
 * @version $Id: DefaultCredentials.java 10061 2012-10-11 17:02:56Z dkocher $
 */
public class DefaultCredentials extends Credentials {

    public DefaultCredentials() {
        super();
    }

    public DefaultCredentials(final String user, final String password) {
        super(user, password);
    }

    public DefaultCredentials(final String user, final String password, final boolean save) {
        super(user, password, save);
    }

    @Override
    public String getUsernamePlaceholder() {
        throw new UnsupportedOperationException();
    }

    @Override
    public String getPasswordPlaceholder() {
        throw new UnsupportedOperationException();
    }
}
