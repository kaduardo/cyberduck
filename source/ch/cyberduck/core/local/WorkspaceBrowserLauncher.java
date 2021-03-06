package ch.cyberduck.core.local;

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

import ch.cyberduck.ui.cocoa.application.AppKitFunctions;
import ch.cyberduck.ui.cocoa.application.NSWorkspace;
import ch.cyberduck.ui.cocoa.foundation.NSURL;

import org.apache.commons.lang.StringUtils;

/**
 * @version $Id: WorkspaceBrowserLauncher.java 10445 2012-10-18 17:20:09Z dkocher $
 */
public class WorkspaceBrowserLauncher implements BrowserLauncher {

    public static void register() {
        BrowserLauncherFactory.addFactory(Factory.NATIVE_PLATFORM, new Factory());
    }

    private static class Factory extends BrowserLauncherFactory {
        @Override
        protected BrowserLauncher create() {
            return new WorkspaceBrowserLauncher();
        }
    }

    @Override
    public void open(String url) {
        if(StringUtils.isNotBlank(url)) {
            if(!NSWorkspace.sharedWorkspace().openURL(NSURL.URLWithString(url))) {
                AppKitFunctions.instance.NSBeep();
            }
        }
        else {
            AppKitFunctions.instance.NSBeep();
        }
    }
}