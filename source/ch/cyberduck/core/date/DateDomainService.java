package ch.cyberduck.core.date;

/**
 * @version $Id: DateDomainService.java 9963 2012-10-09 14:28:07Z dkocher $
 */
public interface DateDomainService<T> {

    T asDate(final long timestamp, final Instant precision);

}
