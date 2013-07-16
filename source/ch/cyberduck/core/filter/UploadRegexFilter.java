package ch.cyberduck.core.filter;

import ch.cyberduck.core.PathFilter;
import ch.cyberduck.core.Preferences;
import ch.cyberduck.core.local.Local;

import org.apache.log4j.Logger;

import java.util.regex.Pattern;

/**
 * @version $Id: UploadRegexFilter.java 10250 2012-10-15 18:32:38Z dkocher $
 */
public class UploadRegexFilter implements PathFilter<Local> {
    private static final Logger log = Logger.getLogger(UploadRegexFilter.class);

    private final Pattern pattern
            = Pattern.compile(Preferences.instance().getProperty("queue.upload.skip.regex"));

    @Override
    public boolean accept(final Local file) {
        if(file.attributes().isDuplicate()) {
            return false;
        }
        if(Preferences.instance().getBoolean("queue.upload.skip.enable")) {
            if(pattern.matcher(file.getName()).matches()) {
                if(log.isDebugEnabled()) {
                    log.debug(String.format("Skip %s excluded with regex", file.getAbsolute()));
                }
                return false;
            }
        }
        return true;
    }
}
