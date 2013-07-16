package ch.cyberduck.core;

/**
 * @version $Id: NullProtocol.java 10983 2013-05-02 10:26:52Z dkocher $
 */
public class NullProtocol extends Protocol {

    @Override
    public String getIdentifier() {
        return "null";
    }

    @Override
    public String getDescription() {
        throw new UnsupportedOperationException();
    }

    @Override
    public Type getType() {
        return Type.ftp;
    }

    @Override
    public Scheme getScheme() {
        return Scheme.http;
    }
}
