package ch.cyberduck.core.importer;

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

import ch.cyberduck.core.Preferences;
import ch.cyberduck.core.local.Local;
import ch.cyberduck.core.local.LocalFactory;

/**
 * @version $Id: CloudberryS3BookmarkCollection.java 10142 2012-10-15 10:29:48Z dkocher $
 */
public class CloudberryS3BookmarkCollection extends CloudberryBookmarkCollection {
    private static final long serialVersionUID = -7710256176319275583L;

    @Override
    public String getBundleIdentifier() {
        return "com.cloudberrylab.explorer.s3";
    }

    @Override
    public String getName() {
        return "CloudBerry Explorer for Amazon S3";
    }

    @Override
    public Local getFile() {
        return LocalFactory.createLocal(Preferences.instance().getProperty("bookmark.import.cloudberry.s3.location"));
    }
}