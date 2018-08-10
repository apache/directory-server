/*
 *  Licensed to the Apache Software Foundation (ASF) under one
 *  or more contributor license agreements.  See the NOTICE file
 *  distributed with this work for additional information
 *  regarding copyright ownership.  The ASF licenses this file
 *  to you under the Apache License, Version 2.0 (the
 *  "License"); you may not use this file except in compliance
 *  with the License.  You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing,
 *  software distributed under the License is distributed on an
 *  "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 *  KIND, either express or implied.  See the License for the
 *  specific language governing permissions and limitations
 *  under the License.
 *
 */
package org.apache.directory.kerberos.client;

import java.io.File;

import org.apache.directory.kerberos.credentials.cache.Credentials;
import org.apache.directory.kerberos.credentials.cache.CredentialsCache;
import org.apache.directory.shared.kerberos.codec.types.PrincipalNameType;
import org.apache.directory.shared.kerberos.components.PrincipalName;

/**
 * Authenticates to the Kerberos server and gets the initial Ticket Granting Ticket,
 * then cache the tgt in credentials cache, as MIT kinit does.
 * 
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 */
public class Kinit
{
    private KdcConnection kdc;
    private File credCacheFile;
    
    public Kinit( KdcConnection kdc )
    {
    	this.kdc = kdc;
    }
    
    public void setCredCacheFile( File credCacheFile )
    {
    	this.credCacheFile = credCacheFile;
    }
    
    public File getCredCacheFile()
    {
    	return this.credCacheFile;
    }
    
    /**
     * Authenticates to the Kerberos server and gets the initial Ticket Granting Ticket,
     * then cache the tgt in credentials cache, as MIT kinit does.
     * 
     * @param principal the client's principal 
     * @param password password of the client
     * @throws Exception If we had an issue while getting the TGT, or creating the PrincipalName, or
     * storing the credentials
     */
    public void kinit( String principal, String password ) throws Exception
    {
        if ( principal == null || password == null || credCacheFile == null )
        {
        	throw new IllegalArgumentException( "Invalid principal, password, or credentials cache file" );
        }
        
        TgTicket tgt = kdc.getTgt( principal, password );
        
        CredentialsCache credCache = new CredentialsCache();
        
        PrincipalName princ = new PrincipalName( principal, PrincipalNameType.KRB_NT_PRINCIPAL );
        princ.setRealm( tgt.getRealm() );
        credCache.setPrimaryPrincipalName( princ );
        
        Credentials cred = new Credentials( tgt );
        credCache.addCredentials( cred );
        
        CredentialsCache.store( credCacheFile, credCache );
    }
}
