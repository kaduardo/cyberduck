package ch.cyberduck.core.threading;

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
 * Action to be run on the main thread
 *
 * @version $Id: MainAction.java 6968 2010-09-16 16:06:08Z yla $
 */
public abstract class MainAction implements Runnable {

    /**
     * @return False if the action should not be run anymore because the parent container has
     * been invalidated in the mean time.
     */
    public abstract boolean isValid();

    public abstract Object lock();    
}