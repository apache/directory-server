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
package org.apache.directory.server.core.authn;


import org.apache.directory.server.core.DirectoryService;
import org.apache.directory.shared.ldap.constants.AuthenticationLevel;
import org.apache.directory.shared.ldap.exception.LdapException;
import org.apache.directory.shared.ldap.name.DN;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * Base class for all Authenticators.
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 */
public abstract class AbstractAuthenticator implements Authenticator
{
    /** A logger for the extending classes */
    protected static final Logger LOG = LoggerFactory.getLogger( AbstractAuthenticator.class );

    /** The associated DirectoryService */
    private DirectoryService directoryService;
    
    /** authenticator type */
    private final AuthenticationLevel authenticatorType;


    /**
     * Creates a new instance.
     *
     * @param type the type of this authenticator (e.g. <tt>'simple'</tt>, <tt>'none'</tt>...)
     */
    protected AbstractAuthenticator( AuthenticationLevel type )
    {
        this.authenticatorType = type;
    }


    /**
     * Returns {@link DirectoryService} for this authenticator.
     * @return the directory service core
     */
    public DirectoryService getDirectoryService()
    {
        return directoryService;
    }
    

    /**
     * {@inheritDoc}
     */
    public AuthenticationLevel getAuthenticatorType()
    {
        return authenticatorType;
    }


    /**
     * Initializes (<tt>directoryService</tt> and and calls {@link #doInit()} method.
     * Please put your initialization code into {@link #doInit()}.
     * @param directoryService the directory core for this authenticator
     * @throws LdapException if there is a problem starting up the authenticator
     */
    public final void init( DirectoryService directoryService ) throws LdapException
    {
        this.directoryService = directoryService;
        doInit();
    }


    /**
     * Implement your initialization code here.
     */
    protected void doInit()
    {
    }


    /**
     * Calls {@link #doDestroy()} method, and clears default properties
     * (<tt>factoryConfiguration</tt> and <tt>configuration</tt>).
     * Please put your deinitialization code into {@link #doDestroy()}. 
     */
    public final void destroy()
    {
        try
        {
            doDestroy();
        }
        finally
        {
            this.directoryService = null;
        }
    }


    /**
     * Implement your deinitialization code here.
     */
    protected void doDestroy()
    {
    }


    /**
     * Does nothing leaving it so subclasses can override.
     */
    public void invalidateCache( DN bindDn )
    {
    }
}
