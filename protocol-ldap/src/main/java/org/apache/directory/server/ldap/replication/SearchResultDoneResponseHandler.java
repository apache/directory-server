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
package org.apache.directory.server.ldap.replication;


import org.apache.directory.server.i18n.I18n;
import org.apache.directory.server.ldap.LdapSession;
import org.apache.directory.shared.ldap.codec.controls.replication.syncDoneValue.SyncDoneValueControl;
import org.apache.directory.shared.ldap.codec.search.SearchResultDoneCodec;
import org.apache.directory.shared.ldap.message.control.Control;
import org.apache.directory.shared.ldap.message.internal.InternalSearchResponseDone;
import org.apache.directory.shared.ldap.util.StringTools;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * Handle the SearchResultDone response, received from a producer server, when
 * replication is set.
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 * @version $Rev$, $Date$
 */
public class SearchResultDoneResponseHandler<T extends InternalSearchResponseDone>
{
    private static final Logger LOG = LoggerFactory.getLogger( SearchResultDoneResponseHandler.class );
    
    /** Speedup for logs */
    private static final boolean IS_DEBUG = LOG.isDebugEnabled();

    
    public final void handle( LdapSession session, T response ) throws Exception
    {
        LOG.debug( "Handling single searchResultDone response: {}", response );
        
        SearchResultDoneCodec searchResult = (SearchResultDoneCodec)response;
        
        // Get the control
        Control control = searchResult.getCurrentControl();
        SyncDoneValueControl syncDoneCtrl = ( SyncDoneValueControl ) control;

        /** the sync cookie sent by the server */
        byte[] syncCookie;

        if ( syncDoneCtrl.getCookie() != null )
        {
            syncCookie = syncDoneCtrl.getCookie();
            LOG.debug( "assigning cookie from sync done value control: {}", StringTools.utf8ToString( syncCookie ) );
        }
        else
        {
            LOG.info( "cookie in syncdone message is null" );
        }

        SyncreplConfiguration config = (SyncreplConfiguration)session.getIoSession().getAttribute( "SYNC_COOKIE" );
        
        if ( !config.isRefreshPersist() )
        {
            // Now, switch to refreshAndPresist
            config.setRefreshPersist( true );
            LOG.debug( "Swithing to RefreshAndPersist" );
         
            try
            {
                // the below call is required to send the updated cookie
                // and refresh mode change (i.e to refreshAndPersist stage)
                // cause while the startSync() method sleeps even after the 'sync done'
                // message arrives as part of initial searchRequest with 'refreshOnly' mode.
                // During this sleep time any 'modifications' happened on the server 
                // to already fetched entries will be sent as SearchResultEntries with
                // SyncState value as 'ADD' which will conflict with the DNs of initially received entries
                
                // TODO thinking of alternative ways to implement this in case of large DITs 
                // where the modification to entry(ies) can happen before the initial sync is done
                //doSyncSearch();
            }
            catch( Exception e )
            {
                LOG.error( I18n.err( I18n.ERR_170 ), e );
            }
        }
    }
}
