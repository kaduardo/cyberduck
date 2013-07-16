package ch.cyberduck.core.transfer.upload;

import ch.cyberduck.core.transfer.symlink.SymlinkResolver;

/**
 * @version $Id: OverwriteFilter.java 10292 2012-10-16 09:36:55Z dkocher $
 */
public class OverwriteFilter extends AbstractUploadFilter {

    public OverwriteFilter(final SymlinkResolver symlinkResolver) {
        super(symlinkResolver);
    }
}
