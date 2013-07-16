package ch.cyberduck.core.date;

import java.text.SimpleDateFormat;

/**
 * @version $Id: MDTMMillisecondsDateFormatter.java 9949 2012-10-08 16:51:29Z dkocher $
 */
public class MDTMMillisecondsDateFormatter extends AbstractDateFormatter {

    /**
     * Format to interpret MTDM timestamp
     */
    private static final SimpleDateFormat tsFormatMilliseconds =
            new SimpleDateFormat("yyyyMMddHHmmss.SSS");

    public MDTMMillisecondsDateFormatter() {
        super(tsFormatMilliseconds);
    }
}
