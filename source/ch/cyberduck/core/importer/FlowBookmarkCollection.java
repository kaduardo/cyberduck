package ch.cyberduck.core.importer;

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

import ch.cyberduck.core.Host;
import ch.cyberduck.core.Preferences;
import ch.cyberduck.core.Protocol;
import ch.cyberduck.core.ftp.FTPConnectMode;
import ch.cyberduck.core.local.Local;
import ch.cyberduck.core.local.LocalFactory;
import ch.cyberduck.core.serializer.impl.PlistDeserializer;
import ch.cyberduck.ui.cocoa.foundation.NSDictionary;

import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;

import java.util.List;

/**
 * @version $Id: FlowBookmarkCollection.java 10226 2012-10-15 17:30:25Z dkocher $
 */
public class FlowBookmarkCollection extends ThirdpartyBookmarkCollection {
    private static Logger log = Logger.getLogger(FlowBookmarkCollection.class);

    private static final long serialVersionUID = 2017398431454618548L;

    @Override
    public String getBundleIdentifier() {
        return "com.extendmac.Flow";
    }

    @Override
    public Local getFile() {
        return LocalFactory.createLocal(Preferences.instance().getProperty("bookmark.import.flow.location"));
    }

    @Override
    protected void parse(Local file) {
        NSDictionary serialized = NSDictionary.dictionaryWithContentsOfFile(file.getAbsolute());
        if(null == serialized) {
            log.error("Invalid bookmark file:" + file);
            return;
        }
        this.parse(serialized);
    }

    private void parse(NSDictionary serialized) {
        List<NSDictionary> items = new PlistDeserializer(serialized).listForKey("BookmarkItems");
        for(NSDictionary item : items) {
            final PlistDeserializer bookmark = new PlistDeserializer(item);
            String classname = bookmark.stringForKey("ClassName");
            if(null == classname) {
                continue;
            }
            if("Bookmark".equals(classname)) {
                String server = bookmark.stringForKey("Server");
                if(null == server) {
                    continue;
                }
                Host host = new Host(server);
                String port = bookmark.stringForKey("Port");
                if(StringUtils.isNotBlank(port)) {
                    host.setPort(Integer.parseInt(port));
                }
                String path = bookmark.stringForKey("InitialPath");
                if(StringUtils.isNotBlank(path)) {
                    host.setDefaultPath(path);
                }
                String name = bookmark.stringForKey("Name");
                if(StringUtils.isNotBlank(name)) {
                    host.setNickname(name);
                }
                String user = bookmark.stringForKey("Username");
                if(StringUtils.isNotBlank(user)) {
                    host.getCredentials().setUsername(user);
                }
                else {
                    host.getCredentials().setUsername(
                            Preferences.instance().getProperty("connection.login.anon.name"));
                }
                String mode = bookmark.stringForKey("PreferredFTPDataConnectionType");
                if(StringUtils.isNotBlank(mode)) {
                    if("Passive".equals(mode)) {
                        host.setFTPConnectMode(FTPConnectMode.PASV);
                    }
                    if("Active".equals(mode)) {
                        host.setFTPConnectMode(FTPConnectMode.PORT);
                    }
                }
                String protocol = bookmark.stringForKey("Protocol");
                if(StringUtils.isNotBlank(protocol)) {
                    try {
                        switch(Integer.parseInt(protocol)) {
                            case 0:
                                host.setProtocol(Protocol.FTP);
                                break;
                            case 1:
                                host.setProtocol(Protocol.SFTP);
                                break;
                            case 3:
                                host.setProtocol(Protocol.S3_SSL);
                                break;
                            case 2:
                            case 4:
                                if(host.getPort() == Protocol.WEBDAV_SSL.getDefaultPort()) {
                                    host.setProtocol(Protocol.WEBDAV_SSL);
                                }
                                else {
                                    host.setProtocol(Protocol.WEBDAV);
                                }
                                break;
                        }
                    }
                    catch(NumberFormatException e) {
                        log.warn("Unknown protocol:" + e.getMessage());
                    }
                }
                this.add(host);
            }
            if("BookmarkFolder".equals(classname)) {
                this.parse(item);
            }
        }
    }
}