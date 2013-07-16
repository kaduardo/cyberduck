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

import ch.cyberduck.core.AbstractPath;

/**
 * @version $Id: FileDescriptor.java 10372 2012-10-18 08:01:45Z dkocher $
 */
public interface FileDescriptor {

    /**
     * Human readable localized description of file type
     *
     * @param file Filename
     * @return Description of file type
     */
    String getKind(AbstractPath file);

    /**
     * Human readable localized description of file type
     *
     * @param filename Filename
     * @return Description of file type
     */
    String getKind(String filename);
}
