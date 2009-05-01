/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.directory.server.core.journal;

import org.apache.directory.server.core.DirectoryService;
import org.apache.directory.server.core.authn.LdapPrincipal;
import org.apache.directory.shared.ldap.ldif.LdifEntry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The default journal implementation. It stores the operation and the
 * associated status (acked or nacked) in a file which will be used to
 * restore the server if it crashes.
 * 
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 * @version $Rev$, $Date$
 */
public class DefaultJournal implements Journal
{
    /** The class logger */
    private static final Logger LOG = LoggerFactory.getLogger( DefaultJournal.class );

    /** Tells if the service is activated or not */ 
    private boolean enabled;

    /** An instance of the Journal store */
    private JournalStore store;

    
    /**
     * {@inheritDoc}
     */
    public void destroy() throws Exception
    {
        LOG.debug( "Stopping the journal" );
        
        // We have to release the file, otherwise Windows won't be able
        // to stop the server
        store.destroy();
    }


    /**
     * {@inheritDoc}
     */
    public JournalStore getJournalStore()
    {
        return store;
    }


    /**
     * {@inheritDoc}
     */
    public void init( DirectoryService directoryService ) throws Exception
    {
        LOG.debug( "Starting the journal" );

        store = new DefaultJournalStore();
        store.init( directoryService );

        LOG.debug( "The Journal service has been initialized" );
    }


    /**
     * {@inheritDoc}
     */
    public boolean isEnabled()
    {
        return enabled;
    }


    /**
     * {@inheritDoc}
     */
    public void log( LdapPrincipal principal, long revision, LdifEntry entry ) throws Exception
    {
        store.log( principal, revision, entry );
    }


    /**
     * {@inheritDoc}
     */
    public void ack( long revision )
    {
        store.ack( revision );
    }


    /**
     * {@inheritDoc}
     */
    public void nack( long revision )
    {
        store.nack( revision );
    }


    public void setEnabled( boolean enabled )
    {
        // TODO Auto-generated method stub
    }


    public void setJournalStore( JournalStore store )
    {
        // TODO Auto-generated method stub
    }
}
