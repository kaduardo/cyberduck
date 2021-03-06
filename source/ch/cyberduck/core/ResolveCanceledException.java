package ch.cyberduck.core;

/*
 *  Copyright (c) 2007 David Kocher. All rights reserved.
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

/**
 * @version $Id: ResolveCanceledException.java 9665 2012-08-22 13:15:35Z dkocher $
 */
public class ResolveCanceledException extends ConnectionCanceledException {
    private static final long serialVersionUID = -8022014902668414964L;

    public ResolveCanceledException() {
        super();
    }

    public ResolveCanceledException(String s) {
        super(s);
    }
}
