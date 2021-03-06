package ch.cyberduck.ui.cocoa;

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

import ch.cyberduck.ui.cocoa.application.NSTableColumn;

import java.util.HashMap;

/**
 * @version $Id: TableColumnFactory.java 10360 2012-10-17 20:52:11Z dkocher $
 */
public class TableColumnFactory extends HashMap<String, NSTableColumn> {
    private static final long serialVersionUID = -1455753054446012489L;

    public NSTableColumn create(String identifier) {
        if(!this.containsKey(identifier)) {
            this.put(identifier, NSTableColumn.tableColumnWithIdentifier(identifier));
        }
        return this.get(identifier);
    }
}
