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

package org.apache.directory.server.ldap.replication.provider;


import static java.lang.Math.min;
import static org.apache.directory.server.ldap.LdapServer.NO_SIZE_LIMIT;
import static org.apache.directory.server.ldap.LdapServer.NO_TIME_LIMIT;

import java.io.File;
import java.net.InetSocketAddress;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import org.apache.directory.server.core.DirectoryService;
import org.apache.directory.server.core.event.EventType;
import org.apache.directory.server.core.event.NotificationCriteria;
import org.apache.directory.server.core.filtering.EntryFilteringCursor;
import org.apache.directory.server.i18n.I18n;
import org.apache.directory.server.ldap.LdapServer;
import org.apache.directory.server.ldap.LdapSession;
import org.apache.directory.server.ldap.handlers.SearchAbandonListener;
import org.apache.directory.server.ldap.handlers.SearchTimeLimitingMonitor;
import org.apache.directory.server.ldap.replication.ReplicaEventMessage;
import org.apache.directory.shared.ldap.extras.controls.SyncDoneValue;
import org.apache.directory.shared.ldap.extras.controls.SyncInfoValue;
import org.apache.directory.shared.ldap.extras.controls.SyncRequestValue;
import org.apache.directory.shared.ldap.extras.controls.SyncStateTypeEnum;
import org.apache.directory.shared.ldap.extras.controls.SyncStateValue;
import org.apache.directory.shared.ldap.extras.controls.SynchronizationInfoEnum;
import org.apache.directory.shared.ldap.extras.controls.SynchronizationModeEnum;
import org.apache.directory.shared.ldap.extras.controls.syncrepl_impl.SyncDoneValueDecorator;
import org.apache.directory.shared.ldap.extras.controls.syncrepl_impl.SyncInfoValueDecorator;
import org.apache.directory.shared.ldap.extras.controls.syncrepl_impl.SyncStateValueDecorator;
import org.apache.directory.shared.ldap.model.constants.SchemaConstants;
import org.apache.directory.shared.ldap.model.csn.Csn;
import org.apache.directory.shared.ldap.model.entry.Attribute;
import org.apache.directory.shared.ldap.model.entry.Entry;
import org.apache.directory.shared.ldap.model.entry.StringValue;
import org.apache.directory.shared.ldap.model.entry.Value;
import org.apache.directory.shared.ldap.model.exception.LdapException;
import org.apache.directory.shared.ldap.model.exception.LdapURLEncodingException;
import org.apache.directory.shared.ldap.model.filter.AndNode;
import org.apache.directory.shared.ldap.model.filter.EqualityNode;
import org.apache.directory.shared.ldap.model.filter.ExprNode;
import org.apache.directory.shared.ldap.model.filter.GreaterEqNode;
import org.apache.directory.shared.ldap.model.filter.LessEqNode;
import org.apache.directory.shared.ldap.model.filter.OrNode;
import org.apache.directory.shared.ldap.model.filter.PresenceNode;
import org.apache.directory.shared.ldap.model.message.IntermediateResponse;
import org.apache.directory.shared.ldap.model.message.IntermediateResponseImpl;
import org.apache.directory.shared.ldap.model.message.LdapResult;
import org.apache.directory.shared.ldap.model.message.ReferralImpl;
import org.apache.directory.shared.ldap.model.message.Response;
import org.apache.directory.shared.ldap.model.message.ResultCodeEnum;
import org.apache.directory.shared.ldap.model.message.SearchRequest;
import org.apache.directory.shared.ldap.model.message.SearchResultDone;
import org.apache.directory.shared.ldap.model.message.SearchResultEntry;
import org.apache.directory.shared.ldap.model.message.SearchResultEntryImpl;
import org.apache.directory.shared.ldap.model.message.SearchResultReference;
import org.apache.directory.shared.ldap.model.message.SearchResultReferenceImpl;
import org.apache.directory.shared.ldap.model.message.SearchScope;
import org.apache.directory.shared.ldap.model.message.controls.ChangeType;
import org.apache.directory.shared.ldap.model.message.controls.ManageDsaIT;
import org.apache.directory.shared.ldap.model.schema.AttributeType;
import org.apache.directory.shared.ldap.model.url.LdapUrl;
import org.apache.directory.shared.util.Strings;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * Class used to process the incoming synchronization request from the consumers.
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 */
@SuppressWarnings("unchecked")
public class SyncReplRequestHandler implements ReplicationRequestHandler
{
    /** The logger for this class */
    private static final Logger LOG = LoggerFactory.getLogger( SyncReplRequestHandler.class );

    /** A logger for the replication provider */
    private static final Logger PROVIDER_LOG = LoggerFactory.getLogger( "PROVIDER_LOG" );

    /** A delimiter for the replicaId */
    public static final String REPLICA_ID_DELIM = ";";

    /** Tells if the replication handler is already started */
    private boolean initialized = false;

    /** The directory service instance */
    private DirectoryService dirService;

    /** The reference on the Ldap server instance */
    protected LdapServer ldapServer;

    /** An ObjectClass AT instance */
    private static AttributeType OBJECT_CLASS_AT;

    private Map<Integer, ReplicaEventLog> replicaLogMap = new HashMap<Integer, ReplicaEventLog>();

    private File syncReplData;

    private AtomicInteger replicaCount = new AtomicInteger( 0 );

    private ReplConsumerManager replicaUtil;


    /**
     * Create a SyncReplRequestHandler empty instance 
     */
    public SyncReplRequestHandler()
    {
    }


    /**
     * {@inheritDoc}
     */
    public void start( LdapServer server )
    {
        if ( initialized )
        {
            LOG.warn( "syncrepl provider was already initialized" );
            PROVIDER_LOG.warn( "syncrepl provider was already initialized" );
            return;
        }
        
        try
        {
            LOG.info( "initializing the syncrepl provider" );
            PROVIDER_LOG.debug( "initializing the syncrepl provider" );

            this.ldapServer = server;
            this.dirService = server.getDirectoryService();

            OBJECT_CLASS_AT = dirService.getSchemaManager()
                .lookupAttributeTypeRegistry( SchemaConstants.OBJECT_CLASS_AT );

            File workDir = dirService.getInstanceLayout().getLogDirectory();
            syncReplData = new File( workDir, "syncrepl-data" );
            
            if ( !syncReplData.exists() )
            {
                syncReplData.mkdirs();
            }

            replicaUtil = new ReplConsumerManager( dirService );

            loadReplicaInfo();

            registerPersistentSearches();

            Thread consumerInfoUpdateThread = new Thread( createConsumerInfoUpdateTask() );
            consumerInfoUpdateThread.setDaemon( true );
            consumerInfoUpdateThread.start();

            initialized = true;
            LOG.info( "syncrepl provider initialized successfully" );
            PROVIDER_LOG.debug( "syncrepl provider initialized successfully" );
        }
        catch ( Exception e )
        {
            LOG.error( "Failed to initialize the log files required by the syncrepl provider", e );
            PROVIDER_LOG.error( "Failed to initialize the log files required by the syncrepl provider", e );
            throw new RuntimeException( e );
        }
    }


    /**
     * {@inheritDoc}
     */
    public void stop()
    {
        for ( ReplicaEventLog log : replicaLogMap.values() )
        {
            try
            {
                PROVIDER_LOG.debug( "Stopping the logging for replica ", log.getId() );
                log.stop();
            }
            catch( Exception e )
            {
                LOG.warn( "Failed to close the event log {}", log.getId() );
                LOG.warn( "", e );
                PROVIDER_LOG.error( "Failed to close the event log {}", log.getId(), e );
            }
        }
        
        initialized = false;
    }


    /**
     * Process the incoming search request sent by a remote server when trying to replicate.
     * 
     * @param session The used LdapSession. Should be the dedicated user
     * @param request The search request
     */
    public void handleSyncRequest( LdapSession session, SearchRequest request ) throws LdapException
    {
        try
        {
            // First extract the Sync control from the request
            SyncRequestValue syncControl = ( SyncRequestValue ) request.getControls().get(
                SyncRequestValue.OID );

            // cookie is in the format <replicaId>;<Csn value>
            byte[] cookieBytes = syncControl.getCookie();

            if ( cookieBytes == null )
            {
                PROVIDER_LOG.debug( "Received a replication request {} with no cookie", request );
                // No cookie ? We have to get all the entries from the provider
                // This is an initiate Content Poll action (RFC 4533, 3.3.1)
                doInitialRefresh( session, request );
            }
            else
            {
                String cookieString = Strings.utf8ToString( cookieBytes );
                
                PROVIDER_LOG.debug( "Received a replication request {} with a cookie '{}'", request, cookieString );
                LOG.debug( "search request received with the cookie {}", cookieString );
                
                if ( !isValidCookie( cookieString ) )
                {
                    LOG.error( "received a invalid cookie {} from the consumer with session {}", cookieString, session );
                    PROVIDER_LOG.debug( "received a invalid cookie {} from the consumer with session {}", cookieString, session );
                    sendESyncRefreshRequired( session, request );
                }
                else
                {
                    ReplicaEventLog clientMsgLog = getReplicaEventLog( cookieString );
                    
                    if ( clientMsgLog == null )
                    {
                        LOG.warn( "received a valid cookie {} but there is no event log associated with this replica",
                            cookieString );
                        PROVIDER_LOG.debug( "received a valid cookie {} but there is no event log associated with this replica",
                            cookieString );
                        sendESyncRefreshRequired( session, request );
                    }
                    else
                    {
                        String consumerCsn = getCsn( cookieString );
                        doContentUpdate( session, request, clientMsgLog, consumerCsn );
                    }
                }
            }
        }
        catch ( Exception e )
        {
            LOG.error( "Failed to handle the syncrepl request", e );
            PROVIDER_LOG.error( "Failed to handle the syncrepl request", e );

            LdapException le = new LdapException( e.getMessage(), e );
            le.initCause( e );

            throw le;
        }
    }


    /**
     * Send all the stored modifications to the consumer
     */
    private String sendContentFromLog( LdapSession session, SearchRequest req, ReplicaEventLog clientMsgLog, String consumerCsn )
        throws Exception
    {
        // do the search from the log
        String lastSentCsn = clientMsgLog.getLastSentCsn();

        ReplicaJournalCursor cursor = clientMsgLog.getCursor( consumerCsn );
        
        PROVIDER_LOG.debug( "Processing the log for replica {}", clientMsgLog.getId() );

        while ( cursor.next() )
        {
            ReplicaEventMessage replicaEventMessage = cursor.get();
            Entry entry = replicaEventMessage.getEntry();
            LOG.debug( "Read message from the queue {}", entry );
            PROVIDER_LOG.debug( "Read message from the queue {}", entry );
            
            lastSentCsn = entry.get( SchemaConstants.ENTRY_CSN_AT ).getString();

            ChangeType event = replicaEventMessage.getChangeType();

            // if event type is null, then it is a MODDN operation
            if ( event == ChangeType.MODDN )
            {
                sendSearchResultEntry( session, req, entry, SyncStateTypeEnum.MODIFY );
            }
            else
            {
                SyncStateTypeEnum syncStateType = null;
                
                switch ( event )
                {
                    case ADD :
                    case MODIFY :
                        syncStateType = SyncStateTypeEnum.ADD;
                        break;
                        
                    case DELETE :
                        syncStateType = SyncStateTypeEnum.DELETE;
                        break;
                }

                sendSearchResultEntry( session, req, entry, syncStateType );
            }
        }
        
        PROVIDER_LOG.debug( "All pending modifciations for replica {} processed", clientMsgLog.getId() );
        cursor.close();

        return lastSentCsn;
    }


    /**
     * process the update of the consumer, starting from the given LastEntryCSN the consumer 
     * has sent with the sync request.
     */
    private void doContentUpdate( LdapSession session, SearchRequest req, ReplicaEventLog replicaLog, String consumerCsn )
        throws Exception
    {
        boolean refreshNPersist = isRefreshNPersist( req );

        // if this method is called with refreshAndPersist  
        // means the client was offline after it initiated a persistent synch session
        // we need to update the handler's session 
        if ( refreshNPersist )
        {
            SyncReplSearchListener handler = replicaLog.getPersistentListener();
            handler.setSearchRequest( req );
            handler.setSession( session );
        }

        String lastSentCsn = sendContentFromLog( session, req, replicaLog, consumerCsn );

        PROVIDER_LOG.debug( "The latest entry sent to the consumer {} has this CSN : {}", replicaLog.getId(), lastSentCsn );
        byte[] cookie = Strings.getBytesUtf8(replicaLog.getId() + REPLICA_ID_DELIM + lastSentCsn);

        if ( refreshNPersist )
        {
            IntermediateResponse intermResp = new IntermediateResponseImpl( req.getMessageId() );
            intermResp.setResponseName( SyncInfoValue.OID );

            SyncInfoValue syncInfo = new SyncInfoValueDecorator( ldapServer.getDirectoryService()
                .getLdapCodecService(),
                SynchronizationInfoEnum.NEW_COOKIE );
            syncInfo.setCookie( cookie );
            intermResp.setResponseValue( ((SyncInfoValueDecorator)syncInfo).getValue() );

            PROVIDER_LOG.debug( "Sent the intermediate response to the {} consumer, {}", replicaLog.getId(), intermResp );
            session.getIoSession().write( intermResp );

            replicaLog.getPersistentListener().setPushInRealTime( refreshNPersist );
        }
        else
        {
            SearchResultDone searchDoneResp = ( SearchResultDone ) req.getResultResponse();
            searchDoneResp.getLdapResult().setResultCode( ResultCodeEnum.SUCCESS );
            SyncDoneValue syncDone = new SyncDoneValueDecorator( 
                ldapServer.getDirectoryService().getLdapCodecService() );
            syncDone.setCookie( cookie );
            searchDoneResp.addControl( syncDone );

            PROVIDER_LOG.debug( "Send a SearchResultDone response to the {} consumer", replicaLog.getId(), searchDoneResp );

            session.getIoSession().write( searchDoneResp );
        }

        replicaLog.setLastSentCsn( lastSentCsn );
    }


    /**
     * Process the initial refresh : we will send all the entries 
     */
    private void doInitialRefresh( LdapSession session, SearchRequest request ) throws Exception
    {
        String originalFilter = request.getFilter().toString();
        InetSocketAddress address = ( InetSocketAddress ) session.getIoSession().getRemoteAddress();
        String hostName = address.getAddress().getHostName();

        ExprNode modifiedFilter = modifyFilter( session, request );

        String contextCsn = dirService.getContextCsn();

        boolean refreshNPersist = isRefreshNPersist( request );

        // first register a ReplicaEventLog before starting the initial content refresh
        // this is to log all the operations happen on DIT during initial content refresh
        ReplicaEventLog replicaLog = createRelicaEventLog( hostName, originalFilter );

        replicaLog.setRefreshNPersist( refreshNPersist );
        StringValue contexCsnValue = new StringValue( contextCsn );

        // modify the filter to include the context Csn
        GreaterEqNode csnGeNode = new GreaterEqNode( SchemaConstants.ENTRY_CSN_AT, contexCsnValue );
        ExprNode postInitContentFilter = new AndNode( modifiedFilter, csnGeNode );
        request.setFilter( postInitContentFilter );

        // now we process entries forever as they change
        LOG.info( "starting persistent search for the client {}", replicaLog );
        PROVIDER_LOG.debug( "Starting persistent search for the client {}", replicaLog );

        // irrespective of the sync mode set the 'isRealtimePush' to false initially so that we can
        // store the modifications in the queue and later if it is a persist mode
        // we push this queue's content and switch to realtime mode
        SyncReplSearchListener handler = new SyncReplSearchListener( session, request, replicaLog, false );
        replicaLog.setPersistentListener( handler );

        // compose notification criteria and add the listener to the event 
        // service using that notification criteria to determine which events 
        // are to be delivered to the persistent search issuing client
        NotificationCriteria criteria = new NotificationCriteria();
        criteria.setAliasDerefMode( request.getDerefAliases() );
        criteria.setBase( request.getBase() );
        criteria.setFilter( request.getFilter() );
        criteria.setScope( request.getScope() );
        criteria.setEventMask( EventType.ALL_EVENT_TYPES_MASK );

        replicaLog.setSearchCriteria( criteria );

        dirService.getEventService().addListener( handler, criteria );

        // then start pushing initial content
        LessEqNode csnNode = new LessEqNode( SchemaConstants.ENTRY_CSN_AT, contexCsnValue );

        // modify the filter to include the context Csn
        ExprNode initialContentFilter = new AndNode( modifiedFilter, csnNode );
        request.setFilter( initialContentFilter );

        // Now, do a search to get all the entries
        SearchResultDone searchDoneResp = doSimpleSearch( session, request );

        if ( searchDoneResp.getLdapResult().getResultCode() == ResultCodeEnum.SUCCESS )
        {
            replicaLog.setLastSentCsn( contextCsn );
            byte[] cookie = Strings.getBytesUtf8( replicaLog.getId() + REPLICA_ID_DELIM + contextCsn );

            if ( refreshNPersist ) // refreshAndPersist mode
            {
                contextCsn = sendContentFromLog( session, request, replicaLog, contextCsn );
                cookie = Strings.getBytesUtf8(replicaLog.getId() + REPLICA_ID_DELIM + contextCsn);

                IntermediateResponse intermResp = new IntermediateResponseImpl( request.getMessageId() );
                intermResp.setResponseName( SyncInfoValue.OID );

                SyncInfoValue syncInfo = new SyncInfoValueDecorator( 
                    ldapServer.getDirectoryService().getLdapCodecService(), SynchronizationInfoEnum.NEW_COOKIE );
                syncInfo.setCookie( cookie );
                intermResp.setResponseValue( ((SyncInfoValueDecorator)syncInfo).getValue() );

                PROVIDER_LOG.info( "Sending the intermediate response to consumer {}, {}", replicaLog, syncInfo );

                session.getIoSession().write( intermResp );

                // switch the handler mode to realtime push
                handler.setPushInRealTime( refreshNPersist );
            }
            else
            {
                // no need to send from the log, that will be done in the next refreshOnly session
                SyncDoneValue syncDone = new SyncDoneValueDecorator(
                    ldapServer.getDirectoryService().getLdapCodecService() );
                syncDone.setCookie( cookie );
                searchDoneResp.addControl( syncDone );
                PROVIDER_LOG.info( "Sending the searchResultDone response to consumer {}, {}", replicaLog, searchDoneResp );

                session.getIoSession().write( searchDoneResp );
            }
        }
        else
        // if not succeeded return
        {
            LOG.warn( "initial content refresh didn't succeed due to {}", searchDoneResp.getLdapResult()
                .getResultCode() );
            PROVIDER_LOG.warn( "initial content refresh didn't succeed due to {}", searchDoneResp.getLdapResult()
                .getResultCode() );
            replicaLog.truncate();
            replicaLog = null;

            // remove the listener
            dirService.getEventService().removeListener( handler );

            return;
        }

        // if all is well then store the consumer information
        replicaUtil.addConsumerEntry(replicaLog );

        // add to the map only after storing in the DIT, else the Replica update thread barfs
        replicaLogMap.put( replicaLog.getId(), replicaLog );
    }


    /**
     * Process a search on the provider to get all the modified entries. We then send all
     * of them to the consumer
     */
    private SearchResultDone doSimpleSearch( LdapSession session, SearchRequest req ) throws Exception
    {
        SearchResultDone searchDoneResp = ( SearchResultDone ) req.getResultResponse();
        LdapResult ldapResult = searchDoneResp.getLdapResult();

        // A normal search
        // Check that we have a cursor or not. 
        // No cursor : do a search.
        EntryFilteringCursor cursor = session.getCoreSession().search( req );

        // Position the cursor at the beginning
        cursor.beforeFirst();

        /*
         * Iterate through all search results building and sending back responses
         * for each search result returned.
         */
        try
        {
            // Get the size limits
            // Don't bother setting size limits for administrators that don't ask for it
            long serverLimit = getServerSizeLimit( session, req );

            long requestLimit = req.getSizeLimit() == 0L ? Long.MAX_VALUE : req.getSizeLimit();

            req.addAbandonListener( new SearchAbandonListener( ldapServer, cursor ) );
            setTimeLimitsOnCursor( req, session, cursor );
            LOG.debug( "using <{},{}> for size limit", requestLimit, serverLimit );
            long sizeLimit = min( requestLimit, serverLimit );

            readResults( session, req, ldapResult, cursor, sizeLimit );
        }
        finally
        {
            if ( cursor != null )
            {
                try
                {
                    cursor.close();
                }
                catch ( Exception e )
                {
                    LOG.error( I18n.err( I18n.ERR_168 ), e );
                }
            }
        }

        return searchDoneResp;
    }


    /**
     * Process the results get from a search request. We will send them to the client.
     */
    private void readResults( LdapSession session, SearchRequest req, LdapResult ldapResult,
        EntryFilteringCursor cursor, long sizeLimit ) throws Exception
    {
        long count = 0;

        
        while ( ( count < sizeLimit ) && cursor.next() )
        {
            // Handle closed session
            if ( session.getIoSession().isClosing() )
            {
                // The client has closed the connection
                LOG.debug( "Request terminated for message {}, the client has closed the session", req.getMessageId() );
                PROVIDER_LOG.debug( "Request terminated for message {}, the client has closed the session", req.getMessageId() );
                break;
            }

            if ( req.isAbandoned() )
            {
                // The cursor has been closed by an abandon request.
                LOG.debug( "Request terminated by an AbandonRequest for message {}", req.getMessageId() );
                PROVIDER_LOG.debug( "Request terminated by an AbandonRequest for message {}", req.getMessageId() );
                break;
            }

            Entry entry = cursor.get();

            sendSearchResultEntry( session, req, entry, SyncStateTypeEnum.ADD );

            count++;
        }

        // DO NOT WRITE THE RESPONSE - JUST RETURN IT
        ldapResult.setResultCode( ResultCodeEnum.SUCCESS );

        if ( ( count >= sizeLimit ) && ( cursor.next() ) )
        {
            // We have reached the limit
            // Move backward on the cursor to restore the previous position, as we moved forward
            // to check if there is one more entry available
            cursor.previous();
            // Special case if the user has requested more elements than the request size limit
            ldapResult.setResultCode( ResultCodeEnum.SIZE_LIMIT_EXCEEDED );
        }
    }


    /**
     * Prepare and send a search result entry response, with the associated
     * SyncState control.
     */
    private void sendSearchResultEntry( LdapSession session, SearchRequest req, Entry entry,
        SyncStateTypeEnum syncStateType ) throws Exception
    {
        Attribute uuid = entry.get( SchemaConstants.ENTRY_UUID_AT );
        
        // Create the SyncState control
        SyncStateValue syncStateControl = new SyncStateValueDecorator(
            ldapServer.getDirectoryService().getLdapCodecService() );
        syncStateControl.setSyncStateType( syncStateType );
        syncStateControl.setEntryUUID( Strings.uuidToBytes( uuid.getString() ) );

        if ( syncStateType == SyncStateTypeEnum.DELETE )
        {
            // clear the entry's all attributes except the Dn and entryUUID
            entry.clear();
            entry.add( uuid );
        }

        Response resp = generateResponse( session, req, entry );
        resp.addControl( syncStateControl );

        LOG.debug( "Sending {}", entry.getDn() );
        PROVIDER_LOG.debug( "Sending the entry:", entry.getDn() );
        session.getIoSession().write( resp );
    }


    /**
     * Build the response to be sent to the client
     */
    private Response generateResponse( LdapSession session, SearchRequest req, Entry entry ) throws Exception
    {
        Attribute ref = entry.get( SchemaConstants.REF_AT );
        boolean hasManageDsaItControl = req.getControls().containsKey( ManageDsaIT.OID );

        if ( ( ref != null ) && !hasManageDsaItControl )
        {
            // The entry is a referral.
            SearchResultReference respRef;
            respRef = new SearchResultReferenceImpl( req.getMessageId() );
            respRef.setReferral( new ReferralImpl() );

            for ( Value<?> val : ref )
            {
                String url = val.getString();

                if ( !url.startsWith( "ldap" ) )
                {
                    respRef.getReferral().addLdapUrl( url );
                }

                LdapUrl ldapUrl = null;
                
                try
                {
                    ldapUrl = new LdapUrl( url );
                    ldapUrl.setForceScopeRendering( true );
                }
                catch ( LdapURLEncodingException e )
                {
                    LOG.error( I18n.err( I18n.ERR_165, url, entry ) );
                }

                switch ( req.getScope() )
                {
                    case SUBTREE:
                        ldapUrl.setScope( SearchScope.SUBTREE.getScope() );
                        break;

                    case ONELEVEL: // one level here is object level on remote server
                        ldapUrl.setScope( SearchScope.OBJECT.getScope() );
                        break;

                    default:
                        throw new IllegalStateException( I18n.err( I18n.ERR_686 ) );
                }

                respRef.getReferral().addLdapUrl( ldapUrl.toString() );
            }

            return respRef;
        }
        else
        {
            // The entry is not a referral, or the ManageDsaIt control is set
            SearchResultEntry respEntry;
            respEntry = new SearchResultEntryImpl( req.getMessageId() );
            respEntry.setEntry( entry );
            respEntry.setObjectName( entry.getDn() );

            return respEntry;
        }
    }


    /**
     * Return the server size limit
     */
    private long getServerSizeLimit( LdapSession session, SearchRequest request )
    {
        if ( session.getCoreSession().isAnAdministrator() )
        {
            if ( request.getSizeLimit() == NO_SIZE_LIMIT )
            {
                return Long.MAX_VALUE;
            }
            else
            {
                return request.getSizeLimit();
            }
        }
        else
        {
            if ( ldapServer.getMaxSizeLimit() == NO_SIZE_LIMIT )
            {
                return Long.MAX_VALUE;
            }
            else
            {
                return ldapServer.getMaxSizeLimit();
            }
        }
    }


    private void setTimeLimitsOnCursor( SearchRequest req, LdapSession session,
        final EntryFilteringCursor cursor )
    {
        // Don't bother setting time limits for administrators
        if ( session.getCoreSession().isAnAdministrator() && req.getTimeLimit() == NO_TIME_LIMIT )
        {
            return;
        }

        /*
         * Non administrator based searches are limited by time if the server 
         * has been configured with unlimited time and the request specifies 
         * unlimited search time
         */
        if ( ldapServer.getMaxTimeLimit() == NO_TIME_LIMIT && req.getTimeLimit() == NO_TIME_LIMIT )
        {
            return;
        }

        /*
         * If the non-administrator user specifies unlimited time but the server 
         * is configured to limit the search time then we limit by the max time 
         * allowed by the configuration 
         */
        if ( req.getTimeLimit() == 0 )
        {
            cursor.setClosureMonitor( new SearchTimeLimitingMonitor( ldapServer.getMaxTimeLimit(), TimeUnit.SECONDS ) );
            return;
        }

        /*
         * If the non-administrative user specifies a time limit equal to or 
         * less than the maximum limit configured in the server then we 
         * constrain search by the amount specified in the request
         */
        if ( ldapServer.getMaxTimeLimit() >= req.getTimeLimit() )
        {
            cursor.setClosureMonitor( new SearchTimeLimitingMonitor( req.getTimeLimit(), TimeUnit.SECONDS ) );
            return;
        }

        /*
         * Here the non-administrative user's requested time limit is greater 
         * than what the server's configured maximum limit allows so we limit
         * the search to the configured limit
         */
        cursor.setClosureMonitor( new SearchTimeLimitingMonitor( ldapServer.getMaxTimeLimit(), TimeUnit.SECONDS ) );
    }


    public ExprNode modifyFilter( LdapSession session, SearchRequest req ) throws Exception
    {
        /*
         * Most of the time the search filter is just (objectClass=*) and if 
         * this is the case then there's no reason at all to OR this with an
         * (objectClass=referral).  If we detect this case then we leave it 
         * as is to represent the OR condition:
         * 
         *  (| (objectClass=referral)(objectClass=*)) == (objectClass=*)
         */
        boolean isOcPresenceFilter = false;
        
        if ( req.getFilter() instanceof PresenceNode )
        {
            PresenceNode presenceNode = ( PresenceNode ) req.getFilter();

            AttributeType at = session.getCoreSession().getDirectoryService().getSchemaManager()
                .lookupAttributeTypeRegistry( presenceNode.getAttribute() );
            
            if ( at.getOid().equals( SchemaConstants.OBJECT_CLASS_AT_OID ) )
            {
                isOcPresenceFilter = true;
            }
        }

        ExprNode filter = req.getFilter();
        
        if ( !req.hasControl( ManageDsaIT.OID ) && !isOcPresenceFilter )
        {
            filter = new OrNode( req.getFilter(), newIsReferralEqualityNode( session ) );
        }

        return filter;
    }


    private EqualityNode<String> newIsReferralEqualityNode( LdapSession session ) throws Exception
    {
        EqualityNode<String> ocIsReferral = new EqualityNode<String>( SchemaConstants.OBJECT_CLASS_AT, new StringValue(
            OBJECT_CLASS_AT, SchemaConstants.REFERRAL_OC ) );

        return ocIsReferral;
    }


    /**
     * Update the consumer configuration entries if they are 'dirty' (ie, if
     * the consumer lastCSN is not up to date)
     */
    private void storeReplicaInfo()
    {
        try
        {
            for ( Map.Entry<Integer, ReplicaEventLog> e : replicaLogMap.entrySet() )
            {
                ReplicaEventLog replica = e.getValue();
                
                if ( replica.isDirty() )
                {
                    LOG.debug( "updating the details of replica {}", replica );
                    PROVIDER_LOG.debug( "updating the details of replica {}", replica );
                    replicaUtil.updateReplicaLastSentCsn( replica );
                    replica.setDirty( false );
                }
            }
        }
        catch ( Exception e )
        {
            LOG.error( "Failed to store the replica information", e );
            PROVIDER_LOG.error( "Failed to store the replica information", e );
        }
    }


    /**
     * Read and store the consumer's informations
     */
    private void loadReplicaInfo()
    {
        try
        {
            List<ReplicaEventLog> eventLogs = replicaUtil.getReplicaEventLogs();
            
            if ( !eventLogs.isEmpty() )
            {
                for ( ReplicaEventLog replica : eventLogs )
                {
                    LOG.debug( "initializing the replica log from {}", replica.getId() );
                    PROVIDER_LOG.debug( "initializing the replica log from {}", replica.getId() );
                    replicaLogMap.put( replica.getId(), replica );

                    // update the replicaCount's value to assign a correct value to the new replica(s) 
                    if ( replicaCount.get() < replica.getId() )
                    {
                        replicaCount.set( replica.getId() );
                    }
                }
            }
            else
            {
                LOG.debug( "no replica logs found to initialize" );
                PROVIDER_LOG.debug( "no replica logs found to initialize" );
            }
        }
        catch ( Exception e )
        {
            LOG.error( "Failed to load the replica information", e );
            PROVIDER_LOG.error( "Failed to load the replica information", e );
        }
    }


    /**
     * Register the listeners for each existing consumers
     */
    private void registerPersistentSearches() throws Exception
    {
        for ( Map.Entry<Integer, ReplicaEventLog> e : replicaLogMap.entrySet() )
        {
            ReplicaEventLog log = e.getValue();

            if ( log.getSearchCriteria() != null )
            {
                LOG.debug( "registering peristent search for the replica {}", log.getId() );
                PROVIDER_LOG.debug( "registering peristent search for the replica {}", log.getId() );
                SyncReplSearchListener handler = new SyncReplSearchListener( null, null, log, false );
                log.setPersistentListener( handler );

                dirService.getEventService().addListener( handler, log.getSearchCriteria() );
            }
            else
            {
                LOG.warn( "invalid peristent search criteria {} for the replica {}", log.getSearchCriteria(), log
                    .getId() );
                PROVIDER_LOG.warn( "invalid peristent search criteria {} for the replica {}", log.getSearchCriteria(), log
                    .getId() );
            }
        }
    }


    /**
     * Create a thread to process replication communication with a consumer
     */
    private Runnable createConsumerInfoUpdateTask()
    {
        Runnable task = new Runnable()
        {
            public void run()
            {
                while ( true )
                {
                    storeReplicaInfo();
                    
                    try
                    {
                        Thread.sleep( 10000 );
                    }
                    catch ( InterruptedException e )
                    {
                        LOG.warn( "thread storing the replica information was interrupted", e );
                        PROVIDER_LOG.warn( "thread storing the replica information was interrupted", e );
                    }
                }
            }
        };

        return task;
    }


    /**
     * Check the cookie syntax. A cookie must have the following syntax :
     * <replicaId> [ ';' [ <CSN> ] ]
     */
    private boolean isValidCookie( String cookieString )
    {
        if ( ( cookieString == null ) || ( cookieString.trim().length() == 0 ) )
        {
            return false;
        }

        int pos = cookieString.indexOf( REPLICA_ID_DELIM );
        
        // position should start from 1 or higher cause a cookie can be
        // like "0;<csn>" or "11;<csn>"
        if ( pos <= 0 )  
        {
            return false;
        }

        String replicaId = cookieString.substring( 0, pos );
        
        try
        {
            Integer.parseInt( replicaId );
        }
        catch ( NumberFormatException e )
        {
            LOG.debug( "Failed to parse the replica id {}", replicaId );
            return false;
        }

        if ( pos == cookieString.length() )
        {
            return false;
        }

        String csnString = cookieString.substring( pos + 1 );

        return Csn.isValid( csnString );
    }

    /**
     * returns the CSN present in cookie
     * 
     * @param cookieString the cookie
     * @return
     */
    private String getCsn( String cookieString )
    {
        int pos = cookieString.indexOf( REPLICA_ID_DELIM );
        return cookieString.substring( pos + 1 );
    }

    
    /**
     * returns the replica id present in cookie
     * 
     * @param cookieString  the cookie
     * @return
     */
    private int getReplicaId( String cookieString )
    {
        String replicaId = cookieString.substring( 0, cookieString.indexOf( REPLICA_ID_DELIM ) );
        return Integer.parseInt( replicaId );
    }


    /**
     * Get the Replica event log from the replica ID found in the cookie 
     */
    private ReplicaEventLog getReplicaEventLog( String cookieString ) throws Exception
    {
        ReplicaEventLog replicaLog = null;

        if ( isValidCookie( cookieString ) )
        {
            int clientId = getReplicaId( cookieString );
            replicaLog = replicaLogMap.get( clientId );
        }

        return replicaLog;
    }


    /**
     * Create a new ReplicaEventLog
     */
    private ReplicaEventLog createRelicaEventLog( String hostName, String filter ) throws Exception
    {
        int replicaId = replicaCount.incrementAndGet();

        LOG.debug( "creating a new event log for the replica with id {}", replicaId );

        ReplicaEventLog replicaLog = new ReplicaEventLog( dirService, replicaId );
        replicaLog.setHostName( hostName );
        replicaLog.setSearchFilter( filter );

        return replicaLog;
    }


    /**
     * Send an error response to he consue r: it has to send a SYNC_REFRESH request first. 
     */
    private void sendESyncRefreshRequired( LdapSession session, SearchRequest req ) throws Exception
    {
        SearchResultDone searchDoneResp = ( SearchResultDone ) req.getResultResponse();
        searchDoneResp.getLdapResult().setResultCode( ResultCodeEnum.E_SYNC_REFRESH_REQUIRED );
        SyncDoneValue syncDone = new SyncDoneValueDecorator(
            ldapServer.getDirectoryService().getLdapCodecService() );
        searchDoneResp.addControl( syncDone );

        session.getIoSession().write( searchDoneResp );
    }


    /**
     * Tells if the control contains the REFRESHNPERSIST mode
     */
    private boolean isRefreshNPersist( SearchRequest req )
    {
        SyncRequestValue control = ( SyncRequestValue ) req.getControls().get( SyncRequestValue.OID );
        
        return control.getMode() == SynchronizationModeEnum.REFRESH_AND_PERSIST;
    }
}
