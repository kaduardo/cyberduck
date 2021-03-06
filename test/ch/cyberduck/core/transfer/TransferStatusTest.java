package ch.cyberduck.core.transfer;

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

import ch.cyberduck.core.AbstractTestCase;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class TransferStatusTest {

    @Test
    public void testSetResume() throws Exception {
        TransferStatus status = new TransferStatus();
        status.setCurrent(1024);
        status.setResume(true);
        assertEquals(1024, status.getCurrent());
        status.setResume(false);
        assertEquals(0, status.getCurrent());
    }

    @Test
    public void testSetComplete() throws Exception {
        TransferStatus status = new TransferStatus();
        status.setComplete();
        assertTrue(status.isComplete());
    }
}