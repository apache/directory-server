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


import static java.lang.Math.min;
import static org.apache.directory.server.ldap.LdapServer.NO_SIZE_LIMIT;
import static org.apache.directory.server.ldap.LdapServer.NO_TIME_LIMIT;

import java.io.File;
import java.net.InetSocketAddress;
import java.net.URI;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import org.apache.activemq.ActiveMQConnection;
import org.apache.activemq.ActiveMQConnectionFactory;
import org.apache.activemq.broker.BrokerService;
import org.apache.directory.server.core.DirectoryService;
import org.apache.directory.server.core.entry.ClonedServerEntry;
import org.apache.directory.server.core.event.EventType;
import org.apache.directory.server.core.event.NotificationCriteria;
import org.apache.directory.server.core.filtering.EntryFilteringCursor;
import org.apache.directory.server.i18n.I18n;
import org.apache.directory.server.ldap.LdapServer;
import org.apache.directory.server.ldap.LdapSession;
import org.apache.directory.server.ldap.handlers.SearchAbandonListener;
import org.apache.directory.server.ldap.handlers.SearchTimeLimitingMonitor;
import org.apache.directory.shared.ldap.codec.controls.ManageDsaITControl;
import org.apache.directory.shared.ldap.codec.controls.replication.syncDoneValue.SyncDoneValueControl;
import org.apache.directory.shared.ldap.codec.controls.replication.syncInfoValue.SyncInfoValueControl;
import org.apache.directory.shared.ldap.codec.controls.replication.syncRequestValue.SyncRequestValueControl;
import org.apache.directory.shared.ldap.codec.controls.replication.syncStateValue.SyncStateValueControl;
import org.apache.directory.shared.ldap.codec.controls.replication.syncmodifydn.SyncModifyDnControl;
import org.apache.directory.shared.ldap.model.exception.LdapURLEncodingException;
import org.apache.directory.shared.ldap.model.constants.SchemaConstants;
import org.apache.directory.shared.ldap.csn.Csn;
import org.apache.directory.shared.ldap.model.entry.Entry;
import org.apache.directory.shared.ldap.model.entry.EntryAttribute;
import org.apache.directory.shared.ldap.model.entry.StringValue;
import org.apache.directory.shared.ldap.model.entry.Value;
import org.apache.directory.shared.ldap.model.exception.LdapException;
import org.apache.directory.shared.ldap.model.filter.*;
import org.apache.directory.shared.ldap.model.message.IntermediateResponse;
import org.apache.directory.shared.ldap.message.IntermediateResponseImpl;
import org.apache.directory.shared.ldap.model.message.LdapResult;
import org.apache.directory.shared.ldap.message.ReferralImpl;
import org.apache.directory.shared.ldap.model.message.Response;
import org.apache.directory.shared.ldap.model.message.*;
import org.apache.directory.shared.ldap.model.message.SearchRequest;
import org.apache.directory.shared.ldap.model.message.SearchResultDone;
import org.apache.directory.shared.ldap.model.message.SearchResultEntry;
import org.apache.directory.shared.ldap.message.SearchResultEntryImpl;
import org.apache.directory.shared.ldap.model.message.SearchResultReference;
import org.apache.directory.shared.ldap.message.SearchResultReferenceImpl;
import org.apache.directory.shared.ldap.message.control.replication.SyncStateTypeEnum;
import org.apache.directory.shared.ldap.message.control.replication.SynchronizationInfoEnum;
import org.apache.directory.shared.ldap.message.control.replication.SynchronizationModeEnum;
import org.apache.directory.shared.ldap.schema.AttributeType;
import org.apache.directory.shared.ldap.model.filter.LdapURL;
import org.apache.directory.shared.util.Strings;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * NOTE: doco is missing at many parts. Will be added once the functionality is satisfactory
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 */
@SuppressWarnings("unchecked")
public class SyncReplProvider implements ReplicationProvider
{

    public static final String REPLICA_ID_DELIM = ";";

    private static final Logger LOG = LoggerFactory.getLogger( SyncReplProvider.class );

    private boolean initialized = false;

    private DirectoryService dirService;

    /** The reference on the Ldap server instance */
    protected LdapServer ldapServer;

    private AttributeType objectClassAttributeType;

    private Map<Integer, ReplicaEventLog> replicaLogMap = new HashMap<Integer, ReplicaEventLog>();

    private BrokerService brokerService;

    private ActiveMQConnection amqConnection;

    private File syncReplData;

    private AtomicInteger replicaCount = new AtomicInteger( 0 );

    private ReplicaDitStoreUtil replicaUtil;


    public SyncReplProvider()
    {
    }


    public void init( LdapServer server )
    {
        if ( initialized )
        {
            LOG.warn( "syncrepl provider was already initialized" );
            return;
        }
        try
        {
            LOG.info( "initializing the syncrepl provider" );

            this.ldapServer = server;
            this.dirService = server.getDirectoryService();

            File workDir = dirService.getInstanceLayout().getLogDirectory();
            syncReplData = new File( workDir, "syncrepl-data" );
            if ( !syncReplData.exists() )
            {
                syncReplData.mkdirs();
            }

            String path = syncReplData.getPath();

            brokerService = new BrokerService();
            brokerService.setUseJmx( false );
            brokerService.setPersistent( true );
            brokerService.setDataDirectory( path );

            URI vmConnectorUri = new URI( "vm://localhost" );
            brokerService.setVmConnectorURI( vmConnectorUri );

            brokerService.start();
            ActiveMQConnectionFactory amqFactory = new ActiveMQConnectionFactory( vmConnectorUri.toString() );
            amqFactory.setObjectMessageSerializationDefered( false );

            amqConnection = ( ActiveMQConnection ) amqFactory.createConnection();
            amqConnection.start();

            // set the static reference to SchemaManager
            ReplicaEventMessage.setSchemaManager( dirService.getSchemaManager() );

            replicaUtil = new ReplicaDitStoreUtil( dirService );

            loadReplicaInfo();

            registerPersistentSearches();

            Thread consumerInfoUpdateThread = new Thread( createConsumerInfoUpdateTask() );
            consumerInfoUpdateThread.setDaemon( true );
            consumerInfoUpdateThread.start();

            initialized = true;
            LOG.info( "syncrepl provider initialized successfully" );
        }
        catch ( Exception e )
        {
            LOG.error( "Failed to initialize the log files required by the syncrepl provider", e );
            throw new RuntimeException( e );
        }
    }


    public void stop()
    {
        try
        {
            brokerService.stop();
            amqConnection.close();
        }
        catch ( Exception e )
        {
            LOG.warn( "Failed to close the message queue connection", e );
        }

        initialized = false;
    }


    public void handleSyncRequest( LdapSession session, SearchRequest req ) throws LdapException
    {
        try
        {
            SyncRequestValueControl syncControl = ( SyncRequestValueControl ) req.getControls().get(
                SyncRequestValueControl.CONTROL_OID );

            // cookie is in the format <replicaId>;<Csn value>
            byte[] cookieBytes = syncControl.getCookie();
            String cookieString = Strings.utf8ToString(cookieBytes);

            if ( cookieBytes == null )
            {
                doInitialRefresh( session, req );
            }
            else
            {
                LOG.warn( "search request received with the cookie {}", Strings.utf8ToString(cookieBytes) );
                if ( !isValidCookie( cookieString ) )
                {
                    LOG.error( "received a invalid cookie {} from the consumer with session {}", cookieString, session );
                    sendESyncRefreshRequired( session, req );
                }
                else
                {
                    ReplicaEventLog clientMsgLog = getReplicaEventLog( cookieString );
                    if ( clientMsgLog == null )
                    {
                        LOG.warn( "received a valid cookie {} but there is no event log associated with this replica",
                            cookieString );
                        sendESyncRefreshRequired( session, req );
                    }
                    else
                    {
                        doContentUpdate( session, req, clientMsgLog );
                    }
                }
            }
        }
        catch ( Exception e )
        {
            LOG.error( "Failed to handle the syncrepl request", e );

            LdapException le = new LdapException( e.getMessage() );
            le.initCause( e );

            throw le;
        }
    }


    private String sendContentFromLog( LdapSession session, SearchRequest req, ReplicaEventLog clientMsgLog )
        throws Exception
    {
        // do the search from the log
        String lastSentCsn = clientMsgLog.getLastSentCsn();

        ReplicaEventLogCursor cursor = clientMsgLog.getCursor();
        while ( cursor.next() )
        {
            ReplicaEventMessage message = cursor.get();
            Entry entry = message.getEntry();
            LOG.debug( "received message from the queue {}", entry );

            lastSentCsn = entry.get( SchemaConstants.ENTRY_CSN_AT ).getString();

            EventType event = message.getEventType();

            // if event type is null, then it is a MODDN operation
            if ( event == null )
            {
                sendSearchResultEntry( session, req, entry, message.getModDnControl() );
            }
            else
            {
                SyncStateTypeEnum syncStateType = null;
                if ( event == EventType.ADD || event == EventType.MODIFY )
                {
                    syncStateType = SyncStateTypeEnum.ADD;
                }
                else if ( event == EventType.DELETE )
                {
                    syncStateType = SyncStateTypeEnum.DELETE;
                }

                sendSearchResultEntry( session, req, entry, syncStateType );
            }
        }
        cursor.close();

        return lastSentCsn;
    }


    private void doContentUpdate( LdapSession session, SearchRequest req, ReplicaEventLog replicaLog )
        throws Exception
    {
        boolean refreshNPersist = isRefreshNPersist( req );

        // if this method is called with refreshAndPersist  
        // means the client was offline after it initiated a persistent synch session
        // we need to update the handler's session 
        if ( refreshNPersist )
        {
            SyncReplSearchListener handler = replicaLog.getPersistentListener();
            handler.setReq( req );
            handler.setSession( session );
        }

        String lastSentCsn = sendContentFromLog( session, req, replicaLog );

        byte[] cookie = Strings.getBytesUtf8(replicaLog.getId() + REPLICA_ID_DELIM + lastSentCsn);

        if ( refreshNPersist )
        {
            IntermediateResponse intermResp = new IntermediateResponseImpl( req.getMessageId() );
            intermResp.setResponseName( SyncInfoValueControl.CONTROL_OID );

            SyncInfoValueControl syncInfo = new SyncInfoValueControl( SynchronizationInfoEnum.NEW_COOKIE );
            syncInfo.setCookie( cookie );
            intermResp.setResponseValue( syncInfo.getValue() );

            session.getIoSession().write( intermResp );

            replicaLog.getPersistentListener().setPushInRealTime( refreshNPersist );
        }
        else
        {
            SearchResultDone searchDoneResp = ( SearchResultDone ) req.getResultResponse();
            searchDoneResp.getLdapResult().setResultCode( ResultCodeEnum.SUCCESS );
            SyncDoneValueControl syncDone = new SyncDoneValueControl();
            syncDone.setCookie( cookie );
            searchDoneResp.addControl( syncDone );

            session.getIoSession().write( searchDoneResp );
        }

        replicaLog.setLastSentCsn( lastSentCsn );
    }


    private void doInitialRefresh( LdapSession session, SearchRequest req ) throws Exception
    {

        String originalFilter = req.getFilter().toString();
        InetSocketAddress address = ( InetSocketAddress ) session.getIoSession().getRemoteAddress();
        String hostName = address.getAddress().getHostName();

        ExprNode modifiedFilter = modifyFilter( session, req );

        String contextCsn = dirService.getContextCsn();

        boolean refreshNPersist = isRefreshNPersist( req );

        // first register a persistent search handler before starting the initial content refresh
        // this is to log all the operations happen on DIT during initial content refresh

        ReplicaEventLog replicaLog = createRelicaEventLog( hostName, originalFilter );

        replicaLog.setRefreshNPersist( refreshNPersist );

        // modify the filter to include the context Csn
        GreaterEqNode csnGeNode = new GreaterEqNode( SchemaConstants.ENTRY_CSN_AT, new StringValue( contextCsn ) );
        ExprNode postInitContentFilter = new AndNode( modifiedFilter, csnGeNode );
        req.setFilter( postInitContentFilter );

        // now we process entries forever as they change
        LOG.info( "starting persistent search for the client {}", replicaLog );

        // irrespective of the sync mode set the 'isRealtimePush' to false initially so that we can
        // store the modifications in the queue and later if it is a persist mode
        // we push this queue's content and switch to realtime mode
        SyncReplSearchListener handler = new SyncReplSearchListener( session, req, replicaLog, false );
        replicaLog.setPersistentListener( handler );

        // compose notification criteria and add the listener to the event 
        // service using that notification criteria to determine which events 
        // are to be delivered to the persistent search issuing client
        NotificationCriteria criteria = new NotificationCriteria();
        criteria.setAliasDerefMode( req.getDerefAliases() );
        criteria.setBase( req.getBase() );
        criteria.setFilter( req.getFilter() );
        criteria.setScope( req.getScope() );
        criteria.setEventMask( EventType.ALL_EVENT_TYPES_MASK );

        replicaLog.setSearchCriteria( criteria );

        dirService.getEventService().addListener( handler, criteria );

        // then start pushing initial content
        LessEqNode csnNode = new LessEqNode( SchemaConstants.ENTRY_CSN_AT, new StringValue( contextCsn ) );

        // modify the filter to include the context Csn
        ExprNode initialContentFilter = new AndNode( modifiedFilter, csnNode );
        req.setFilter( initialContentFilter );

        SearchResultDone searchDoneResp = doSimpleSearch( session, req );

        if ( searchDoneResp.getLdapResult().getResultCode() == ResultCodeEnum.SUCCESS )
        {
            replicaLog.setLastSentCsn( contextCsn );
            byte[] cookie = Strings.getBytesUtf8(replicaLog.getId() + REPLICA_ID_DELIM + contextCsn);

            if ( refreshNPersist ) // refreshAndPersist mode
            {
                contextCsn = sendContentFromLog( session, req, replicaLog );
                cookie = Strings.getBytesUtf8(replicaLog.getId() + REPLICA_ID_DELIM + contextCsn);

                IntermediateResponse intermResp = new IntermediateResponseImpl( req.getMessageId() );
                intermResp.setResponseName( SyncInfoValueControl.CONTROL_OID );

                SyncInfoValueControl syncInfo = new SyncInfoValueControl( SynchronizationInfoEnum.NEW_COOKIE );
                syncInfo.setCookie( cookie );
                intermResp.setResponseValue( syncInfo.getValue() );

                session.getIoSession().write( intermResp );

                // switch the handler mode to realtime push
                handler.setPushInRealTime( refreshNPersist );
            }
            else
            {
                // no need to send from the log, that will be done in the next refreshOnly session
                SyncDoneValueControl syncDone = new SyncDoneValueControl();
                syncDone.setCookie( cookie );
                searchDoneResp.addControl( syncDone );
                session.getIoSession().write( searchDoneResp );
            }
        }
        else
        // if not succeeded return
        {
            LOG.warn( "initial content refresh didn't succeed due to {}", searchDoneResp.getLdapResult()
                .getResultCode() );
            replicaLog.truncate();
            replicaLog = null;

            // remove the listener
            dirService.getEventService().removeListener( handler );

            return;
        }

        // if all is well then store the consumer infor
        replicaUtil.addConsumerEntry( replicaLog );

        // add to the map only after storing in the DIT, else the Replica update thread barfs
        replicaLogMap.put( replicaLog.getId(), replicaLog );
    }


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
                break;
            }

            if ( req.isAbandoned() )
            {
                // The cursor has been closed by an abandon request.
                LOG.debug( "Request terminated by an AbandonRequest for message {}", req.getMessageId() );
                break;
            }

            ClonedServerEntry entry = cursor.get();

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


    private void sendSearchResultEntry( LdapSession session, SearchRequest req, Entry entry,
        SyncStateTypeEnum syncStateType ) throws Exception
    {

        EntryAttribute uuid = entry.get( SchemaConstants.ENTRY_UUID_AT );
        SyncStateValueControl syncStateControl = new SyncStateValueControl();
        syncStateControl.setSyncStateType( syncStateType );
        syncStateControl.setEntryUUID( Strings.uuidToBytes(uuid.getString()) );

        if ( syncStateType == SyncStateTypeEnum.DELETE )
        {
            // clear the entry's all attributes except the Dn and entryUUID
            entry.clear();
            entry.add( uuid );
        }

        Response resp = generateResponse( session, req, entry );
        resp.addControl( syncStateControl );

        session.getIoSession().write( resp );
        LOG.debug( "Sending {}", entry.getDn() );
    }


    private void sendSearchResultEntry( LdapSession session, SearchRequest req, Entry entry,
        SyncModifyDnControl modDnControl ) throws Exception
    {

        EntryAttribute uuid = entry.get( SchemaConstants.ENTRY_UUID_AT );
        SyncStateValueControl syncStateControl = new SyncStateValueControl();
        syncStateControl.setSyncStateType( SyncStateTypeEnum.MODDN );
        syncStateControl.setEntryUUID( Strings.uuidToBytes(uuid.getString()) );

        Response resp = generateResponse( session, req, entry );
        resp.addControl( syncStateControl );
        resp.addControl( modDnControl );

        session.getIoSession().write( resp );
        LOG.debug( "Sending {}", entry.getDn() );
    }


    private Response generateResponse( LdapSession session, SearchRequest req, Entry entry ) throws Exception
    {
        EntryAttribute ref = entry.get( SchemaConstants.REF_AT );
        boolean hasManageDsaItControl = req.getControls().containsKey( ManageDsaITControl.CONTROL_OID );

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

                LdapURL ldapUrl = new LdapURL();
                ldapUrl.setForceScopeRendering( true );
                try
                {
                    ldapUrl.parse( url.toCharArray() );
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

            // Filter the userPassword if the server mandate to do so
            if ( session.getCoreSession().getDirectoryService().isPasswordHidden() )
            {
                // Remove the userPassord attribute from the entry.
                respEntry.getEntry().removeAttributes( SchemaConstants.USER_PASSWORD_AT );
            }

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
         * Do not add the OR'd (objectClass=referral) expression if the user 
         * searches for the subSchemaSubEntry as the SchemaIntercepter can't 
         * handle an OR'd filter.
         */
        //        if ( isSubSchemaSubEntrySearch( session, req ) )
        //        {
        //            return;
        //        }

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
        if ( !req.hasControl( ManageDsaITControl.CONTROL_OID ) && !isOcPresenceFilter )
        {
            filter = new OrNode( req.getFilter(), newIsReferralEqualityNode( session ) );
        }

        return filter;
    }


    private EqualityNode<String> newIsReferralEqualityNode( LdapSession session ) throws Exception
    {
        if ( objectClassAttributeType == null )
        {
            objectClassAttributeType = session.getCoreSession().getDirectoryService().getSchemaManager()
                .lookupAttributeTypeRegistry( SchemaConstants.OBJECT_CLASS_AT );
        }

        EqualityNode<String> ocIsReferral = new EqualityNode<String>( SchemaConstants.OBJECT_CLASS_AT, new StringValue(
            objectClassAttributeType, SchemaConstants.REFERRAL_OC ) );

        return ocIsReferral;
    }


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
                    replicaUtil.updateReplicaLastSentCsn( replica );
                    replica.setDirty( false );
                }
            }
        }
        catch ( Exception e )
        {
            LOG.error( "Failed to store the replica information", e );
        }
    }


    private void loadReplicaInfo()
    {
        try
        {

            List<ReplicaEventLog> replicas = replicaUtil.getReplicaConsumers();
            if ( !replicas.isEmpty() )
            {
                for ( ReplicaEventLog r : replicas )
                {
                    LOG.debug( "initializing the replica log from {}", r.getId() );
                    r.configure( amqConnection, brokerService );
                    replicaLogMap.put( r.getId(), r );

                    // update the replicaCount's value to assign a correct value to the new replica(s) 
                    if ( replicaCount.get() < r.getId() )
                    {
                        replicaCount.set( r.getId() );
                    }
                }
            }
            else
            {
                LOG.debug( "no replica logs found to initialize" );
            }
        }
        catch ( Exception e )
        {
            LOG.error( "Failed to load the replica information", e );
        }
    }


    private void registerPersistentSearches() throws Exception
    {
        for ( Map.Entry<Integer, ReplicaEventLog> e : replicaLogMap.entrySet() )
        {
            ReplicaEventLog log = e.getValue();

            if ( log.getSearchCriteria() != null )
            {
                LOG.debug( "registering peristent search for the replica {}", log.getId() );
                SyncReplSearchListener handler = new SyncReplSearchListener( null, null, log, false );
                log.setPersistentListener( handler );

                dirService.getEventService().addListener( handler, log.getSearchCriteria() );
            }
            else
            {
                LOG.warn( "invalid peristent search criteria {} for the replica {}", log.getSearchCriteria(), log
                    .getId() );
            }
        }
    }


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
                    }
                }
            }
        };

        return task;
    }


    private boolean isValidCookie( String cookieString )
    {
        if ( cookieString == null || cookieString.trim().length() == 0 )
        {
            return false;
        }

        int pos = cookieString.indexOf( REPLICA_ID_DELIM );
        if ( pos <= 0 ) // position should start from 1 or higher cause a cookie can be like "0;<csn>" or "11;<csn>"
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


    private int getReplicaId( String cookieString )
    {
        String replicaId = cookieString.substring( 0, cookieString.indexOf( REPLICA_ID_DELIM ) );
        return Integer.parseInt( replicaId );
    }


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


    private ReplicaEventLog createRelicaEventLog( String hostName, String filter ) throws Exception
    {
        int replicaId = replicaCount.incrementAndGet();

        LOG.debug( "creating a new event log for the replica with id {}", replicaId );

        ReplicaEventLog replicaLog = new ReplicaEventLog( replicaId );
        replicaLog.setHostName( hostName );
        replicaLog.setSearchFilter( filter );

        replicaLog.configure( amqConnection, brokerService );

        return replicaLog;
    }


    private void sendESyncRefreshRequired( LdapSession session, SearchRequest req ) throws Exception
    {
        SearchResultDone searchDoneResp = ( SearchResultDone ) req.getResultResponse();
        searchDoneResp.getLdapResult().setResultCode( ResultCodeEnum.E_SYNC_REFRESH_REQUIRED );
        SyncDoneValueControl syncDone = new SyncDoneValueControl();
        searchDoneResp.addControl( syncDone );

        session.getIoSession().write( searchDoneResp );
    }


    private boolean isRefreshNPersist( SearchRequest req )
    {
        SyncRequestValueControl control = ( SyncRequestValueControl ) req.getControls().get(
            SyncRequestValueControl.CONTROL_OID );
        return ( control.getMode() == SynchronizationModeEnum.REFRESH_AND_PERSIST ? true : false );
    }
}
