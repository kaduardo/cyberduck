package ch.cyberduck.core.threading;

/*
 *  Copyright (c) 2008 David Kocher. All rights reserved.
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
 * @version $Id: BackgroundActionRegistry.java 10395 2012-10-18 09:02:22Z dkocher $
 */
public class BackgroundActionRegistry extends AbstractActionRegistry<BackgroundAction> {
    private static final long serialVersionUID = 1721336643608575003L;

    private static BackgroundActionRegistry global = null;

    private static final Object lock = new Object();

    public static BackgroundActionRegistry global() {
        synchronized(lock) {
            if(null == global) {
                global = new BackgroundActionRegistry();
            }
            return global;
        }
    }

    private BackgroundAction current;

    /**
     * @return The currently running background action. Null if none is currently running.
     */
    public BackgroundAction getCurrent() {
        return current;
    }

    /**
     * Actions added are automatically removed when canceled or stopped.
     *
     * @param action Action to run in background
     * @return True
     */
    @Override
    public boolean add(final BackgroundAction action) {
        action.addListener(new BackgroundActionListener() {
            public void start(final BackgroundAction action) {
                current = action;
            }

            public void cancel(final BackgroundAction action) {
                remove(action);
            }

            public void stop(final BackgroundAction action) {
                current = null;
                action.removeListener(this);
                remove(action);
            }
        });
        return super.add(action);
    }

    public BackgroundActionRegistry() {
        //
    }
}
