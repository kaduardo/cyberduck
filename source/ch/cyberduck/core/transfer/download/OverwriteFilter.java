package ch.cyberduck.core.transfer.download;

import ch.cyberduck.core.transfer.symlink.SymlinkResolver;

/**
 * @version $Id: OverwriteFilter.java 10292 2012-10-16 09:36:55Z dkocher $
 */
public class OverwriteFilter extends AbstractDownloadFilter {

    public OverwriteFilter(final SymlinkResolver symlinkResolver) {
        super(symlinkResolver);
    }
}
