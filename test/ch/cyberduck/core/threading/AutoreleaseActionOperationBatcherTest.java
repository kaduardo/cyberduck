package ch.cyberduck.core.threading;

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

import ch.cyberduck.core.AbstractTestCase;

import org.junit.BeforeClass;
import org.junit.Test;

import java.util.concurrent.Callable;

/**
 * @version $Id: AutoreleaseActionOperationBatcherTest.java 10394 2012-10-18 08:50:31Z dkocher $
 */
public class AutoreleaseActionOperationBatcherTest extends AbstractTestCase {

    @BeforeClass
    public static void register() {
        AutoreleaseActionOperationBatcher.register();
    }

    @Test
    public void testOperate() throws Exception {
        this.repeat(new Callable<Object>() {
            @Override
            public Object call() throws Exception {
                ActionOperationBatcherFactory.get().operate();
                return null;
            }
        }, 20);
    }
}
