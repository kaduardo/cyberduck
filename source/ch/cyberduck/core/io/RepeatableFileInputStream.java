package ch.cyberduck.core.io;

/*
 *  Copyright (c) 2009 David Kocher. All rights reserved.
 *  http://cyberduck.ch/
 *
 *  This program is free software; you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation; either version 2 of the License, or
 *  (at your option) any later version.
 *
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  Bug fixes, suggestions and comments should be sent to:
 *  dkocher@cyberduck.ch
 */

import java.io.File;
import java.io.FileNotFoundException;

/**
 * @version $Id: RepeatableFileInputStream.java 9671 2012-08-22 13:23:03Z dkocher $
 */
public class RepeatableFileInputStream extends org.jets3t.service.io.RepeatableFileInputStream {

    /**
     * Creates a repeatable input stream based on a file.
     *
     * @param file Plain file
     * @throws FileNotFoundException Invalid file
     */
    public RepeatableFileInputStream(File file) throws FileNotFoundException {
        super(file);
    }
}
