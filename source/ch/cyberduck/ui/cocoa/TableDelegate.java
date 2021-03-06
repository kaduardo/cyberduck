package ch.cyberduck.ui.cocoa;

/*
 *  Copyright (c) 2005 David Kocher. All rights reserved.
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

import org.rococoa.ID;

import java.util.Comparator;

/**
 * @version $Id: TableDelegate.java 9440 2012-02-26 18:39:08Z dkocher $
 */
public interface TableDelegate<E> {
    void enterKeyPressed(final ID sender);

    void deleteKeyPressed(final ID sender);

    boolean isSortedAscending();

    Comparator<E> getSortingComparator();

    String tooltip(E object);
}
