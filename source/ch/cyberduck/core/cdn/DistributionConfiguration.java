package ch.cyberduck.core.cdn;

/*
 * Copyright (c) 2002-2010 David Kocher. All rights reserved.
 *
 * http://cyberduck.ch/
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * Bug fixes, suggestions and comments should be sent to:
 * dkocher@cyberduck.ch
 */

import ch.cyberduck.core.Path;
import ch.cyberduck.core.Protocol;

import java.util.List;

/**
 * CDN configuration actions.
 *
 * @version $Id: DistributionConfiguration.java 10565 2012-10-23 15:10:58Z dkocher $
 */
public interface DistributionConfiguration {

    /**
     * @param method Distribution method
     * @return True if configuration is known.
     */
    boolean isCached(Distribution.Method method);

    /**
     * Write distribution configuration for origin.
     *
     * @param enabled           True if distribution should be activated if not yet.
     * @param origin            Source server
     * @param method            Distribution method
     * @param cnames            CNAME entires in the DNS pointing to the same origin.
     * @param logging           True if logging should be enabled for access to CDN.
     * @param loggingBucket     The logging target
     * @param defaultRootObject Index file for root of container
     */
    void write(boolean enabled, String origin, Distribution.Method method,
               String[] cnames, boolean logging, String loggingBucket, String defaultRootObject);

    /**
     * Read distribution configuration of origin
     *
     * @param origin Source server
     * @param method Protocol
     * @return Distribution Configuration
     */
    Distribution read(String origin, Distribution.Method method);

    /**
     * Invalidate distribution objects.
     *
     * @param origin    Source server
     * @param method    Distribution method
     * @param files     Selected files or containers
     * @param recursive Apply recursively to selected container or placeholder
     */
    void invalidate(String origin, Distribution.Method method, List<Path> files, boolean recursive);

    /**
     * @param method Distribution method
     * @return True if objects in the edge location can be deleted from the CDN
     */
    boolean isInvalidationSupported(Distribution.Method method);

    /**
     * Index file for root of container
     *
     * @param method Distribution method
     * @return True if index file can be specified
     */
    boolean isDefaultRootSupported(Distribution.Method method);

    /**
     * @param method Distribution method
     * @return True if CDN is is configured logging requests to storage
     */
    boolean isLoggingSupported(Distribution.Method method);

    /**
     * @param method Distribution method
     * @return If there is an analytics provider
     */
    boolean isAnalyticsSupported(Distribution.Method method);

    /**
     * @param method Distribution method
     * @return True if CNAME for for the CDN URI can be configured
     */
    boolean isCnameSupported(Distribution.Method method);

    /**
     * List available distribution methods for this CDN.
     *
     * @param container Origin
     * @return The supported protocols
     */
    List<Distribution.Method> getMethods(String container);

    /**
     * @param method    Distribution method
     * @param container Bucket
     * @return Bucket name not fully qualified.
     */
    String getOrigin(Distribution.Method method, String container);

    /**
     * @return Hostname and port
     */
    Protocol getProtocol();

    /**
     * Marketing name for the distribution service
     *
     * @return Localized description
     */
    String getName();

    /**
     * @param method Distribution method
     * @return CDN name
     */
    String getName(Distribution.Method method);

    /**
     * Clear any cached distribution information.
     *
     * @see #isCached(ch.cyberduck.core.cdn.Distribution.Method)
     */
    void clear();
}
