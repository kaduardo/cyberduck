package ch.cyberduck.core.transfer.symlink;

import ch.cyberduck.core.Path;

import org.apache.commons.lang.StringUtils;

/**
 * @version $Id: AbstractSymlinkResolver.java 10292 2012-10-16 09:36:55Z dkocher $
 */
public abstract class AbstractSymlinkResolver implements SymlinkResolver {

    @Override
    public String relativize(final String base, final String name) {
        final String parent = Path.getParent(base, Path.DELIMITER);
        if(name.startsWith(parent)) {
            return StringUtils.substring(name, parent.length() + 1);
        }
        else {
            return String.format("../%s", this.relativize(parent, name));
        }
    }
}