package ch.cyberduck.core.date;

import java.text.ParseException;
import java.util.Date;
import java.util.TimeZone;

/**
 * @version $Id: DateFormatter.java 9949 2012-10-08 16:51:29Z dkocher $
 */
public interface DateFormatter {

    String format(Date input, TimeZone zone);

    String format(long milliseconds, TimeZone zone);

    Date parse(String input) throws ParseException;

}
