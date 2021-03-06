package ch.cyberduck.ui.cocoa.delegate;

/*
 *  Copyright (c) 2006 David Kocher. All rights reserved.
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

import ch.cyberduck.core.BookmarkCollection;
import ch.cyberduck.core.Host;
import ch.cyberduck.ui.cocoa.BrowserController;
import ch.cyberduck.ui.cocoa.MainController;
import ch.cyberduck.ui.cocoa.application.NSMenu;
import ch.cyberduck.ui.cocoa.application.NSMenuItem;
import ch.cyberduck.ui.cocoa.resources.IconCache;

import org.apache.log4j.Logger;
import org.rococoa.Foundation;
import org.rococoa.Selector;
import org.rococoa.cocoa.foundation.NSInteger;

/**
 * @version $Id: BookmarkMenuDelegate.java 10709 2012-12-22 19:26:49Z dkocher $
 */
public class BookmarkMenuDelegate extends CollectionMenuDelegate<Host> {
    private static Logger log = Logger.getLogger(BookmarkMenuDelegate.class);

    private BookmarkCollection collection;

    public BookmarkMenuDelegate() {
        super(BookmarkCollection.defaultCollection());
        collection = BookmarkCollection.defaultCollection();
    }

    private static final int BOOKMARKS_INDEX = 8;

    @Override
    public NSInteger numberOfItemsInMenu(NSMenu menu) {
        if(this.isPopulated()) {
            // If you return a negative value, the number of items is left unchanged
            // and menu:updateItem:atIndex:shouldCancel: is not called.
            return new NSInteger(-1);
        }
        /**
         * Toogle Bookmarks
         * Sort By
         * ----------------
         * New Bookmark
         * Edit Bookmark
         * Delete Bookmark
         * Duplicate Bookmark
         * ----------------
         * History
         * Bonjour
         * ----------------
         * ...
         */
        return new NSInteger(collection.size() + BOOKMARKS_INDEX + 3);
    }

    @Override
    public boolean menuUpdateItemAtIndex(NSMenu menu, NSMenuItem item, NSInteger index, boolean cancel) {
        if(index.intValue() == BOOKMARKS_INDEX) {
            item.setEnabled(true);
            item.setImage(IconCache.iconNamed("history.tiff", 16));
        }
        if(index.intValue() == BOOKMARKS_INDEX + 1) {
            item.setEnabled(true);
            item.setImage(IconCache.iconNamed("rendezvous.tiff", 16));
        }
        if(index.intValue() > BOOKMARKS_INDEX + 2) {
            Host h = collection.get(index.intValue() - (BOOKMARKS_INDEX + 3));
            item.setTitle(h.getNickname());
            item.setTarget(this.id());
            item.setImage(IconCache.iconNamed(h.getProtocol().icon(), 16));
            item.setAction(this.getDefaultAction());
            item.setRepresentedObject(h.getUuid());
        }
        return super.menuUpdateItemAtIndex(menu, item, index, cancel);
    }

    public void bookmarkMenuItemClicked(final NSMenuItem sender) {
        log.debug("bookmarkMenuItemClicked:" + sender);
        BrowserController controller = MainController.newDocument();
        controller.mount(collection.lookup(sender.representedObject()));
    }

    @Override
    protected Selector getDefaultAction() {
        return Foundation.selector("bookmarkMenuItemClicked:");
    }
}
