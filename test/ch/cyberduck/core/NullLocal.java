package ch.cyberduck.core;

import ch.cyberduck.core.local.Local;

/**
 * @version $Id: NullLocal.java 10545 2012-10-22 15:32:42Z dkocher $
 */
public class NullLocal extends Local {

    public NullLocal(final String parent, final String name) {
        super(parent, name);
    }

    @Override
    public void trash() {
        throw new UnsupportedOperationException();
    }

    @Override
    public void writeUnixPermission(final Permission permission) {
        throw new UnsupportedOperationException();
    }
}
