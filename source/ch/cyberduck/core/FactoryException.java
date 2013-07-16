package ch.cyberduck.core;

/**
 * @version $Id: FactoryException.java 10072 2012-10-11 20:13:04Z dkocher $
 */
public class FactoryException extends RuntimeException {
    private static final long serialVersionUID = -3392543613133815960L;

    public FactoryException(final String message, final Throwable cause) {
        super(message, cause);
    }

    public FactoryException(final String message) {
        super(message);
    }
}
