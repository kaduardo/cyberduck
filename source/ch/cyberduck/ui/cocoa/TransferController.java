package ch.cyberduck.ui.cocoa;

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

import ch.cyberduck.core.AbstractCollectionListener;
import ch.cyberduck.core.Collection;
import ch.cyberduck.core.Path;
import ch.cyberduck.core.Preferences;
import ch.cyberduck.core.Session;
import ch.cyberduck.core.formatter.SizeFormatterFactory;
import ch.cyberduck.core.i18n.Locale;
import ch.cyberduck.core.io.BandwidthThrottle;
import ch.cyberduck.core.local.ApplicationLauncherFactory;
import ch.cyberduck.core.local.LocalFactory;
import ch.cyberduck.core.local.RevealService;
import ch.cyberduck.core.local.RevealServiceFactory;
import ch.cyberduck.core.threading.AbstractBackgroundAction;
import ch.cyberduck.core.threading.BackgroundAction;
import ch.cyberduck.core.transfer.Queue;
import ch.cyberduck.core.transfer.Transfer;
import ch.cyberduck.core.transfer.TransferAction;
import ch.cyberduck.core.transfer.TransferAdapter;
import ch.cyberduck.core.transfer.TransferCollection;
import ch.cyberduck.core.transfer.TransferListener;
import ch.cyberduck.core.transfer.TransferOptions;
import ch.cyberduck.core.transfer.TransferPrompt;
import ch.cyberduck.core.transfer.download.DownloadTransfer;
import ch.cyberduck.core.transfer.synchronisation.SyncTransfer;
import ch.cyberduck.ui.PathPasteboard;
import ch.cyberduck.ui.cocoa.application.*;
import ch.cyberduck.ui.cocoa.delegate.AbstractMenuDelegate;
import ch.cyberduck.ui.cocoa.foundation.NSArray;
import ch.cyberduck.ui.cocoa.foundation.NSAttributedString;
import ch.cyberduck.ui.cocoa.foundation.NSIndexSet;
import ch.cyberduck.ui.cocoa.foundation.NSNotification;
import ch.cyberduck.ui.cocoa.foundation.NSNotificationCenter;
import ch.cyberduck.ui.cocoa.foundation.NSRange;
import ch.cyberduck.ui.cocoa.resources.IconCache;
import ch.cyberduck.ui.cocoa.threading.AlertRepeatableBackgroundAction;
import ch.cyberduck.ui.cocoa.threading.WindowMainAction;
import ch.cyberduck.ui.cocoa.view.ControllerCell;
import ch.cyberduck.ui.threading.ControllerMainAction;

import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.rococoa.Foundation;
import org.rococoa.ID;
import org.rococoa.Rococoa;
import org.rococoa.Selector;
import org.rococoa.cocoa.CGFloat;
import org.rococoa.cocoa.foundation.NSInteger;
import org.rococoa.cocoa.foundation.NSSize;
import org.rococoa.cocoa.foundation.NSUInteger;

import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.StringTokenizer;

/**
 * @version $Id: TransferController.java 10999 2013-05-03 09:18:26Z dkocher $
 */
public final class TransferController extends WindowController implements NSToolbar.Delegate {
    private static Logger log = Logger.getLogger(TransferController.class);

    private static TransferController instance = null;

    private NSToolbar toolbar;

    private RevealService reveal = RevealServiceFactory.get();

    @Override
    public void awakeFromNib() {
        this.toolbar = NSToolbar.toolbarWithIdentifier("Queue Toolbar");
        this.toolbar.setDelegate(this.id());
        this.toolbar.setAllowsUserCustomization(true);
        this.toolbar.setAutosavesConfiguration(true);
        this.window.setToolbar(toolbar);

        TransferCollection source = TransferCollection.defaultCollection();
        if(!source.isLoaded()) {
            transferSpinner.startAnimation(null);
        }
        source.addListener(new AbstractCollectionListener<Transfer>() {
            @Override
            public void collectionLoaded() {
                invoke(new WindowMainAction(TransferController.this) {
                    @Override
                    public void run() {
                        transferSpinner.stopAnimation(null);
                        transferTable.setGridStyleMask(NSTableView.NSTableViewSolidHorizontalGridLineMask);
                    }
                });
            }
        });
        if(source.isLoaded()) {
            transferSpinner.stopAnimation(null);
            transferTable.setGridStyleMask(NSTableView.NSTableViewSolidHorizontalGridLineMask);
        }

        super.awakeFromNib();
    }

    @Override
    public void setWindow(NSWindow window) {
        window.setMovableByWindowBackground(true);
        window.setTitle(Locale.localizedString("Transfers"));
        window.setDelegate(this.id());
        super.setWindow(window);
    }

    @Override
    public boolean isSingleton() {
        return true;
    }

    @Override
    public void windowDidBecomeKey(NSNotification notification) {
        this.updateHighlight();
    }

    @Override
    public void windowDidResignKey(NSNotification notification) {
        this.updateHighlight();
    }

    @Override
    public void windowDidBecomeMain(NSNotification notification) {
        this.updateHighlight();
    }

    @Override
    public void windowDidResignMain(NSNotification notification) {
        this.updateHighlight();
    }

    @Outlet
    private NSTextField urlField;

    public void setUrlField(NSTextField urlField) {
        this.urlField = urlField;
        this.urlField.setAllowsEditingTextAttributes(true);
        this.urlField.setSelectable(true);
    }

    @Outlet
    private NSTextField localField;

    public void setLocalField(NSTextField localField) {
        this.localField = localField;
        this.localField.setAllowsEditingTextAttributes(true);
        this.localField.setSelectable(true);
    }

    @Outlet
    private NSTextField localLabel;

    public void setLocalLabel(NSTextField localLabel) {
        this.localLabel = localLabel;
        this.localLabel.setStringValue(Locale.localizedString("Local File:", "Transfer"));
    }

    @Outlet
    private NSImageView iconView;

    public void setIconView(final NSImageView iconView) {
        this.iconView = iconView;
    }

    @Outlet
    private NSStepper queueSizeStepper;

    public void setQueueSizeStepper(final NSStepper queueSizeStepper) {
        this.queueSizeStepper = queueSizeStepper;
        this.queueSizeStepper.setTarget(this.id());
        this.queueSizeStepper.setAction(Foundation.selector("queueSizeStepperChanged:"));
    }

    @Action
    public void queueSizeStepperChanged(final ID sender) {
        Queue.instance().resize();
    }

    @Outlet
    private NSTextField filterField;

    public void setFilterField(NSTextField filterField) {
        this.filterField = filterField;
        NSNotificationCenter.defaultCenter().addObserver(this.id(),
                Foundation.selector("filterFieldTextDidChange:"),
                NSControl.NSControlTextDidChangeNotification,
                this.filterField);
    }

    public void filterFieldTextDidChange(NSNotification notification) {
        transferTableModel.setFilter(filterField.stringValue());
        this.reload();
    }

    @Outlet
    NSProgressIndicator transferSpinner;

    public void setTransferSpinner(NSProgressIndicator transferSpinner) {
        this.transferSpinner = transferSpinner;
    }

    /**
     * Change focus to filter field
     *
     * @param sender Search field
     */
    @Action
    public void searchButtonClicked(final ID sender) {
        this.window().makeFirstResponder(this.filterField);
    }

    private TranscriptController transcript;

    private NSDrawer logDrawer;

    public void drawerWillOpen(NSNotification notification) {
        logDrawer.setContentSize(new NSSize(logDrawer.contentSize().width.doubleValue(),
                Preferences.instance().getDouble("queue.logDrawer.size.height")
        ));
    }

    public void drawerDidOpen(NSNotification notification) {
        Preferences.instance().setProperty("queue.logDrawer.isOpen", true);
    }

    public void drawerWillClose(NSNotification notification) {
        Preferences.instance().setProperty("queue.logDrawer.size.height",
                logDrawer.contentSize().height.doubleValue());
    }

    public void drawerDidClose(NSNotification notification) {
        Preferences.instance().setProperty("queue.logDrawer.isOpen", false);
    }

    public void setLogDrawer(NSDrawer logDrawer) {
        this.logDrawer = logDrawer;
        this.transcript = new TranscriptController();
        this.logDrawer.setContentView(this.transcript.getLogView());
        NSNotificationCenter.defaultCenter().addObserver(this.id(),
                Foundation.selector("drawerWillOpen:"),
                NSDrawer.DrawerWillOpenNotification,
                this.logDrawer);
        NSNotificationCenter.defaultCenter().addObserver(this.id(),
                Foundation.selector("drawerDidOpen:"),
                NSDrawer.DrawerDidOpenNotification,
                this.logDrawer);
        NSNotificationCenter.defaultCenter().addObserver(this.id(),
                Foundation.selector("drawerWillClose:"),
                NSDrawer.DrawerWillCloseNotification,
                this.logDrawer);
        NSNotificationCenter.defaultCenter().addObserver(this.id(),
                Foundation.selector("drawerDidClose:"),
                NSDrawer.DrawerDidCloseNotification,
                this.logDrawer);
    }

    public void toggleLogDrawer(final ID sender) {
        this.logDrawer.toggle(sender);
    }

    @Outlet
    private NSPopUpButton bandwidthPopup;

    private AbstractMenuDelegate bandwidthPopupDelegate;

    public void setBandwidthPopup(NSPopUpButton bandwidthPopup) {
        this.bandwidthPopup = bandwidthPopup;
        this.bandwidthPopup.setEnabled(false);
        this.bandwidthPopup.setAllowsMixedState(true);
        this.bandwidthPopup.setTarget(this.id());
        this.bandwidthPopup.setAction(Foundation.selector("bandwidthPopupChanged:"));
        this.bandwidthPopup.removeAllItems();
        this.bandwidthPopup.addItemWithTitle(StringUtils.EMPTY);
        this.bandwidthPopup.lastItem().setImage(IconCache.iconNamed("bandwidth.tiff", 16));
        this.bandwidthPopup.addItemWithTitle(Locale.localizedString("Unlimited Bandwidth", "Transfer"));
        this.bandwidthPopup.lastItem().setRepresentedObject(String.valueOf(BandwidthThrottle.UNLIMITED));
        this.bandwidthPopup.menu().addItem(NSMenuItem.separatorItem());
        final StringTokenizer options = new StringTokenizer(Preferences.instance().getProperty("queue.bandwidth.options"), ",");
        while(options.hasMoreTokens()) {
            final String bytes = options.nextToken();
            this.bandwidthPopup.addItemWithTitle(SizeFormatterFactory.get().format(Integer.parseInt(bytes)) + "/s");
            this.bandwidthPopup.lastItem().setRepresentedObject(bytes);
        }
        this.bandwidthPopup.menu().setDelegate((this.bandwidthPopupDelegate = new BandwidthMenuDelegate()).id());
    }

    private class BandwidthMenuDelegate extends AbstractMenuDelegate {
        @Override
        public NSInteger numberOfItemsInMenu(NSMenu menu) {
            return new NSInteger(new StringTokenizer(Preferences.instance().getProperty("queue.bandwidth.options"), ",").countTokens() + 3);
        }

        @Override
        public boolean menuUpdateItemAtIndex(NSMenu menu, NSMenuItem item, NSInteger i, boolean cancel) {
            if(item.representedObject() != null) {
                final int selected = transferTable.numberOfSelectedRows().intValue();
                int bytes = Integer.valueOf(item.representedObject());
                NSIndexSet iterator = transferTable.selectedRowIndexes();
                for(NSUInteger index = iterator.firstIndex(); !index.equals(NSIndexSet.NSNotFound); index = iterator.indexGreaterThanIndex(index)) {
                    Transfer transfer = TransferCollection.defaultCollection().get(index.intValue());
                    if(BandwidthThrottle.UNLIMITED == transfer.getBandwidth().getRate()) {
                        if(BandwidthThrottle.UNLIMITED == bytes) {
                            item.setState(selected > 1 ? NSCell.NSMixedState : NSCell.NSOnState);
                            break;
                        }
                        else {
                            item.setState(NSCell.NSOffState);
                        }
                    }
                    else {
                        int bandwidth = (int) transfer.getBandwidth().getRate();
                        if(bytes == bandwidth) {
                            item.setState(selected > 1 ? NSCell.NSMixedState : NSCell.NSOnState);
                            break;
                        }
                        else {
                            item.setState(NSCell.NSOffState);
                        }
                    }
                }
            }
            return super.menuUpdateItemAtIndex(menu, item, i, cancel);
        }

        @Override
        protected Selector getDefaultAction() {
            return Foundation.selector("bandwidthPopupChanged:");
        }
    }

    @Action
    public void bandwidthPopupChanged(NSPopUpButton sender) {
        NSIndexSet selected = transferTable.selectedRowIndexes();
        float bandwidth = Float.valueOf(sender.selectedItem().representedObject());
        for(NSUInteger index = selected.firstIndex(); !index.equals(NSIndexSet.NSNotFound); index = selected.indexGreaterThanIndex(index)) {
            Transfer transfer = TransferCollection.defaultCollection().get(index.intValue());
            transfer.setBandwidth(bandwidth);
        }
        this.updateBandwidthPopup();
    }

    /**
     * Loading bundle
     */
    private TransferController() {
        this.loadBundle();
        TransferCollection.defaultCollection().addListener(new AbstractCollectionListener<Transfer>() {
            @Override
            public void collectionLoaded() {
                invoke(new ControllerMainAction(TransferController.this) {
                    @Override
                    public void run() {
                        reload();
                    }
                });
            }

            @Override
            public void collectionItemAdded(Transfer item) {
                invoke(new ControllerMainAction(TransferController.this) {
                    @Override
                    public void run() {
                        reload();
                    }
                });
            }

            @Override
            public void collectionItemRemoved(Transfer item) {
                invoke(new ControllerMainAction(TransferController.this) {
                    @Override
                    public void run() {
                        reload();
                    }
                });
            }
        });
    }

    public static TransferController instance() {
        synchronized(NSApplication.sharedApplication()) {
            if(null == instance) {
                instance = new TransferController();
            }
            return instance;
        }
    }

    @Override
    protected String getBundleName() {
        return "Transfer";
    }

    @Override
    protected void invalidate() {
        toolbar.setDelegate(null);
        toolbarItems.clear();
        transferTableModel.invalidate();
        bandwidthPopup.menu().setDelegate(null);
        super.invalidate();
    }

    /**
     * @param app Singleton
     * @return NSApplication.TerminateLater or NSApplication.TerminateNow depending if there are
     *         running transfers to be checked first
     */
    public static NSUInteger applicationShouldTerminate(final NSApplication app) {
        if(null != instance) {
            //Saving state of transfer window
            Preferences.instance().setProperty("queue.openByDefault", instance.window().isVisible());
            if(TransferCollection.defaultCollection().numberOfRunningTransfers() > 0) {
                final NSAlert alert = NSAlert.alert(Locale.localizedString("Transfer in progress"), //title
                        Locale.localizedString("There are files currently being transferred. Quit anyway?"), // message
                        Locale.localizedString("Quit"), // defaultbutton
                        Locale.localizedString("Cancel"), //alternative button
                        null //other button
                );
                instance.alert(alert, new SheetCallback() {
                    @Override
                    public void callback(int returncode) {
                        if(returncode == DEFAULT_OPTION) { //Quit
                            for(Transfer transfer : TransferCollection.defaultCollection()) {
                                if(transfer.isRunning()) {
                                    transfer.interrupt();
                                }
                            }
                            app.replyToApplicationShouldTerminate(true);
                        }
                        if(returncode == CANCEL_OPTION) { //Cancel
                            app.replyToApplicationShouldTerminate(false);
                        }
                    }
                });
                return NSApplication.NSTerminateLater; //break
            }
        }
        return NSApplication.NSTerminateNow;
    }

    private final TableColumnFactory tableColumnsFactory = new TableColumnFactory();

    @Outlet
    private NSTableView transferTable;
    private TransferTableDataSource transferTableModel;
    private AbstractTableDelegate<Transfer> transferTableDelegate;

    public void setQueueTable(NSTableView view) {
        this.transferTable = view;
        this.transferTable.setRowHeight(new CGFloat(82));
        this.transferTable.setDataSource((transferTableModel = new TransferTableDataSource()).id());
        this.transferTable.setDelegate((transferTableDelegate = new AbstractTableDelegate<Transfer>() {
            @Override
            public String tooltip(Transfer t) {
                return t.getName();
            }

            @Override
            public void enterKeyPressed(final ID sender) {
                this.tableRowDoubleClicked(sender);
            }

            @Override
            public void deleteKeyPressed(final ID sender) {
                deleteButtonClicked(sender);
            }

            @Override
            public void tableColumnClicked(NSTableView view, NSTableColumn tableColumn) {
                //
            }

            @Override
            public void tableRowDoubleClicked(final ID sender) {
                reloadButtonClicked(sender);
            }

            @Override
            public void selectionIsChanging(NSNotification notification) {
                updateHighlight();
            }

            @Override
            public void selectionDidChange(NSNotification notification) {
                updateHighlight();
                updateSelection();
                transferTable.noteHeightOfRowsWithIndexesChanged(
                        NSIndexSet.indexSetWithIndexesInRange(
                                NSRange.NSMakeRange(new NSUInteger(0), new NSUInteger(transferTable.numberOfRows()))));
            }

            public void tableView_willDisplayCell_forTableColumn_row(NSTableView view, NSCell cell, NSTableColumn tableColumn, NSInteger row) {
                Rococoa.cast(cell, ControllerCell.class).setView(transferTableModel.getController(row.intValue()).view());
            }

            @Override
            public boolean isTypeSelectSupported() {
                return true;
            }

            public String tableView_typeSelectStringForTableColumn_row(NSTableView tableView,
                                                                       NSTableColumn tableColumn,
                                                                       NSInteger row) {
                return transferTableModel.getSource().get(row.intValue()).getName();
            }
        }).id());
        // receive drag events from types
        // in fact we are not interested in file promises, but because the browser model can only initiate
        // a drag with tableView.dragPromisedFilesOfTypes(), we listens for those events
        // and then use the private pasteboard instead.
        this.transferTable.registerForDraggedTypes(NSArray.arrayWithObjects(
                NSPasteboard.StringPboardType,
                NSPasteboard.FilesPromisePboardType));

        {
            NSTableColumn c = tableColumnsFactory.create(TransferTableDataSource.PROGRESS_COLUMN);
            c.setMinWidth(80f);
            c.setWidth(300f);
            c.setResizingMask(NSTableColumn.NSTableColumnAutoresizingMask);
            c.setDataCell(prototype);
            this.transferTable.addTableColumn(c);
        }
        this.transferTable.setGridStyleMask(NSTableView.NSTableViewGridNone);
        //selection properties
        this.transferTable.setAllowsMultipleSelection(true);
        this.transferTable.setAllowsEmptySelection(true);
        this.transferTable.setAllowsColumnReordering(false);
        this.transferTable.sizeToFit();
    }

    private final NSCell prototype = ControllerCell.controllerCell();

    /**
     *
     */
    private void updateHighlight() {
        boolean main = window().isMainWindow();
        NSIndexSet set = transferTable.selectedRowIndexes();
        for(int i = 0; i < transferTableModel.numberOfRowsInTableView(transferTable).intValue(); i++) {
            boolean highlighted = set.containsIndex(new NSUInteger(i)) && main;
            if(transferTableModel.isHighlighted(i) == highlighted) {
                continue;
            }
            transferTableModel.setHighlighted(i, highlighted);
        }
    }

    /**
     *
     */
    private void updateSelection() {
        this.updateLabels();
        this.updateIcon();
        this.updateBandwidthPopup();
        toolbar.validateVisibleItems();
    }

    private void updateLabels() {
        final int selected = transferTable.numberOfSelectedRows().intValue();
        if(1 == selected) {
            final Transfer transfer = transferTableModel.getSource().get(transferTable.selectedRow().intValue());
            // Draw text fields at the bottom
            if(transfer.numberOfRoots() == 1) {
                urlField.setAttributedStringValue(
                        HyperlinkAttributedStringFactory.create(transfer.getRemote()));
                localField.setAttributedStringValue(
                        HyperlinkAttributedStringFactory.create(transfer.getLocal()));
            }
            else {
                urlField.setAttributedStringValue(NSAttributedString.attributedStringWithAttributes(
                        Locale.localizedString("Multiple files"),
                        TRUNCATE_MIDDLE_ATTRIBUTES));
                localField.setAttributedStringValue(NSAttributedString.attributedStringWithAttributes(
                        Locale.localizedString("Multiple files"),
                        TRUNCATE_MIDDLE_ATTRIBUTES));
            }
        }
        else {
            urlField.setStringValue(StringUtils.EMPTY);
            localField.setStringValue(StringUtils.EMPTY);
        }
    }

    private void updateIcon() {
        final int selected = transferTable.numberOfSelectedRows().intValue();
        if(1 == selected) {
            final Transfer transfer = transferTableModel.getSource().get(transferTable.selectedRow().intValue());
            // Draw file type icon
            if(transfer.numberOfRoots() == 1) {
                if(transfer.getLocal() != null) {
                    iconView.setImage(IconCache.instance().iconForPath(transfer.getRoot().getLocal(), 32));
                }
                else {
                    iconView.setImage(IconCache.instance().iconForPath(transfer.getRoot(), 32));
                }
            }
            else {
                iconView.setImage(IconCache.iconNamed("NSMultipleDocuments", 32));
            }
        }
        else {
            iconView.setImage(null);
        }
    }

    private void updateBandwidthPopup() {
        final int selected = transferTable.numberOfSelectedRows().intValue();
        bandwidthPopup.setEnabled(selected > 0);
        NSIndexSet set = transferTable.selectedRowIndexes();
        for(NSUInteger index = set.firstIndex(); !index.equals(NSIndexSet.NSNotFound); index = set.indexGreaterThanIndex(index)) {
            final Transfer transfer = transferTableModel.getSource().get(index.intValue());
            if(transfer instanceof SyncTransfer) {
                // Currently we do not support bandwidth throtling for sync transfers due to
                // the problem of mapping both download and upload rate in the GUI
                bandwidthPopup.setEnabled(false);
                // Break through and set the standard icon below
                break;
            }
            if(transfer.getBandwidth().getRate() != BandwidthThrottle.UNLIMITED) {
                // Mark as throttled
                this.bandwidthPopup.itemAtIndex(new NSInteger(0)).setImage(IconCache.iconNamed("turtle.tiff"));
                return;
            }
        }
        // Set the standard icon
        this.bandwidthPopup.itemAtIndex(new NSInteger(0)).setImage(IconCache.iconNamed("bandwidth.tiff", 16));
    }

    private void reload() {
        while(transferTable.subviews().count().intValue() > 0) {
            (Rococoa.cast(transferTable.subviews().lastObject(), NSView.class)).removeFromSuperviewWithoutNeedingDisplay();
        }
        transferTable.reloadData();
        this.updateHighlight();
        this.updateSelection();
    }

    /**
     * Add this item to the list; select it and scroll the view to make it visible
     *
     * @param transfer Transfer
     */
    public void addTransfer(final Transfer transfer, final BackgroundAction action) {
        final TransferCollection collection = TransferCollection.defaultCollection();
        if(collection.size() > Preferences.instance().getInteger("queue.size.warn")) {
            final NSAlert alert = NSAlert.alert(
                    Locale.localizedString(TOOLBAR_CLEAN_UP), //title
                    Locale.localizedString("Remove completed transfers from list."), // message
                    Locale.localizedString(TOOLBAR_CLEAN_UP), // defaultbutton
                    Locale.localizedString("Cancel"), // alternate button
                    null //other button
            );
            alert.setShowsSuppressionButton(true);
            alert.suppressionButton().setTitle(Locale.localizedString("Don't ask again", "Configuration"));
            this.alert(alert, new SheetCallback() {
                @Override
                public void callback(int returncode) {
                    if(alert.suppressionButton().state() == NSCell.NSOnState) {
                        // Never show again.
                        Preferences.instance().setProperty("queue.size.warn", Integer.MAX_VALUE);
                    }
                    if(returncode == DEFAULT_OPTION) {
                        clearButtonClicked(null);
                    }
                    addTransfer(transfer);
                    background(action);
                }
            });
        }
        else {
            this.addTransfer(transfer);
            this.background(action);
        }
    }

    private void addTransfer(final Transfer transfer) {
        final TransferCollection collection = TransferCollection.defaultCollection();
        collection.add(transfer);
        final int row = collection.size() - 1;
        final NSInteger index = new NSInteger(row);
        transferTable.selectRowIndexes(NSIndexSet.indexSetWithIndex(index), false);
        transferTable.scrollRowToVisible(index);
    }

    /**
     * @param transfer Transfer
     */
    public void startTransfer(final Transfer transfer) {
        this.startTransfer(transfer, false, false);
    }

    /**
     * @param transfer        Transfer
     * @param resumeRequested Resume button clicked
     * @param reloadRequested Reload button clicked
     */
    private void startTransfer(final Transfer transfer, final boolean resumeRequested, final boolean reloadRequested) {
        if(Preferences.instance().getBoolean("queue.orderFrontOnStart")) {
            this.window().makeKeyAndOrderFront(null);
        }
        final AlertRepeatableBackgroundAction action = new AlertRepeatableBackgroundAction(this) {
            private boolean resume = resumeRequested;
            private boolean reload = reloadRequested;

            private TransferListener tl;

            @Override
            public boolean prepare() {
                transfer.addListener(tl = new TransferAdapter() {
                    @Override
                    public void transferQueued() {
                        validateToolbar();
                    }

                    @Override
                    public void transferResumed() {
                        validateToolbar();
                    }

                    @Override
                    public void transferWillStart() {
                        validateToolbar();
                    }

                    @Override
                    public void transferDidEnd() {
                        validateToolbar();
                    }
                });
                // Attach listeners
                super.prepare();
                // Always continue. Current status might be canceled if interrupted before.
                return true;
            }

            @Override
            public void run() {
                final TransferOptions options = new TransferOptions();
                options.reloadRequested = reload;
                options.resumeRequested = resume;
                transfer.start(new TransferPrompt() {
                    @Override
                    public TransferAction prompt() {
                        return TransferPromptController.create(TransferController.this, transfer).prompt();
                    }
                }, options);
            }

            @Override
            public void finish() {
                super.finish();
                transfer.removeListener(tl);
            }

            @Override
            public void cleanup() {
                final TransferCollection collection = TransferCollection.defaultCollection();
                if(transfer.isComplete() && !transfer.isCanceled() && transfer.isReset()) {
                    if(Preferences.instance().getBoolean("queue.removeItemWhenComplete")) {
                        collection.remove(transfer);
                    }
                    if(Preferences.instance().getBoolean("queue.orderBackOnStop")) {
                        if(!(collection.numberOfRunningTransfers() > 0)) {
                            window().close();
                        }
                    }
                }
                collection.save();
            }

            @Override
            public List<Session> getSessions() {
                return transfer.getSessions();
            }

            @Override
            public String getActivity() {
                return transfer.getName();
            }

            @Override
            public void pause() {
                transfer.fireTransferQueued();
                // Upon retry do not suggest to overwrite already completed items from the transfer
                reload = false;
                resume = true;
                super.pause();
                transfer.fireTransferResumed();
            }

            @Override
            public boolean isCanceled() {
                return transfer.isCanceled();
            }

            @Override
            public void log(final boolean request, final String message) {
                if(logDrawer.state() == NSDrawer.OpenState) {
                    invoke(new WindowMainAction(TransferController.this) {
                        @Override
                        public void run() {
                            TransferController.this.transcript.log(request, message);
                        }
                    });
                }
                super.log(request, message);
            }

            private final Object lock = new Object();

            @Override
            public Object lock() {
                // No synchronization with other tasks
                return lock;
            }
        };
        if(!TransferCollection.defaultCollection().contains(transfer)) {
            this.addTransfer(transfer, action);
        }
        else {
            this.background(action);
        }
    }

    private void validateToolbar() {
        invoke(new WindowMainAction(TransferController.this) {
            @Override
            public void run() {
                window().toolbar().validateVisibleItems();
                updateIcon();
            }
        });
    }

    private static final String TOOLBAR_RESUME = "Resume";
    private static final String TOOLBAR_RELOAD = "Reload";
    private static final String TOOLBAR_STOP = "Stop";
    private static final String TOOLBAR_REMOVE = "Remove";
    private static final String TOOLBAR_CLEAN_UP = "Clean Up";
    private static final String TOOLBAR_OPEN = "Open";
    private static final String TOOLBAR_SHOW = "Show";
    private static final String TOOLBAR_TRASH = "Trash";
    private static final String TOOLBAR_LOG = "Log";
    private static final String TOOLBAR_FILTER = "Search";

    /**
     * Keep reference to weak toolbar items
     */
    private Map<String, NSToolbarItem> toolbarItems
            = new HashMap<String, NSToolbarItem>();

    @Override
    public NSToolbarItem toolbar_itemForItemIdentifier_willBeInsertedIntoToolbar(NSToolbar toolbar, final String itemIdentifier, boolean flag) {
        if(!toolbarItems.containsKey(itemIdentifier)) {
            toolbarItems.put(itemIdentifier, NSToolbarItem.itemWithIdentifier(itemIdentifier));
        }
        final NSToolbarItem item = toolbarItems.get(itemIdentifier);
        if(itemIdentifier.equals(TOOLBAR_STOP)) {
            item.setLabel(Locale.localizedString(TOOLBAR_STOP));
            item.setPaletteLabel(Locale.localizedString(TOOLBAR_STOP));
            item.setToolTip(Locale.localizedString(TOOLBAR_STOP));
            item.setImage(IconCache.iconNamed("stop.tiff", 32));
            item.setTarget(this.id());
            item.setAction(Foundation.selector("stopButtonClicked:"));
            return item;
        }
        if(itemIdentifier.equals(TOOLBAR_RESUME)) {
            item.setLabel(Locale.localizedString(TOOLBAR_RESUME));
            item.setPaletteLabel(Locale.localizedString(TOOLBAR_RESUME));
            item.setToolTip(Locale.localizedString(TOOLBAR_RESUME));
            item.setImage(IconCache.iconNamed("resume.tiff"));
            item.setTarget(this.id());
            item.setAction(Foundation.selector("resumeButtonClicked:"));
            return item;
        }
        if(itemIdentifier.equals(TOOLBAR_RELOAD)) {
            item.setLabel(Locale.localizedString(TOOLBAR_RELOAD));
            item.setPaletteLabel(Locale.localizedString(TOOLBAR_RELOAD));
            item.setToolTip(Locale.localizedString(TOOLBAR_RELOAD));
            item.setImage(IconCache.iconNamed("reload.tiff"));
            item.setTarget(this.id());
            item.setAction(Foundation.selector("reloadButtonClicked:"));
            return item;
        }
        if(itemIdentifier.equals(TOOLBAR_SHOW)) {
            item.setLabel(Locale.localizedString(TOOLBAR_SHOW));
            item.setPaletteLabel(Locale.localizedString("Show in Finder"));
            item.setToolTip(Locale.localizedString("Show in Finder"));
            item.setImage(IconCache.iconNamed("reveal.tiff"));
            item.setTarget(this.id());
            item.setAction(Foundation.selector("revealButtonClicked:"));
            return item;
        }
        if(itemIdentifier.equals(TOOLBAR_OPEN)) {
            item.setLabel(Locale.localizedString(TOOLBAR_OPEN));
            item.setPaletteLabel(Locale.localizedString(TOOLBAR_OPEN));
            item.setToolTip(Locale.localizedString(TOOLBAR_OPEN));
            item.setImage(IconCache.iconNamed("open.tiff"));
            item.setTarget(this.id());
            item.setAction(Foundation.selector("openButtonClicked:"));
            return item;
        }
        if(itemIdentifier.equals(TOOLBAR_REMOVE)) {
            item.setLabel(Locale.localizedString(TOOLBAR_REMOVE));
            item.setPaletteLabel(Locale.localizedString(TOOLBAR_REMOVE));
            item.setToolTip(Locale.localizedString(TOOLBAR_REMOVE));
            item.setImage(IconCache.iconNamed("clean.tiff"));
            item.setTarget(this.id());
            item.setAction(Foundation.selector("deleteButtonClicked:"));
            return item;
        }
        if(itemIdentifier.equals(TOOLBAR_CLEAN_UP)) {
            item.setLabel(Locale.localizedString(TOOLBAR_CLEAN_UP));
            item.setPaletteLabel(Locale.localizedString(TOOLBAR_CLEAN_UP));
            item.setToolTip(Locale.localizedString(TOOLBAR_CLEAN_UP));
            item.setImage(IconCache.iconNamed("cleanall.tiff"));
            item.setTarget(this.id());
            item.setAction(Foundation.selector("clearButtonClicked:"));
            return item;
        }
        if(itemIdentifier.equals(TOOLBAR_TRASH)) {
            item.setLabel(Locale.localizedString(TOOLBAR_TRASH));
            item.setPaletteLabel(Locale.localizedString(TOOLBAR_TRASH));
            item.setToolTip(Locale.localizedString("Move to Trash"));
            item.setImage(IconCache.iconNamed("trash.tiff"));
            item.setTarget(this.id());
            item.setAction(Foundation.selector("trashButtonClicked:"));
            return item;
        }
        if(itemIdentifier.equals(TOOLBAR_LOG)) {
            item.setLabel(Locale.localizedString(TOOLBAR_LOG));
            item.setPaletteLabel(Locale.localizedString(TOOLBAR_LOG));
            item.setToolTip(Locale.localizedString("Toggle Log Drawer"));
            item.setImage(IconCache.iconNamed("log.tiff"));
            item.setTarget(this.id());
            item.setAction(Foundation.selector("toggleLogDrawer:"));
            return item;
        }
        if(itemIdentifier.equals(TOOLBAR_FILTER)) {
            item.setLabel(Locale.localizedString(TOOLBAR_FILTER));
            item.setPaletteLabel(Locale.localizedString(TOOLBAR_FILTER));
            item.setView(this.filterField);
            item.setMinSize(this.filterField.frame().size);
            item.setMaxSize(this.filterField.frame().size);
            return item;
        }
        // itemIdent refered to a toolbar item that is not provide or supported by us or cocoa.
        // Returning null will inform the toolbar this kind of item is not supported.
        return null;
    }

    @Action
    public void paste(final ID sender) {
        for(PathPasteboard pasteboard : PathPasteboard.allPasteboards()) {
            if(pasteboard.isEmpty()) {
                continue;
            }
            if(log.isDebugEnabled()) {
                log.debug("Paste download transfer from pasteboard");
            }
            final List<Path> downloads = pasteboard.copy();
            for(Path download : downloads) {
                download.setLocal(LocalFactory.createLocal(download.getHost().getDownloadFolder(), download.getName()));
            }
            this.addTransfer(new DownloadTransfer(downloads), new AbstractBackgroundAction() {
                @Override
                public void run() {
                    //
                }
            });
            pasteboard.clear();
        }
    }

    @Action
    public void stopButtonClicked(final ID sender) {
        NSIndexSet selected = transferTable.selectedRowIndexes();
        for(NSUInteger index = selected.firstIndex(); !index.equals(NSIndexSet.NSNotFound); index = selected.indexGreaterThanIndex(index)) {
            final Transfer transfer = transferTableModel.getSource().get(index.intValue());
            if(transfer.isRunning()) {
                this.background(new AbstractBackgroundAction<Void>() {
                    @Override
                    public void run() {
                        transfer.cancel();
                    }
                });
            }
        }
    }

    @Action
    public void stopAllButtonClicked(final ID sender) {
        final Collection<Transfer> transfers = transferTableModel.getSource();
        for(final Transfer transfer : transfers) {
            if(transfer.isRunning()) {
                this.background(new AbstractBackgroundAction<Void>() {
                    @Override
                    public void run() {
                        transfer.cancel();
                    }
                });
            }
        }
    }

    @Action
    public void resumeButtonClicked(final ID sender) {
        NSIndexSet selected = transferTable.selectedRowIndexes();
        for(NSUInteger index = selected.firstIndex(); !index.equals(NSIndexSet.NSNotFound); index = selected.indexGreaterThanIndex(index)) {
            final Collection<Transfer> transfers = transferTableModel.getSource();
            final Transfer transfer = transfers.get(index.intValue());
            if(!transfer.isRunning()) {
                this.startTransfer(transfer, true, false);
            }
        }
    }

    @Action
    public void reloadButtonClicked(final ID sender) {
        NSIndexSet selected = transferTable.selectedRowIndexes();
        for(NSUInteger index = selected.firstIndex(); !index.equals(NSIndexSet.NSNotFound); index = selected.indexGreaterThanIndex(index)) {
            final Collection<Transfer> transfers = transferTableModel.getSource();
            final Transfer transfer = transfers.get(index.intValue());
            if(!transfer.isRunning()) {
                this.startTransfer(transfer, false, true);
            }
        }
    }

    @Action
    public void openButtonClicked(final ID sender) {
        if(transferTable.numberOfSelectedRows().intValue() == 1) {
            final Transfer transfer = transferTableModel.getSource().get(transferTable.selectedRow().intValue());
            for(Path i : transfer.getRoots()) {
                ApplicationLauncherFactory.get().open(i.getLocal());
            }
        }
    }

    @Action
    public void revealButtonClicked(final ID sender) {
        NSIndexSet selected = transferTable.selectedRowIndexes();
        final Collection<Transfer> transfers = transferTableModel.getSource();
        for(NSUInteger index = selected.firstIndex(); !index.equals(NSIndexSet.NSNotFound); index = selected.indexGreaterThanIndex(index)) {
            final Transfer transfer = transfers.get(index.intValue());
            for(Path i : transfer.getRoots()) {
                reveal.reveal(i.getLocal());
            }
        }
    }

    @Action
    public void deleteButtonClicked(final ID sender) {
        NSIndexSet selected = transferTable.selectedRowIndexes();
        final Collection<Transfer> transfers = transferTableModel.getSource();
        int i = 0;
        final List<Transfer> remove = new ArrayList<Transfer>();
        for(NSUInteger index = selected.firstIndex(); !index.equals(NSIndexSet.NSNotFound); index = selected.indexGreaterThanIndex(index)) {
            final Transfer transfer = transfers.get(index.intValue() - i);
            if(!transfer.isRunning()) {
                remove.add(transfer);
            }
        }
        final TransferCollection collection = TransferCollection.defaultCollection();
        for(Transfer t : remove) {
            collection.remove(t);
        }
        collection.save();
    }

    @Action
    public void clearButtonClicked(final ID sender) {
        final TransferCollection collection = TransferCollection.defaultCollection();
        for(Iterator<Transfer> iter = collection.iterator(); iter.hasNext(); ) {
            Transfer transfer = iter.next();
            if(!transfer.isRunning() && transfer.isComplete()) {
                iter.remove();
            }
        }
        collection.save();
    }

    @Action
    public void trashButtonClicked(final ID sender) {
        NSIndexSet selected = transferTable.selectedRowIndexes();
        final Collection<Transfer> transfers = transferTableModel.getSource();
        for(NSUInteger index = selected.firstIndex(); !index.equals(NSIndexSet.NSNotFound); index = selected.indexGreaterThanIndex(index)) {
            final Transfer transfer = transfers.get(index.intValue());
            if(!transfer.isRunning()) {
                for(Path i : transfer.getRoots()) {
                    i.getLocal().trash();
                }
            }
        }
        this.updateIcon();
    }

    @Action
    public void printDocument(final ID sender) {
        this.print(transferTable);
    }

    /**
     * NSToolbar.Delegate
     *
     * @param toolbar Window toolbar
     */
    @Override
    public NSArray toolbarDefaultItemIdentifiers(NSToolbar toolbar) {
        return NSArray.arrayWithObjects(
                TOOLBAR_RESUME,
                TOOLBAR_STOP,
                TOOLBAR_RELOAD,
                TOOLBAR_REMOVE,
                TOOLBAR_SHOW,
                NSToolbarItem.NSToolbarFlexibleItemIdentifier,
                TOOLBAR_FILTER
        );
    }

    /**
     * NSToolbar.Delegate
     *
     * @param toolbar Window toolbar
     */
    @Override
    public NSArray toolbarAllowedItemIdentifiers(NSToolbar toolbar) {
        return NSArray.arrayWithObjects(
                TOOLBAR_RESUME,
                TOOLBAR_RELOAD,
                TOOLBAR_STOP,
                TOOLBAR_REMOVE,
                TOOLBAR_CLEAN_UP,
                TOOLBAR_SHOW,
                TOOLBAR_OPEN,
                TOOLBAR_TRASH,
                TOOLBAR_FILTER,
                TOOLBAR_LOG,
                NSToolbarItem.NSToolbarCustomizeToolbarItemIdentifier,
                NSToolbarItem.NSToolbarSpaceItemIdentifier,
                NSToolbarItem.NSToolbarSeparatorItemIdentifier,
                NSToolbarItem.NSToolbarFlexibleSpaceItemIdentifier
        );
    }

    @Override
    public NSArray toolbarSelectableItemIdentifiers(NSToolbar toolbar) {
        return NSArray.array();
    }

    /**
     * @param item Menu item
     * @return True if enabled
     */
    public boolean validateMenuItem(NSMenuItem item) {
        final Selector action = item.action();
        if(action.equals(Foundation.selector("paste:"))) {
            final List<PathPasteboard> pasteboards = PathPasteboard.allPasteboards();
            if(pasteboards.size() == 1) {
                for(PathPasteboard pasteboard : pasteboards) {
                    if(pasteboard.size() == 1) {
                        item.setTitle(MessageFormat.format(Locale.localizedString("Paste {0}"), pasteboard.get(0).getName()));
                    }
                    else {
                        item.setTitle(MessageFormat.format(Locale.localizedString("Paste {0}"),
                                MessageFormat.format(Locale.localizedString("{0} Files"), String.valueOf(pasteboard.size())) + ")"));
                    }
                }
            }
            else {
                item.setTitle(Locale.localizedString("Paste"));
            }
        }
        return this.validateItem(action);
    }

    /**
     * @param item Toolbar item
     */
    @Override
    public boolean validateToolbarItem(final NSToolbarItem item) {
        return this.validateItem(item.action());
    }

    /**
     * Validates menu and toolbar items
     *
     * @param action Method target
     * @return true if the item with the identifier should be selectable
     */
    private boolean validateItem(final Selector action) {
        if(action.equals(Foundation.selector("paste:"))) {
            return !PathPasteboard.allPasteboards().isEmpty();
        }
        if(action.equals(Foundation.selector("stopButtonClicked:"))) {
            return this.validate(new TransferToolbarValidator() {
                @Override
                public boolean validate(Transfer transfer) {
                    return transfer.isRunning();
                }
            });
        }
        if(action.equals(Foundation.selector("reloadButtonClicked:"))) {
            return this.validate(new TransferToolbarValidator() {
                @Override
                public boolean validate(Transfer transfer) {
                    return transfer.isReloadable() && !transfer.isRunning();
                }
            });
        }
        if(action.equals(Foundation.selector("deleteButtonClicked:"))) {
            return this.validate(new TransferToolbarValidator() {
                @Override
                public boolean validate(Transfer transfer) {
                    return !transfer.isRunning();
                }
            });
        }
        if(action.equals(Foundation.selector("resumeButtonClicked:"))) {
            return this.validate(new TransferToolbarValidator() {
                @Override
                public boolean validate(Transfer transfer) {
                    if(transfer.isRunning()) {
                        return false;
                    }
                    return transfer.isResumable() && !transfer.isComplete();
                }
            });
        }
        if(action.equals(Foundation.selector("openButtonClicked:"))
                || action.equals(Foundation.selector("trashButtonClicked:"))) {
            return this.validate(new TransferToolbarValidator() {
                @Override
                public boolean validate(Transfer transfer) {
                    if(transfer.getLocal() != null) {
                        if(!transfer.isComplete()) {
                            return false;
                        }
                        if(!transfer.isRunning()) {
                            for(Path i : transfer.getRoots()) {
                                if(i.getLocal().exists()) {
                                    return true;
                                }
                            }
                        }
                    }
                    return false;
                }
            });
        }
        if(action.equals(Foundation.selector("revealButtonClicked:"))) {
            return this.validate(new TransferToolbarValidator() {
                @Override
                public boolean validate(Transfer transfer) {
                    if(transfer.getLocal() != null) {
                        for(Path i : transfer.getRoots()) {
                            if(i.getLocal().exists()) {
                                return true;
                            }
                        }
                    }
                    return false;
                }
            });
        }
        if(action.equals(Foundation.selector("clearButtonClicked:"))) {
            return transferTable.numberOfRows().intValue() > 0;
        }
        return true;
    }

    /**
     * Validates the selected items in the transfer window against the toolbar validator
     *
     * @param v The validator to use
     * @return True if one or more of the selected items passes the validation test
     */
    private boolean validate(final TransferToolbarValidator v) {
        final NSIndexSet iterator = transferTable.selectedRowIndexes();
        final Collection<Transfer> transfers = transferTableModel.getSource();
        for(NSUInteger index = iterator.firstIndex(); !index.equals(NSIndexSet.NSNotFound); index = iterator.indexGreaterThanIndex(index)) {
            final Transfer transfer = transfers.get(index.intValue());
            if(v.validate(transfer)) {
                return true;
            }
        }
        return false;
    }

    private interface TransferToolbarValidator {
        boolean validate(Transfer transfer);
    }
}
