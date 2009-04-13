/*
 *   Licensed to the Apache Software Foundation (ASF) under one
 *   or more contributor license agreements.  See the NOTICE file
 *   distributed with this work for additional information
 *   regarding copyright ownership.  The ASF licenses this file
 *   to you under the Apache License, Version 2.0 (the
 *   "License"); you may not use this file except in compliance
 *   with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 *   Unless required by applicable law or agreed to in writing,
 *   software distributed under the License is distributed on an
 *   "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 *   KIND, either express or implied.  See the License for the
 *   specific language governing permissions and limitations
 *   under the License.
 *
 */
package org.apache.directory.server.ldap.handlers;


import org.apache.directory.server.core.event.DirectoryListener;
import org.apache.directory.server.core.filtering.EntryFilteringCursor;
import org.apache.directory.server.ldap.LdapService;
import org.apache.directory.shared.ldap.exception.OperationAbandonedException;
import org.apache.directory.shared.ldap.message.AbandonListener;
import org.apache.directory.shared.ldap.message.InternalAbandonableRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * An AbandonListener implementation which closes an associated cursor or 
 * removes a DirectoryListener.
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 * @version $Rev$, $Date$
 */
public class SearchAbandonListener implements AbandonListener
{
    private static final Logger LOG = LoggerFactory.getLogger( SearchAbandonListener.class );
    private final LdapService ldapService;
    private EntryFilteringCursor cursor;
    private DirectoryListener listener;
    
    
    public SearchAbandonListener( LdapService ldapService, EntryFilteringCursor cursor, DirectoryListener listener )
    {
        if ( ldapService == null )
        {
            throw new NullPointerException( "ldapService" );
        }
        
        this.ldapService = ldapService;
        this.cursor = cursor;
        this.listener = listener;
    }
    
    
    public SearchAbandonListener( LdapService ldapService, DirectoryListener listener )
    {
        this ( ldapService, null, listener );
    }
    
    
    public SearchAbandonListener( LdapService ldapService, EntryFilteringCursor cursor )
    {
        this ( ldapService, cursor, null );
    }
    
    
    public void requestAbandoned( InternalAbandonableRequest req )
    {
        if ( listener != null )
        {
            ldapService.getDirectoryService().getEventService().removeListener( listener );
        }

        try
        {
            if ( cursor != null )
            {
                /*
                 * When this method is called due to an abandon request it 
                 * will close the cursor but other threads processing the 
                 * search will get an OperationAbandonedException which as
                 * seen below will make sure the proper handling is 
                 * performed.
                 */
                cursor.close( new OperationAbandonedException() );
            }
        }
        catch ( Exception e )
        {
            LOG.error( "Failed to close the search cursor for message {} on abandon request.", 
                req.getMessageId(), e );
        }
    }
}


