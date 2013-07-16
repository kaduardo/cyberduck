package ch.cyberduck.core.transfer.symlink;

import ch.cyberduck.core.Path;

/**
 * @version $Id: NullSymlinkResolver.java 10292 2012-10-16 09:36:55Z dkocher $
 */
public class NullSymlinkResolver extends AbstractSymlinkResolver {

    @Override
    public boolean resolve(final Path file) {
        return false;
    }

    @Override
    public boolean include(final Path file) {
        return true;
    }
}
