package ch.cyberduck.core;

/**
 * @version $Id: NullAttributes.java 10351 2012-10-17 19:54:24Z dkocher $
 */
public class NullAttributes extends Attributes {

    @Override
    public int getType() {
        throw new UnsupportedOperationException();
    }

    @Override
    public long getSize() {
        throw new UnsupportedOperationException();
    }

    @Override
    public long getModificationDate() {
        throw new UnsupportedOperationException();
    }

    @Override
    public long getCreationDate() {
        throw new UnsupportedOperationException();
    }

    @Override
    public long getAccessedDate() {
        throw new UnsupportedOperationException();
    }

    @Override
    public Permission getPermission() {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean isDirectory() {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean isVolume() {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean isFile() {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean isSymbolicLink() {
        return false;
    }

    @Override
    public String getChecksum() {
        throw new UnsupportedOperationException();
    }
}
