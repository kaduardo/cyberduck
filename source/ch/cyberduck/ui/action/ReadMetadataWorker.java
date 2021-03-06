package ch.cyberduck.ui.action;

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

import ch.cyberduck.core.Path;
import ch.cyberduck.core.i18n.Locale;

import org.apache.log4j.Logger;

import java.text.MessageFormat;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @version $Id: ReadMetadataWorker.java 10069 2012-10-11 20:10:33Z dkocher $
 */
public abstract class ReadMetadataWorker extends Worker<Map<String, String>> {
    private static Logger log = Logger.getLogger(ReadMetadataWorker.class);

    /**
     * Selected files.
     */
    private List<Path> files;

    public ReadMetadataWorker(final List<Path> files) {
        this.files = files;
    }

    /**
     * @return Metadata
     */
    @Override
    public Map<String, String> run() {
        final Map<String, Integer> count = new HashMap<String, Integer>();
        final Map<String, String> updated = new HashMap<String, String>() {
            @Override
            public String put(String key, String value) {
                int n = 0;
                if(count.containsKey(key)) {
                    n = count.get(key);
                }
                count.put(key, ++n);
                return super.put(key, value);
            }
        };
        for(Path next : files) {
            // Reading HTTP headers custom metadata
            if(next.attributes().getMetadata().isEmpty()) {
                next.readMetadata();
            }
            final Map<String, String> metadata = next.attributes().getMetadata();
            for(Map.Entry<String, String> entry : metadata.entrySet()) {
                // Prune metadata from entries which are unique to a single file.
                // For example md5-hash
                if(updated.containsKey(entry.getKey())) {
                    if(!entry.getValue().equals(updated.get(entry.getKey()))) {
                        log.info(String.format("Nullify %s from metadata because value is not equal for selected files.", entry));
                        updated.put(entry.getKey(), null);
                        continue;
                    }
                }
                updated.put(entry.getKey(), entry.getValue());
            }
        }
        for(Map.Entry<String, Integer> entry : count.entrySet()) {
            if(files.size() == entry.getValue()) {
                // Every file has this metadata set.
                continue;
            }
            // Not all files selected have this metadata. Remove for editing.
            log.info(String.format("Remove %s from metadata not available for all selected files.", entry.getKey()));
            updated.remove(entry.getKey());
        }
        return updated;
    }

    @Override
    public String getActivity() {
        return MessageFormat.format(Locale.localizedString("Reading metadata of {0}", "Status"),
                this.toString(files));
    }
}