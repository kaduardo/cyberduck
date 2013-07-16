package ch.cyberduck.core.local;

/*
 * Copyright (c) 2012 David Kocher. All rights reserved.
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

/**
 * @version $Id: IconService.java 10258 2012-10-15 18:46:31Z dkocher $
 */
public interface IconService {

    /**
     * @param file  File
     * @param image Image name
     * @return True if icon is set
     */
    boolean setIcon(Local file, String image);

    /**
     * @param file     File
     * @param progress An integer from -1 and 9. If -1 is passed, the icon should be removed.
     * @return True if icon is set
     */
    boolean setProgress(Local file, int progress);
}
