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
import java.io.FilenameFilter;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import org.apache.directory.api.ldap.extras.controls.SynchronizationModeEnum;
import org.apache.directory.api.ldap.extras.controls.syncrepl.syncDone.SyncDoneValue;
import org.apache.directory.api.ldap.extras.controls.syncrepl.syncInfoValue.SyncInfoValue;
import org.apache.directory.api.ldap.extras.controls.syncrepl.syncInfoValue.SyncRequestValue;
import org.apache.directory.api.ldap.extras.controls.syncrepl.syncInfoValue.SynchronizationInfoEnum;
import org.apache.directory.api.ldap.extras.controls.syncrepl.syncState.SyncStateTypeEnum;
import org.apache.directory.api.ldap.extras.controls.syncrepl.syncState.SyncStateValue;
import org.apache.directory.api.ldap.extras.controls.syncrepl_impl.SyncDoneValueDecorator;
import org.apache.directory.api.ldap.extras.controls.syncrepl_impl.SyncInfoValueDecorator;
import org.apache.directory.api.ldap.extras.controls.syncrepl_impl.SyncStateValueDecorator;
import org.apache.directory.api.ldap.model.constants.Loggers;
import org.apache.directory.api.ldap.model.constants.SchemaConstants;
import org.apache.directory.api.ldap.model.cursor.Cursor;
import org.apache.directory.api.ldap.model.entry.Attribute;
import org.apache.directory.api.ldap.model.entry.Entry;
import org.apache.directory.api.ldap.model.entry.Modification;
import org.apache.directory.api.ldap.model.entry.StringValue;
import org.apache.directory.api.ldap.model.entry.Value;
import org.apache.directory.api.ldap.model.exception.LdapException;
import org.apache.directory.api.ldap.model.exception.LdapInvalidAttributeValueException;
import org.apache.directory.api.ldap.model.exception.LdapURLEncodingException;
import org.apache.directory.api.ldap.model.filter.AndNode;
import org.apache.directory.api.ldap.model.filter.EqualityNode;
import org.apache.directory.api.ldap.model.filter.ExprNode;
import org.apache.directory.api.ldap.model.filter.GreaterEqNode;
import org.apache.directory.api.ldap.model.filter.LessEqNode;
import org.apache.directory.api.ldap.model.filter.OrNode;
import org.apache.directory.api.ldap.model.filter.PresenceNode;
import org.apache.directory.api.ldap.model.message.IntermediateResponse;
import org.apache.directory.api.ldap.model.message.IntermediateResponseImpl;
import org.apache.directory.api.ldap.model.message.LdapResult;
import org.apache.directory.api.ldap.model.message.ReferralImpl;
import org.apache.directory.api.ldap.model.message.Response;
import org.apache.directory.api.ldap.model.message.ResultCodeEnum;
import org.apache.directory.api.ldap.model.message.SearchRequest;
import org.apache.directory.api.ldap.model.message.SearchResultDone;
import org.apache.directory.api.ldap.model.message.SearchResultEntry;
import org.apache.directory.api.ldap.model.message.SearchResultEntryImpl;
import org.apache.directory.api.ldap.model.message.SearchResultReference;
import org.apache.directory.api.ldap.model.message.SearchResultReferenceImpl;
import org.apache.directory.api.ldap.model.message.SearchScope;
import org.apache.directory.api.ldap.model.message.controls.ChangeType;
import org.apache.directory.api.ldap.model.message.controls.ManageDsaIT;
import org.apache.directory.api.ldap.model.message.controls.SortKey;
import org.apache.directory.api.ldap.model.message.controls.SortRequest;
import org.apache.directory.api.ldap.model.message.controls.SortRequestControlImpl;
import org.apache.directory.api.ldap.model.name.Dn;
import org.apache.directory.api.ldap.model.schema.AttributeType;
import org.apache.directory.api.ldap.model.url.LdapUrl;
import org.apache.directory.api.util.Strings;
import org.apache.directory.server.constants.ServerDNConstants;
import org.apache.directory.server.core.api.DirectoryService;
import org.apache.directory.server.core.api.event.DirectoryListenerAdapter;
import org.apache.directory.server.core.api.event.EventService;
import org.apache.directory.server.core.api.event.EventType;
import org.apache.directory.server.core.api.event.NotificationCriteria;
import org.apache.directory.server.core.api.interceptor.context.DeleteOperationContext;
import org.apache.directory.server.core.api.interceptor.context.ModifyOperationContext;
import org.apache.directory.server.core.api.interceptor.context.OperationContext;
import org.apache.directory.server.core.api.partition.Partition;
import org.apache.directory.server.i18n.I18n;
import org.apache.directory.server.ldap.LdapProtocolUtils;
import org.apache.directory.server.ldap.LdapServer;
import org.apache.directory.server.ldap.LdapSession;
import org.apache.directory.server.ldap.handlers.SearchAbandonListener;
import org.apache.directory.server.ldap.handlers.SearchTimeLimitingMonitor;
import org.apache.directory.server.ldap.replication.ReplicaEventMessage;
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
    /** A logger for the replication provider */
    private static final Logger PROVIDER_LOG = LoggerFactory.getLogger( Loggers.PROVIDER_LOG.getName() );

    /** Tells if the replication handler is already started */
    private boolean initialized = false;

    /** The directory service instance */
    private DirectoryService dirService;

    /** The reference on the Ldap server instance */
    protected LdapServer ldapServer;

    /** An ObjectClass AT instance */
    private AttributeType objectClassAT;

    /** The CSN AttributeType instance */
    private AttributeType csnAT;

    private Map<Integer, ReplicaEventLog> replicaLogMap = new ConcurrentHashMap<Integer, ReplicaEventLog>();

    private File syncReplData;

    private AtomicInteger replicaCount = new AtomicInteger( 0 );

    private ReplConsumerManager replicaUtil;

    private ConsumerLogEntryChangeListener cledListener;

    private ReplicaEventLogJanitor logJanitor;

    private AttributeType replLogMaxIdleAT;

    private AttributeType replLogPurgeThresholdCountAT;

    /** thread used for updating consumer infor */
    private Thread consumerInfoUpdateThread;

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
        // Check that the handler is not already started : we don't want to start it twice...
        if ( initialized )
        {
            PROVIDER_LOG.warn( "syncrepl provider was already initialized" );

            return;
        }

        try
        {
            PROVIDER_LOG.debug( "initializing the syncrepl provider" );

            this.ldapServer = server;
            this.dirService = server.getDirectoryService();

            csnAT = dirService.getSchemaManager()
                .lookupAttributeTypeRegistry( SchemaConstants.ENTRY_CSN_AT );

            objectClassAT = dirService.getSchemaManager()
                .lookupAttributeTypeRegistry( SchemaConstants.OBJECT_CLASS_AT );

            replLogMaxIdleAT = dirService.getSchemaManager()
                .lookupAttributeTypeRegistry( SchemaConstants.ADS_REPL_LOG_MAX_IDLE );

            replLogPurgeThresholdCountAT = dirService.getSchemaManager()
                .lookupAttributeTypeRegistry( SchemaConstants.ADS_REPL_LOG_PURGE_THRESHOLD_COUNT );

            // Get and create the replication directory if it does not exist
            syncReplData = dirService.getInstanceLayout().getReplDirectory();

            if ( !syncReplData.exists() )
            {
                if ( !syncReplData.mkdirs() )
                {
                    throw new IOException( I18n.err( I18n.ERR_112_COULD_NOT_CREATE_DIRECORY, syncReplData ) );
                }
            }

            // Create the replication manager
            replicaUtil = new ReplConsumerManager( dirService );

            loadReplicaInfo();

            logJanitor = new ReplicaEventLogJanitor( dirService, replicaLogMap );
            logJanitor.start();

            registerPersistentSearches();

            cledListener = new ConsumerLogEntryChangeListener();
            NotificationCriteria criteria = new NotificationCriteria();
            criteria.setBase( new Dn( dirService.getSchemaManager(), ServerDNConstants.REPL_CONSUMER_DN_STR ) );
            criteria.setEventMask( EventType.DELETE );

            dirService.getEventService().addListener( cledListener, criteria );

            CountDownLatch latch = new CountDownLatch( 1 );

            consumerInfoUpdateThread = new Thread( createConsumerInfoUpdateTask( latch ) );
            consumerInfoUpdateThread.setDaemon( true );
            consumerInfoUpdateThread.start();

            // Wait for the thread to be ready. We wait 5 minutes, it should be way more
            // than necessary
            boolean threadInitDone = latch.await( 5, TimeUnit.MINUTES );

            if ( !threadInitDone )
            {
                // We have had a time out : just get out
                PROVIDER_LOG.error( "The consumer replica thread has not been initialized in time" );
                throw new RuntimeException( "Cannot initialize the Provider replica listener" );
            }

            initialized = true;
            PROVIDER_LOG.debug( "syncrepl provider initialized successfully" );
        }
        catch ( Exception e )
        {
            PROVIDER_LOG.error( "Failed to initialize the log files required by the syncrepl provider", e );
            throw new RuntimeException( e );
        }
    }


    /**
     * {@inheritDoc}
     */
    public void stop()
    {
        EventService evtSrv = dirService.getEventService();

        evtSrv.removeListener( cledListener );
        //first set the 'stop' flag
        logJanitor.stopCleaning();
        //then interrupt the janitor
        logJanitor.interrupt();

        //then stop the consumerInfoUpdateThread
        consumerInfoUpdateThread.interrupt();
        
        for ( ReplicaEventLog log : replicaLogMap.values() )
        {
            try
            {
                PROVIDER_LOG.debug( "Stopping the logging for replica ", log.getId() );
                evtSrv.removeListener( log.getPersistentListener() );
                log.stop();
            }
            catch ( Exception e )
            {
                PROVIDER_LOG.error( "Failed to close the event log {}", log.getId(), e );
            }
        }

        // flush the dirty repos
        storeReplicaInfo();

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
        PROVIDER_LOG.debug( "Received a Syncrepl request : {} from {}", request, session );
        try
        {
            if ( !request.getAttributes().contains( SchemaConstants.ALL_OPERATIONAL_ATTRIBUTES ) )
            {
                // this is needed for accessing entryUUID and entryCSN attributes for internal purpose
                request.addAttributes( SchemaConstants.ALL_OPERATIONAL_ATTRIBUTES );
            }

            // First extract the Sync control from the request
            SyncRequestValue syncControl = ( SyncRequestValue ) request.getControls().get(
                SyncRequestValue.OID );

            // cookie is in the format <replicaId>;<Csn value>
            byte[] cookieBytes = syncControl.getCookie();

            if ( cookieBytes == null )
            {
                PROVIDER_LOG.debug( "Received a replication request with no cookie" );
                // No cookie ? We have to get all the entries from the provider
                // This is an initiate Content Poll action (RFC 4533, 3.3.1)
                doInitialRefresh( session, request );
            }
            else
            {
                String cookieString = Strings.utf8ToString( cookieBytes );

                PROVIDER_LOG.debug( "Received a replication request {} with a cookie '{}'", request, cookieString );

                if ( !LdapProtocolUtils.isValidCookie( cookieString ) )
                {
                    PROVIDER_LOG.error( "received an invalid cookie {} from the consumer with session {}",
                        cookieString,
                        session );
                    sendESyncRefreshRequired( session, request );
                }
                else
                {
                    ReplicaEventLog clientMsgLog = getReplicaEventLog( cookieString );

                    if ( clientMsgLog == null )
                    {
                        PROVIDER_LOG.debug(
                            "received a valid cookie {} but there is no event log associated with this replica",
                            cookieString );
                        sendESyncRefreshRequired( session, request );
                    }
                    else
                    {
                        String consumerCsn = LdapProtocolUtils.getCsn( cookieString );
                        doContentUpdate( session, request, clientMsgLog, consumerCsn );
                    }
                }
            }
        }
        catch ( Exception e )
        {
            PROVIDER_LOG.error( "Failed to handle the syncrepl request", e );

            throw new LdapException( e.getMessage(), e );
        }
    }


    /**
     * Send all the stored modifications to the consumer
     */
    private void sendContentFromLog( LdapSession session, SearchRequest req, ReplicaEventLog clientMsgLog,
        String fromCsn )
        throws Exception
    {
        // do the search from the log
        String lastSentCsn = fromCsn;

        ReplicaJournalCursor cursor = clientMsgLog.getCursor( fromCsn );

        PROVIDER_LOG.debug( "Processing the log for replica {}", clientMsgLog.getId() );

        try
        {
            while ( cursor.next() )
            {
                ReplicaEventMessage replicaEventMessage = cursor.get();
                Entry entry = replicaEventMessage.getEntry();
                PROVIDER_LOG.debug( "Read message from the queue {}", entry );

                lastSentCsn = entry.get( csnAT ).getString();

                ChangeType changeType = replicaEventMessage.getChangeType();

                SyncStateTypeEnum syncStateType = null;

                switch ( changeType )
                {
                    case ADD:
                        syncStateType = SyncStateTypeEnum.ADD;
                        break;

                    case MODIFY:
                        syncStateType = SyncStateTypeEnum.MODIFY;
                        break;

                    case MODDN:
                        syncStateType = SyncStateTypeEnum.MODDN;
                        break;

                    case DELETE:
                        syncStateType = SyncStateTypeEnum.DELETE;
                        break;

                    default:
                        throw new IllegalStateException( I18n.err( I18n.ERR_686 ) );
                }

                sendSearchResultEntry( session, req, entry, syncStateType );

                clientMsgLog.setLastSentCsn( lastSentCsn );

                PROVIDER_LOG.debug( "The latest entry sent to the consumer {} has this CSN : {}", clientMsgLog.getId(),
                    lastSentCsn );
            }

            PROVIDER_LOG.debug( "All pending modifciations for replica {} processed", clientMsgLog.getId() );
        }
        finally
        {
            cursor.close();
        }
    }


    /**
     * process the update of the consumer, starting from the given LastEntryCSN the consumer
     * has sent with the sync request.
     */
    private void doContentUpdate( LdapSession session, SearchRequest req, ReplicaEventLog replicaLog, String consumerCsn )
        throws Exception
    {
        synchronized ( replicaLog )
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

            sendContentFromLog( session, req, replicaLog, consumerCsn );

            String lastSentCsn = replicaLog.getLastSentCsn();

            byte[] cookie = LdapProtocolUtils.createCookie( replicaLog.getId(), lastSentCsn );

            if ( refreshNPersist )
            {
                IntermediateResponse intermResp = new IntermediateResponseImpl( req.getMessageId() );
                intermResp.setResponseName( SyncInfoValue.OID );

                SyncInfoValue syncInfo = new SyncInfoValueDecorator( ldapServer.getDirectoryService()
                    .getLdapCodecService(),
                    SynchronizationInfoEnum.NEW_COOKIE );
                syncInfo.setCookie( cookie );
                intermResp.setResponseValue( ( ( SyncInfoValueDecorator ) syncInfo ).getValue() );

                PROVIDER_LOG.debug( "Sent the intermediate response to the {} consumer, {}", replicaLog.getId(),
                    intermResp );
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

                PROVIDER_LOG.debug( "Send a SearchResultDone response to the {} consumer", replicaLog.getId(),
                    searchDoneResp );

                session.getIoSession().write( searchDoneResp );
            }
        }
    }


    /**
     * Process the initial refresh : we will send all the entries
     */
    private void doInitialRefresh( LdapSession session, SearchRequest request ) throws Exception
    {
        PROVIDER_LOG.debug( "Starting an initial refresh" );

        SortRequest ctrl = ( SortRequest ) request.getControl( SortRequest.OID );

        if ( ctrl != null )
        {
            PROVIDER_LOG
                .warn( "Removing the received sort control from the syncrepl search request during initial refresh" );
            request.removeControl( ctrl );
        }

        PROVIDER_LOG
            .debug( "Adding sort control to sort the entries by entryDn attribute to preserve order of insertion" );
        SortKey sk = new SortKey( SchemaConstants.ENTRY_DN_AT );
        // matchingrule for "entryDn"
        sk.setMatchingRuleId( "2.5.13.1" );
        sk.setReverseOrder( true );

        ctrl = new SortRequestControlImpl();
        ctrl.addSortKey( sk );

        request.addControl( ctrl );

        String originalFilter = request.getFilter().toString();
        InetSocketAddress address = ( InetSocketAddress ) session.getIoSession().getRemoteAddress();
        String hostName = address.getAddress().getHostName();

        ExprNode modifiedFilter = modifyFilter( session, request );

        Partition partition = dirService.getPartitionNexus().getPartition( request.getBase() );
        String contextCsn = partition.getContextCsn();

        boolean refreshNPersist = isRefreshNPersist( request );

        // first register a ReplicaEventLog before starting the initial content refresh
        // this is to log all the operations happen on DIT during initial content refresh
        ReplicaEventLog replicaLog = createReplicaEventLog( hostName, originalFilter );

        replicaLog.setRefreshNPersist( refreshNPersist );
        StringValue contexCsnValue = new StringValue( contextCsn );

        // modify the filter to include the context Csn
        GreaterEqNode csnGeNode = new GreaterEqNode( csnAT, contexCsnValue );
        ExprNode postInitContentFilter = new AndNode( modifiedFilter, csnGeNode );
        request.setFilter( postInitContentFilter );

        // now we process entries forever as they change
        // irrespective of the sync mode set the 'isRealtimePush' to false initially so that we can
        // store the modifications in the queue and later if it is a persist mode
        PROVIDER_LOG.debug( "Starting the replicaLog {}", replicaLog );

        // we push this queue's content and switch to realtime mode
        SyncReplSearchListener replicationListener = new SyncReplSearchListener( session, request, replicaLog, false );
        replicaLog.setPersistentListener( replicationListener );

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

        dirService.getEventService().addListener( replicationListener, criteria );

        // then start pushing initial content
        LessEqNode csnNode = new LessEqNode( csnAT, contexCsnValue );

        // modify the filter to include the context Csn
        ExprNode initialContentFilter = new AndNode( modifiedFilter, csnNode );
        request.setFilter( initialContentFilter );

        // Now, do a search to get all the entries
        SearchResultDone searchDoneResp = doSimpleSearch( session, request, replicaLog );

        if ( searchDoneResp.getLdapResult().getResultCode() == ResultCodeEnum.SUCCESS )
        {
            if ( replicaLog.getLastSentCsn() == null )
            {
                replicaLog.setLastSentCsn( contextCsn );
            }

            if ( refreshNPersist ) // refreshAndPersist mode
            {
                PROVIDER_LOG
                    .debug( "Refresh&Persist requested : send the data being modified since the initial refresh" );
                // Now, send the modified entries since the search has started
                sendContentFromLog( session, request, replicaLog, contextCsn );

                byte[] cookie = LdapProtocolUtils.createCookie( replicaLog.getId(), replicaLog.getLastSentCsn() );

                IntermediateResponse intermResp = new IntermediateResponseImpl( request.getMessageId() );
                intermResp.setResponseName( SyncInfoValue.OID );

                SyncInfoValue syncInfo = new SyncInfoValueDecorator(
                    ldapServer.getDirectoryService().getLdapCodecService(), SynchronizationInfoEnum.NEW_COOKIE );
                syncInfo.setCookie( cookie );
                intermResp.setResponseValue( ( ( SyncInfoValueDecorator ) syncInfo ).getValue() );

                PROVIDER_LOG.info( "Sending the intermediate response to consumer {}, {}", replicaLog, syncInfo );

                session.getIoSession().write( intermResp );

                // switch the handler mode to realtime push
                replicationListener.setPushInRealTime( refreshNPersist );
                PROVIDER_LOG.debug( "e waiting for any modification for {}", replicaLog );
            }
            else
            {
                PROVIDER_LOG.debug( "RefreshOnly requested" );
                byte[] cookie = LdapProtocolUtils.createCookie( replicaLog.getId(), contextCsn );

                // no need to send from the log, that will be done in the next refreshOnly session
                SyncDoneValue syncDone = new SyncDoneValueDecorator(
                    ldapServer.getDirectoryService().getLdapCodecService() );
                syncDone.setCookie( cookie );
                searchDoneResp.addControl( syncDone );
                PROVIDER_LOG.info( "Sending the searchResultDone response to consumer {}, {}", replicaLog,
                    searchDoneResp );

                session.getIoSession().write( searchDoneResp );
            }
        }
        else
        // if not succeeded return
        {
            PROVIDER_LOG.warn( "initial content refresh didn't succeed due to {}", searchDoneResp.getLdapResult()
                .getResultCode() );
            replicaLog.stop();
            replicaLog = null;

            // remove the listener
            dirService.getEventService().removeListener( replicationListener );

            return;
        }

        // if all is well then store the consumer information
        replicaUtil.addConsumerEntry( replicaLog );

        // add to the map only after storing in the DIT, else the Replica update thread barfs
        replicaLogMap.put( replicaLog.getId(), replicaLog );
    }


    /**
     * Process a search on the provider to get all the modified entries. We then send all
     * of them to the consumer
     */
    private SearchResultDone doSimpleSearch( LdapSession session, SearchRequest req, ReplicaEventLog replicaLog )
        throws Exception
    {
        PROVIDER_LOG.debug( "Simple Search {} for {}", req, session );
        SearchResultDone searchDoneResp = ( SearchResultDone ) req.getResultResponse();
        LdapResult ldapResult = searchDoneResp.getLdapResult();

        // A normal search
        // Check that we have a cursor or not.
        // No cursor : do a search.
        Cursor<Entry> cursor = session.getCoreSession().search( req );

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
            PROVIDER_LOG.debug( "search operation requested size limit {}, server size limit {}", requestLimit,
                serverLimit );
            long sizeLimit = min( requestLimit, serverLimit );

            readResults( session, req, ldapResult, cursor, sizeLimit, replicaLog );
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
                    PROVIDER_LOG.error( I18n.err( I18n.ERR_168 ), e );
                }
            }
        }

        PROVIDER_LOG.debug( "Search done" );

        return searchDoneResp;
    }


    /**
     * Process the results get from a search request. We will send them to the client.
     */
    private void readResults( LdapSession session, SearchRequest req, LdapResult ldapResult,
        Cursor<Entry> cursor, long sizeLimit, ReplicaEventLog replicaLog ) throws Exception
    {
        long count = 0;

        while ( ( count < sizeLimit ) && cursor.next() )
        {
            // Handle closed session
            if ( session.getIoSession().isClosing() )
            {
                // The client has closed the connection
                PROVIDER_LOG.debug( "Request terminated for message {}, the client has closed the session",
                    req.getMessageId() );
                break;
            }

            if ( req.isAbandoned() )
            {
                // The cursor has been closed by an abandon request.
                PROVIDER_LOG.debug( "Request terminated by an AbandonRequest for message {}", req.getMessageId() );
                break;
            }

            Entry entry = cursor.get();

            sendSearchResultEntry( session, req, entry, SyncStateTypeEnum.ADD );

            String lastSentCsn = entry.get( csnAT ).getString();
            replicaLog.setLastSentCsn( lastSentCsn );

            count++;
        }

        PROVIDER_LOG.debug( "Sent {} entries for {}", count, replicaLog );

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

        PROVIDER_LOG.debug( "Sending the entry:\n {}", resp );
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
                    PROVIDER_LOG.error( I18n.err( I18n.ERR_165, url, entry ) );
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
        final Cursor<Entry> cursor )
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


    public ReplicaEventLogJanitor getLogJanitor()
    {
        return logJanitor;
    }


    public Map<Integer, ReplicaEventLog> getReplicaLogMap()
    {
        return replicaLogMap;
    }


    private EqualityNode<String> newIsReferralEqualityNode( LdapSession session ) throws Exception
    {
        EqualityNode<String> ocIsReferral = new EqualityNode<String>( SchemaConstants.OBJECT_CLASS_AT, new StringValue(
            objectClassAT, SchemaConstants.REFERRAL_OC ) );

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
                    PROVIDER_LOG.debug( "updating the details of replica {}", replica );
                    replicaUtil.updateReplicaLastSentCsn( replica );
                    replica.setDirty( false );
                }
            }
        }
        catch ( Exception e )
        {
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
            Set<String> eventLogNames = new HashSet<String>();

            if ( !eventLogs.isEmpty() )
            {
                for ( ReplicaEventLog replica : eventLogs )
                {
                    PROVIDER_LOG.debug( "initializing the replica log from {}", replica.getId() );
                    replicaLogMap.put( replica.getId(), replica );
                    eventLogNames.add( replica.getName() );

                    // update the replicaCount's value to assign a correct value to the new replica(s)
                    if ( replicaCount.get() < replica.getId() )
                    {
                        replicaCount.set( replica.getId() );
                    }
                }
            }
            else
            {
                PROVIDER_LOG.debug( "no replica logs found to initialize" );
            }

            // remove unused logs
            for ( File f : getAllReplJournalNames() )
            {
                if ( !eventLogNames.contains( f.getName() ) )
                {
                    f.delete();
                    PROVIDER_LOG.info( "removed unused replication event log {}", f );
                }
            }
        }
        catch ( Exception e )
        {
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
                PROVIDER_LOG.debug( "registering persistent search for the replica {}", log.getId() );
                SyncReplSearchListener handler = new SyncReplSearchListener( null, null, log, false );
                log.setPersistentListener( handler );

                dirService.getEventService().addListener( handler, log.getSearchCriteria() );
            }
            else
            {
                PROVIDER_LOG.warn( "invalid persistent search criteria {} for the replica {}", log.getSearchCriteria(),
                    log
                        .getId() );
            }
        }
    }


    /**
     * Create a thread to process replication communication with a consumer
     */
    private Runnable createConsumerInfoUpdateTask( final CountDownLatch latch )
    {
        Runnable task = new Runnable()
        {
            public void run()
            {
                try
                {
                    while ( true )
                    {
                        storeReplicaInfo();
                        
                        latch.countDown();
                        Thread.sleep( 10000 );
                    }
                }
                catch ( InterruptedException e )
                {
                    // log at debug level, this will be interrupted during stop
                    PROVIDER_LOG.debug( "thread storing the replica information was interrupted", e );
                }
            }
        };

        return task;
    }


    /**
     * Get the Replica event log from the replica ID found in the cookie
     */
    private ReplicaEventLog getReplicaEventLog( String cookieString ) throws Exception
    {
        ReplicaEventLog replicaLog = null;

        if ( LdapProtocolUtils.isValidCookie( cookieString ) )
        {
            int clientId = LdapProtocolUtils.getReplicaId( cookieString );
            replicaLog = replicaLogMap.get( clientId );
        }

        return replicaLog;
    }


    /**
     * Create a new ReplicaEventLog. Each replica will have a unique ID, created by the provider.
     */
    private ReplicaEventLog createReplicaEventLog( String hostName, String filter ) throws Exception
    {
        int replicaId = replicaCount.incrementAndGet();

        PROVIDER_LOG.debug( "creating a new event log for the replica with id {}", replicaId );

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


    private File[] getAllReplJournalNames()
    {
        File replDir = dirService.getInstanceLayout().getReplDirectory();
        FilenameFilter filter = new FilenameFilter()
        {
            @Override
            public boolean accept( File dir, String name )
            {
                return name.startsWith( ReplicaEventLog.REPLICA_EVENT_LOG_NAME_PREFIX );
            }
        };

        return replDir.listFiles( filter );
    }

    /**
     * an event listener for handling deletions and updates of replication event log entries present under ou=consumers,ou=system
     */
    private class ConsumerLogEntryChangeListener extends DirectoryListenerAdapter
    {

        private ReplicaEventLog getEventLog( OperationContext opCtx )
        {
            Dn consumerLogDn = opCtx.getDn();
            String name = ReplicaEventLog.REPLICA_EVENT_LOG_NAME_PREFIX + consumerLogDn.getRdn().getValue().getString();

            for ( ReplicaEventLog log : replicaLogMap.values() )
            {
                if ( name.equalsIgnoreCase( log.getName() ) )
                {
                    return log;
                }
            } // end of for

            return null;
        }


        @Override
        public void entryDeleted( DeleteOperationContext deleteContext )
        {
            // lock this listener instance
            synchronized ( this )
            {
                ReplicaEventLog log = getEventLog( deleteContext );
                if ( log != null )
                {
                    logJanitor.removeEventLog( log );
                }
            } // end of synchronized block
        } // end of delete method


        @Override
        public void entryModified( ModifyOperationContext modifyContext )
        {
            List<Modification> mods = modifyContext.getModItems();

            // lock this listener instance
            synchronized ( this )
            {
                for ( Modification m : mods )
                {
                    try
                    {
                        Attribute at = m.getAttribute();

                        if ( at.isInstanceOf( replLogMaxIdleAT ) )
                        {
                            ReplicaEventLog log = getEventLog( modifyContext );
                            if ( log != null )
                            {
                                int maxIdlePeriod = Integer.parseInt( m.getAttribute().getString() );
                                log.setMaxIdlePeriod( maxIdlePeriod );
                            }
                        }
                        else if ( at.isInstanceOf( replLogPurgeThresholdCountAT ) )
                        {
                            ReplicaEventLog log = getEventLog( modifyContext );
                            if ( log != null )
                            {
                                int purgeThreshold = Integer.parseInt( m.getAttribute().getString() );
                                log.setPurgeThresholdCount( purgeThreshold );
                            }
                        }
                    }
                    catch ( LdapInvalidAttributeValueException e )
                    {
                        PROVIDER_LOG.warn( "Invalid attribute type", e );
                    }
                }
            }
        }
    } // end of listener class
}
