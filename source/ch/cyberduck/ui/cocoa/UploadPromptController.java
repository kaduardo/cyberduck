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

import ch.cyberduck.core.transfer.Transfer;
import ch.cyberduck.core.transfer.TransferAction;

/**
 * @version $Id: UploadPromptController.java 10164 2012-10-15 14:48:13Z dkocher $
 */
public class UploadPromptController extends TransferPromptController {

    public UploadPromptController(final WindowController parent, final Transfer transfer) {
        super(parent, transfer);
    }

    @Override
    public TransferAction prompt() {
        browserModel = new UploadPromptModel(this, transfer);
        return super.prompt();
    }
}