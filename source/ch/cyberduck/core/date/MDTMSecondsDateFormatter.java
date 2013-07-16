package ch.cyberduck.core.date;

import java.text.SimpleDateFormat;

/**
 * @version $Id: MDTMSecondsDateFormatter.java 9949 2012-10-08 16:51:29Z dkocher $
 */
public class MDTMSecondsDateFormatter extends AbstractDateFormatter {

    /**
     * Format to interpret MTDM timestamp
     */
    private static final SimpleDateFormat tsFormatSeconds =
            new SimpleDateFormat("yyyyMMddHHmmss");

    public MDTMSecondsDateFormatter() {
        super(tsFormatSeconds);
    }
}