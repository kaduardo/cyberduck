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

import ch.cyberduck.core.Session;
import ch.cyberduck.core.TranscriptListener;
import ch.cyberduck.core.ftp.FTPException;
import ch.cyberduck.ui.cocoa.application.NSButton;
import ch.cyberduck.ui.cocoa.application.NSImageView;
import ch.cyberduck.ui.cocoa.application.NSLayoutManager;
import ch.cyberduck.ui.cocoa.application.NSProgressIndicator;
import ch.cyberduck.ui.cocoa.application.NSTextField;
import ch.cyberduck.ui.cocoa.application.NSTextView;
import ch.cyberduck.ui.cocoa.foundation.NSAttributedString;
import ch.cyberduck.ui.cocoa.foundation.NSObject;
import ch.cyberduck.ui.cocoa.foundation.NSRange;
import ch.cyberduck.ui.cocoa.resources.IconCache;
import ch.cyberduck.ui.cocoa.threading.BrowserBackgroundAction;
import ch.cyberduck.ui.cocoa.threading.WindowMainAction;

import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.rococoa.cocoa.foundation.NSUInteger;

import java.io.IOException;

/**
 * @version $Id: CommandController.java 9843 2012-10-04 12:55:36Z dkocher $
 */
public class CommandController extends SheetController implements TranscriptListener, NSLayoutManager.Delegate {
    private static Logger log = Logger.getLogger(CommandController.class);

    @Outlet
    private NSTextField inputField;
    @Outlet
    private NSTextView responseField;
    @Outlet
    private NSProgressIndicator progress;
    @Outlet
    private NSImageView image;

    public void setInputField(NSTextField inputField) {
        this.inputField = inputField;
    }

    public void setResponseField(NSTextView responseField) {
        this.responseField = responseField;
        this.responseField.setEditable(false);
        this.responseField.setSelectable(true);
        this.responseField.setUsesFontPanel(false);
        this.responseField.setRichText(false);
        this.responseField.layoutManager().setDelegate(this.id());
    }

    public void setProgress(NSProgressIndicator progress) {
        this.progress = progress;
        this.progress.setDisplayedWhenStopped(false);
    }

    public void setImage(NSImageView image) {
        this.image = image;
        this.image.setImage(IconCache.instance().iconForApplication("com.apple.Terminal", 128));
    }

    @Override
    public void layoutManager_didCompleteLayoutForTextContainer_atEnd(NSLayoutManager layoutManager,
                                                                      NSObject textContainer,
                                                                      boolean finished) {
        if(finished && this.responseField.window().isVisible()) {
            this.responseField.scrollRangeToVisible(
                    NSRange.NSMakeRange(this.responseField.textStorage().length(), new NSUInteger(0))
            );
        }
    }

    private Session session;

    public CommandController(final WindowController parent, final Session session) {
        super(parent);
        this.session = session;
        this.session.addTranscriptListener(this);
    }

    @Override
    protected String getBundleName() {
        return "Command";
    }

    @Action
    public void sendButtonClicked(final NSButton sender) {
        final String command = this.inputField.stringValue();
        if(StringUtils.isNotBlank(command)) {
            progress.startAnimation(null);
            sender.setEnabled(false);
            parent.background(new BrowserBackgroundAction((BrowserController) parent) {
                boolean close;

                @Override
                public void run() {
                    try {
                        session.sendCommand(command);
                    }
                    catch(FTPException e) {
                        ; //ignore
                    }
                    catch(IOException e) {
                        log.warn(e.getMessage());
                    }
                }

                @Override
                public void cleanup() {
                    progress.stopAnimation(null);
                    sender.setEnabled(true);
                    if(close) {
                        closeSheet(sender);
                    }
                }

                @Override
                public String getActivity() {
                    return command;
                }
            });
        }
    }

    @Override
    public void log(boolean request, final String message) {
        invoke(new WindowMainAction(this) {
            @Override
            public void run() {
                responseField.textStorage().replaceCharactersInRange_withAttributedString(
                        NSRange.NSMakeRange(responseField.textStorage().length(), new NSUInteger(0)),
                        NSAttributedString.attributedStringWithAttributes(message + "\n", FIXED_WITH_FONT_ATTRIBUTES));
            }
        });
    }

    @Override
    protected boolean validateInput() {
        return true;
    }

    @Override
    public void callback(final int returncode) {
        ;
    }

    @Override
    protected void invalidate() {
        session.removeTranscriptListener(this);
        responseField.layoutManager().setDelegate(null);
        super.invalidate();
    }
}