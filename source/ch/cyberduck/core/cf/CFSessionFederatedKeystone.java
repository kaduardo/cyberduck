package ch.cyberduck.core.cf;

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
import ch.cyberduck.core.*;
import ch.cyberduck.core.ConnectionCanceledException;
import ch.cyberduck.core.Credentials;
import ch.cyberduck.core.Host;
import ch.cyberduck.core.LoginController;
import ch.cyberduck.core.Path;
import ch.cyberduck.core.Preferences;
import ch.cyberduck.core.Protocol;
import ch.cyberduck.core.cdn.Distribution;
import ch.cyberduck.core.cdn.DistributionConfiguration;
import ch.cyberduck.core.cloud.CloudSession;
import ch.cyberduck.core.i18n.Locale;
import ch.cyberduck.core.identity.DefaultCredentialsIdentityConfiguration;
import ch.cyberduck.core.identity.IdentityConfiguration;

import org.apache.commons.lang.StringUtils;
import org.apache.http.HttpException;
import org.apache.log4j.Logger;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.text.MessageFormat;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.ArrayList;
import java.util.Map;
import java.util.LinkedList;

import com.rackspacecloud.client.cloudfiles.FilesCDNContainer;
import com.rackspacecloud.client.cloudfiles.FilesClientFederatedKeystone;
import com.rackspacecloud.client.cloudfiles.FilesContainerMetaData;
import com.rackspacecloud.client.cloudfiles.FilesException;

/**
 * Rackspace Cloud Files Implementation
 *
 * @version $Id: CFSessionKeystone.java 10823 2013-04-08 17:31:39Z dkocher $
 */
public class CFSessionFederatedKeystone extends CFSessionKeystone implements DistributionConfiguration {
    private static final Logger log = Logger.getLogger(CFSessionFederatedKeystone.class);

    private FilesClientFederatedKeystone client;

    public CFSessionFederatedKeystone(Host h) {
        super(h);
    }

    @Override
    protected FilesClientFederatedKeystone getClient() throws ConnectionCanceledException {
        if(null == client) {
            throw new ConnectionCanceledException();
        }
        return client;
    }

    @Override
    protected void connect() throws IOException {
        if(this.isConnected()) {
            return;
        }
      
		this.client = new FilesClientFederatedKeystone(this.http(), null, null, null, null, this.timeout());
        this.fireConnectionWillOpenEvent();

        // Configure for authentication URL
        this.configure();

		
		
        // Prompt the login credentials first
        this.login();

        this.fireConnectionDidOpenEvent();
    }

    /**
     * Set connection properties
     *
     * @throws java.io.IOException If the connection is already canceled
     */
	
    protected void configure() throws IOException {
        final FilesClientFederatedKeystone c = this.getClient();
        c.setConnectionTimeOut(this.timeout());
        c.setUserAgent(this.getUserAgent());
        // Do not calculate ETag in advance
        c.setUseETag(false);
        c.setAuthenticationURL(this.getAuthenticationUrl());
    }
	
	private String getAuthenticationUrl() {
        final StringBuilder authentication = new StringBuilder();
        authentication.append(host.getProtocol().getScheme().toString()).append("://");
        if(host.getHostname().equals("storage.clouddrive.com")) {
            // Legacy bookmarks. Use default authentication server for Rackspace.
            authentication.append("auth.api.rackspacecloud.com");
        }
        else {
            // Use custom authentication server. Swift (OpenStack Object Storage) installation.
            authentication.append(host.getHostname());
        }
        authentication.append(":").append(host.getPort());
        if(StringUtils.isBlank(host.getProtocol().getContext())) {
            authentication.append(Path.normalize(Preferences.instance().getProperty("cf.authentication.context")));
        }
        else {
            authentication.append(Path.normalize(host.getProtocol().getContext()));
        }
        if(log.isInfoEnabled()) {
            log.info(String.format("Using authentication URL %s", authentication.toString()));
        }
        return authentication.toString();
    }

    @Override
    protected void login(final LoginController controller, final Credentials credentials) throws IOException {
        final FilesClientFederatedKeystone client = this.getClient();
        client.setUserName(credentials.getUsername());
        client.setPassword(credentials.getPassword());
		try{
			this.message(Locale.localizedString("Getting IdP List", "Credentials"));
                
			List<String> realms = (LinkedList<String>) client.getRealmList();
			//call dialog
			String selectedRealm = controller.prompt("IdP Server","Choose a IdP server:","Servers",realms);
			if(selectedRealm.equals("")){
				return;
			}
			
			this.message(Locale.localizedString("Connecting to" + selectedRealm , "Credentials"));
            
			String[] idpRequest = client.getIdPRequest(selectedRealm); 
			
			controller.prompt(idpRequest[0] + "-------" + idpRequest[1] );
			
			
			String idpResponse = client.getIdPResponse(idpRequest[0],idpRequest[1]);
		
			
			controller.prompt(idpResponse);
		
			String unscopedTokenJson = client.getUnscopedToken(idpResponse,selectedRealm);
	
			
			controller.prompt(unscopedTokenJson);
	
	
			this.message(Locale.localizedString("Getting Tenant List" , "Credentials"));
            
			List<String> tenants = (LinkedList<String>) client.getTenantsName();
			
			//call dialog
			String selectedTenant = controller.prompt("Tenants","Choose a Tenant:","Tenants",tenants);
			if(selectedTenant.equals("")){
				return;
			}
			controller.prompt(selectedTenant);
			
			String scopedToken =  client.getScopedToken(client.getUnscopedToken(),selectedTenant);
			
			controller.prompt("Scopedtoken:  " + scopedToken);
			
		}
		catch(Exception e){
			this.message(Locale.localizedString("ERROR: " + e.getMessage() , "Credentials"));
            
			controller.prompt(e.getMessage());
			
			IOException failure = new IOException(e.getMessage());
            failure.initCause(e);
            throw failure;
		}
		
	

		try {
            if(!client.login()) {
                this.message(Locale.localizedString("Login failed", "Credentials"));
                controller.fail(host.getProtocol(), credentials);
                this.login();
            }
        }//
        catch(HttpException e) {
            IOException failure = new IOException(e.getMessage());
            failure.initCause(e);
            throw failure;
        }
    }
/*
	private void naoesqucer(){
		LoginController login = LoginControllerFactory.get(this);
		
		List<String> lista = new ArrayList<String>();
		lista.add("ufrn");
		lista.add("ufcg");
		lista.add("ufrj");
		
		String retorno = login.prompt("IDP Server","choose:","Server",lista);
		
		login.prompt(retorno);


	}*/
}