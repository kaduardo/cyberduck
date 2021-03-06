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

import ch.cyberduck.core.AbstractCollectionListener;
import ch.cyberduck.core.Collection;
import ch.cyberduck.core.Path;
import ch.cyberduck.core.Preferences;
import ch.cyberduck.core.Session;
import ch.cyberduck.core.local.LocalFactory;
import ch.cyberduck.core.transfer.NullTransferFilter;
import ch.cyberduck.core.transfer.Transfer;
import ch.cyberduck.core.transfer.TransferCollection;
import ch.cyberduck.core.transfer.TransferFilter;
import ch.cyberduck.core.transfer.download.DownloadTransfer;
import ch.cyberduck.ui.PathPasteboard;
import ch.cyberduck.ui.cocoa.application.NSDraggingInfo;
import ch.cyberduck.ui.cocoa.application.NSPasteboard;
import ch.cyberduck.ui.cocoa.application.NSTableColumn;
import ch.cyberduck.ui.cocoa.application.NSTableView;
import ch.cyberduck.ui.cocoa.foundation.NSArray;
import ch.cyberduck.ui.cocoa.foundation.NSIndexSet;
import ch.cyberduck.ui.cocoa.foundation.NSObject;
import ch.cyberduck.ui.cocoa.foundation.NSString;

import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.rococoa.cocoa.foundation.NSInteger;
import org.rococoa.cocoa.foundation.NSUInteger;

import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

/**
 * @version $Id: TransferTableDataSource.java 10524 2012-10-22 09:59:38Z dkocher $
 */
public class TransferTableDataSource extends ListDataSource {
    private static Logger log = Logger.getLogger(TransferTableDataSource.class);

    public static final String PROGRESS_COLUMN = "PROGRESS";
    // virtual column to implement keyboard selection
    protected static final String TYPEAHEAD_COLUMN = "TYPEAHEAD";

    /**
     *
     */
    private final Map<Transfer, ProgressController> controllers = new HashMap<Transfer, ProgressController>();

    public TransferTableDataSource() {
        TransferCollection.defaultCollection().addListener(new AbstractCollectionListener<Transfer>() {
            @Override
            public void collectionItemRemoved(Transfer item) {
                final ProgressController controller = controllers.remove(item);
                if(controller != null) {
                    controller.invalidate();
                }
            }
        });
    }

    @Override
    protected void invalidate() {
        cache.clear();
        super.invalidate();
    }

    /**
     *
     */
    private TransferFilter filter = new NullTransferFilter();

    /**
     * @param searchString Filter hostname or file
     */
    public void setFilter(final String searchString) {
        if(StringUtils.isBlank(searchString)) {
            // Revert to the default filter
            this.filter = new NullTransferFilter();
        }
        else {
            // Setting up a custom filter
            this.filter = new TransferFilter() {
                @Override
                public boolean accept(final Transfer transfer) {
                    // Match for path names and hostname
                    for(Path root : transfer.getRoots()) {
                        if(root.getName().toLowerCase().contains(searchString.toLowerCase())) {
                            return true;
                        }
                    }
                    for(Session s : transfer.getSessions()) {
                        if(s.getHost().getHostname().toLowerCase().contains(searchString.toLowerCase())) {
                            return true;
                        }
                    }
                    return false;
                }
            };
        }
    }

    /**
     * @return The filtered collection currently to be displayed within the constraints
     */
    protected Collection<Transfer> getSource() {
        if(filter instanceof NullTransferFilter) {
            return TransferCollection.defaultCollection();
        }
        Collection<Transfer> filtered = new Collection<Transfer>(TransferCollection.defaultCollection());
        for(Iterator<Transfer> i = filtered.iterator(); i.hasNext(); ) {
            if(!filter.accept(i.next())) {
                // Temporarily remove the transfer from the collection copy
                i.remove();
            }
        }
        return filtered;
    }

    @Override
    public NSInteger numberOfRowsInTableView(NSTableView view) {
        return new NSInteger(this.getSource().size());
    }

    /**
     * Second cache because it is expensive to create proxy instances
     */
    private AttributeCache<Transfer> cache = new AttributeCache<Transfer>(
            Preferences.instance().getInteger("queue.model.cache.size")
    );

    @Override
    public NSObject tableView_objectValueForTableColumn_row(NSTableView view, NSTableColumn tableColumn, NSInteger row) {
        if(row.intValue() >= this.numberOfRowsInTableView(view).intValue()) {
            return null;
        }
        final String identifier = tableColumn.identifier();
        final Transfer item = this.getSource().get(row.intValue());
        final NSObject cached = cache.get(item, identifier);
        if(null == cached) {
            if(identifier.equals(PROGRESS_COLUMN)) {
                return cache.put(item, identifier, null);
            }
            if(identifier.equals(TYPEAHEAD_COLUMN)) {
                return cache.put(item, identifier, NSString.stringWithString(item.getName()));
            }
            throw new IllegalArgumentException("Unknown identifier: " + identifier);
        }
        return cached;
    }

    // ----------------------------------------------------------
    // Drop methods
    // ----------------------------------------------------------

    @Override
    public NSUInteger tableView_validateDrop_proposedRow_proposedDropOperation(NSTableView view, NSDraggingInfo draggingInfo, NSInteger row, NSUInteger operation) {
        log.debug("tableViewValidateDrop:row:" + row + ",operation:" + operation);
        if(draggingInfo.draggingPasteboard().availableTypeFromArray(NSArray.arrayWithObject(NSPasteboard.StringPboardType)) != null) {
            view.setDropRow(row, NSTableView.NSTableViewDropAbove);
            return NSDraggingInfo.NSDragOperationCopy;
        }
        if(!PathPasteboard.allPasteboards().isEmpty()) {
            view.setDropRow(row, NSTableView.NSTableViewDropAbove);
            return NSDraggingInfo.NSDragOperationCopy;
        }
        log.debug("tableViewValidateDrop:DragOperationNone");
        return NSDraggingInfo.NSDragOperationNone;
    }

    /**
     * Invoked by tableView when the mouse button is released over a table view that previously decided to allow a drop.
     *
     * @param draggingInfo contains details on this dragging operation.
     * @param row          The proposed location is row and action is operation.
     */
    @Override
    public boolean tableView_acceptDrop_row_dropOperation(NSTableView view, NSDraggingInfo draggingInfo, NSInteger row, NSUInteger operation) {
        if(draggingInfo.draggingPasteboard().availableTypeFromArray(NSArray.arrayWithObject(NSPasteboard.StringPboardType)) != null) {
            String droppedText = draggingInfo.draggingPasteboard().stringForType(NSPasteboard.StringPboardType);// get the data from paste board
            if(StringUtils.isNotBlank(droppedText)) {
                log.info("NSPasteboard.StringPboardType:" + droppedText);
                DownloadController c = new DownloadController(TransferController.instance(), droppedText);
                c.beginSheet();
                return true;
            }
            return false;
        }
        final List<PathPasteboard> pasteboards = PathPasteboard.allPasteboards();
        if(pasteboards.isEmpty()) {
            return false;
        }
        for(PathPasteboard pasteboard : pasteboards) {
            if(pasteboard.isEmpty()) {
                continue;
            }
            final List<Path> downloads = pasteboard.copy();
            for(Path download : downloads) {
                download.setLocal(LocalFactory.createLocal(download.getHost().getDownloadFolder(), download.getName()));
            }
            TransferCollection.defaultCollection().add(row.intValue(), new DownloadTransfer(downloads));
            view.reloadData();
            view.selectRowIndexes(NSIndexSet.indexSetWithIndex(row), false);
            view.scrollRowToVisible(row);
        }
        pasteboards.clear();
        return true;
    }

    public ProgressController getController(int row) {
        return this.getController(this.getSource().get(row));
    }

    public ProgressController getController(Transfer t) {
        if(!controllers.containsKey(t)) {
            controllers.put(t, new ProgressController(t));
        }
        return controllers.get(t);
    }

    public boolean isHighlighted(int row) {
        return this.getController(row).isHighlighted();
    }

    public void setHighlighted(int row, boolean highlighted) {
        this.getController(row).setHighlighted(highlighted);
    }
}