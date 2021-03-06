package ch.cyberduck.ui.cocoa.quicklook;

/*
 * Copyright (c) 2002-2009 David Kocher. All rights reserved.
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

import ch.cyberduck.core.local.Local;
import ch.cyberduck.ui.cocoa.foundation.NSURL;

import org.rococoa.ID;
import org.rococoa.cocoa.foundation.NSInteger;

import java.util.ArrayList;
import java.util.List;

/**
 * @version $Id: QuartzQuickLook.java 10530 2012-10-22 12:03:11Z dkocher $
 */
public final class QuartzQuickLook implements QuickLook {

    public static void register() {
        if(!Factory.VERSION_PLATFORM.matches("10\\.5.*")) {
            QuickLookFactory.addFactory(Factory.VERSION_PLATFORM, new Factory());
        }
    }

    private static class Factory extends QuickLookFactory {
        @Override
        protected QuickLook create() {
            return new QuartzQuickLook();
        }
    }

    private List<QLPreviewItem> previews = new ArrayList<QLPreviewItem>();

    private final QLPreviewPanel panel;

    private QuartzQuickLook() {
        panel = QLPreviewPanel.sharedPreviewPanel();
    }

    @Override
    public void select(final List<Local> files) {
        previews.clear();
        for(final Local selected : files) {
            previews.add(new QLPreviewItem() {
                @Override
                public NSURL previewItemURL() {
                    return NSURL.fileURLWithPath(selected.getAbsolute());
                }

                @Override
                public String previewItemTitle() {
                    return selected.getDisplayName();
                }
            });
        }
    }

    private QLPreviewPanelDataSource model = new QLPreviewPanelDataSource() {
        @Override
        public NSInteger numberOfPreviewItemsInPreviewPanel(QLPreviewPanel panel) {
            return new NSInteger(previews.size());
        }

        @Override
        public ID previewPanel_previewItemAtIndex(QLPreviewPanel panel, final int index) {
            return previews.get(index).id();
        }
    };

    @Override
    public boolean isAvailable() {
        return null != panel;
    }

    @Override
    public boolean isOpen() {
        return QLPreviewPanel.sharedPreviewPanelExists() && panel.isVisible();
    }

    @Override
    public void willBeginQuickLook() {
        panel.setDataSource(this.model.id());
    }

    @Override
    public void open() {
        panel.makeKeyAndOrderFront(null);
        if(null == panel.dataSource()) {
            // Do not reload data yet because datasource is not yet setup.
            // Focus has probably changed to another application since
            return;
        }
        panel.reloadData();
    }

    @Override
    public void close() {
        panel.orderOut(null);
    }

    @Override
    public void didEndQuickLook() {
        panel.setDataSource(null);
    }
}