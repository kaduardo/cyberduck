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

import ch.cyberduck.ui.cocoa.application.NSEvent;
import ch.cyberduck.ui.cocoa.application.NSImage;

import org.rococoa.cocoa.foundation.NSRect;

/**
 * This file was autogenerated by <a href="http://jnaerator.googlecode.com/">JNAerator</a>,<br>
 * a tool written by <a href="http://ochafik.free.fr/">Olivier Chafik</a> that <a href="http://code.google.com/p/jnaerator/wiki/CreditsAndLicense">uses a few opensource projects.</a>.<br>
 * For help, please visit <a href="http://nativelibs4java.googlecode.com/">NativeLibs4Java</a>, <a href="http://rococoa.dev.java.net/">Rococoa</a>, or <a href="http://jna.dev.java.net/">JNA</a>.
 *
 * @version $Id: QLPreviewPanelDelegate.java 5258 2009-09-12 12:26:28Z dkocher $
 */
public interface QLPreviewPanelDelegate {
    /**
     * @abstract Invoked by the preview panel when it receives an event it doesn't handle.<br>
     * @result Returns NO if the receiver did not handle the event.<br>
     * Original signature : <code>-(BOOL)previewPanel:(QLPreviewPanel*) handleEvent:(NSEvent*)</code><br>
     * <i>native declaration : line 9</i>
     */
    public boolean previewPanel_handleEvent(QLPreviewPanel panel, NSEvent event);

    /**
     * @ Invoked when the preview panel opens or closes to provide a zoom effect.<br>
     * @discussion Return NSZeroRect if there is no origin point, this will produce a fade of the panel. The coordinates are screen based.<br>
     * Original signature : <code>-(id)previewPanel:(QLPreviewPanel*) sourceFrameOnScreenForPreviewItem:(id<QLPreviewItem>)</code><br>
     * <i>native declaration : line 19</i>
     */
    public NSRect previewPanel_sourceFrameOnScreenForPreviewItem(QLPreviewPanel panel, QLPreviewItem item);

    /**
     * @param contentRect The rect within the image that actually represents the content of the document. For example, for icons the actual rect is generally smaller than the icon itself.<br>
     * @ Invoked when the preview panel opens or closes to provide a smooth transition when zooming.<br>
     * @discussion Return an image the panel will crossfade with when opening or closing. You can specify the actual "document" content rect in the image in contentRect.<br>
     * Original signature : <code>-(id)previewPanel:(QLPreviewPanel*) transitionImageForPreviewItem:(id<QLPreviewItem>) contentRect:(NSRect*)</code><br>
     * <i>native declaration : line 26</i>
     */
    public NSImage previewPanel_transitionImageForPreviewItem_contentRect(QLPreviewPanel panel, QLPreviewItem item, NSRect contentRect);
}
