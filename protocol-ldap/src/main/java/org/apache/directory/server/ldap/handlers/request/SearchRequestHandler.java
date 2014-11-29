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
package org.apache.directory.server.ldap.handlers.request;


import static java.lang.Math.min;
import static org.apache.directory.server.ldap.LdapServer.NO_SIZE_LIMIT;
import static org.apache.directory.server.ldap.LdapServer.NO_TIME_LIMIT;

import java.util.Collection;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import org.apache.commons.lang.exception.ExceptionUtils;
import org.apache.directory.api.ldap.codec.controls.search.pagedSearch.PagedResultsDecorator;
import org.apache.directory.api.ldap.extras.controls.syncrepl.syncInfoValue.SyncRequestValue;
import org.apache.directory.api.ldap.model.constants.SchemaConstants;
import org.apache.directory.api.ldap.model.cursor.Cursor;
import org.apache.directory.api.ldap.model.cursor.CursorClosedException;
import org.apache.directory.api.ldap.model.entry.Attribute;
import org.apache.directory.api.ldap.model.entry.Entry;
import org.apache.directory.api.ldap.model.entry.Value;
import org.apache.directory.api.ldap.model.exception.LdapException;
import org.apache.directory.api.ldap.model.exception.LdapOperationException;
import org.apache.directory.api.ldap.model.exception.LdapURLEncodingException;
import org.apache.directory.api.ldap.model.exception.OperationAbandonedException;
import org.apache.directory.api.ldap.model.filter.EqualityNode;
import org.apache.directory.api.ldap.model.filter.ExprNode;
import org.apache.directory.api.ldap.model.filter.OrNode;
import org.apache.directory.api.ldap.model.filter.PresenceNode;
import org.apache.directory.api.ldap.model.message.Control;
import org.apache.directory.api.ldap.model.message.LdapResult;
import org.apache.directory.api.ldap.model.message.Referral;
import org.apache.directory.api.ldap.model.message.ReferralImpl;
import org.apache.directory.api.ldap.model.message.Response;
import org.apache.directory.api.ldap.model.message.ResultCodeEnum;
import org.apache.directory.api.ldap.model.message.ResultResponseRequest;
import org.apache.directory.api.ldap.model.message.SearchRequest;
import org.apache.directory.api.ldap.model.message.SearchResultDone;
import org.apache.directory.api.ldap.model.message.SearchResultEntry;
import org.apache.directory.api.ldap.model.message.SearchResultEntryImpl;
import org.apache.directory.api.ldap.model.message.SearchResultReference;
import org.apache.directory.api.ldap.model.message.SearchResultReferenceImpl;
import org.apache.directory.api.ldap.model.message.SearchScope;
import org.apache.directory.api.ldap.model.message.controls.ManageDsaIT;
import org.apache.directory.api.ldap.model.message.controls.PagedResults;
import org.apache.directory.api.ldap.model.message.controls.PersistentSearch;
import org.apache.directory.api.ldap.model.name.Dn;
import org.apache.directory.api.ldap.model.schema.AttributeType;
import org.apache.directory.api.ldap.model.url.LdapUrl;
import org.apache.directory.api.util.Strings;
import org.apache.directory.server.core.api.DirectoryService;
import org.apache.directory.server.core.api.ReferralManager;
import org.apache.directory.server.core.api.entry.ClonedServerEntry;
import org.apache.directory.server.core.api.event.EventType;
import org.apache.directory.server.core.api.event.NotificationCriteria;
import org.apache.directory.server.core.api.filtering.EntryFilteringCursor;
import org.apache.directory.server.core.api.partition.PartitionNexus;
import org.apache.directory.server.i18n.I18n;
import org.apache.directory.server.ldap.LdapSession;
import org.apache.directory.server.ldap.handlers.LdapRequestHandler;
import org.apache.directory.server.ldap.handlers.PersistentSearchListener;
import org.apache.directory.server.ldap.handlers.SearchAbandonListener;
import org.apache.directory.server.ldap.handlers.SearchTimeLimitingMonitor;
import org.apache.directory.server.ldap.handlers.controls.PagedSearchContext;
import org.apache.directory.server.ldap.replication.provider.ReplicationRequestHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * A MessageReceived handler for processing search requests.
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 */
public class SearchRequestHandler extends LdapRequestHandler<SearchRequest>
{
    /** The logger */
    private static final Logger LOG = LoggerFactory.getLogger( SearchRequestHandler.class );

    private static final Logger SEARCH_TIME_LOG = LoggerFactory.getLogger( "org.apache.directory.server.ldap.handlers.request.SEARCH_TIME_LOG" );
    
    /** Speedup for logs */
    private static final boolean IS_DEBUG = LOG.isDebugEnabled();

    /** cached to save redundant lookups into registries */
    private AttributeType OBJECT_CLASS_AT;

    /** cached to save redundant lookups into registries */
    private AttributeType SUBSCHEMA_SUBENTRY_AT;

    /** The replication handler */
    protected ReplicationRequestHandler replicationReqHandler;


    /**
     * Constructs a new filter EqualityNode asserting that a candidate
     * objectClass is a referral.
     *
     * @param session the {@link LdapSession} to construct the node for
     * @return the {@link org.apache.directory.api.ldap.model.filter.EqualityNode} (objectClass=referral) non-normalized
     * @throws Exception in the highly unlikely event of schema related failures
     */
    private EqualityNode<String> newIsReferralEqualityNode( LdapSession session ) throws Exception
    {
        if ( OBJECT_CLASS_AT == null )
        {
            OBJECT_CLASS_AT = session.getCoreSession().getDirectoryService().getSchemaManager().getAttributeType(
                SchemaConstants.OBJECT_CLASS_AT );
        }

        EqualityNode<String> ocIsReferral = new EqualityNode<String>( OBJECT_CLASS_AT,
            new org.apache.directory.api.ldap.model.entry.StringValue( OBJECT_CLASS_AT, SchemaConstants.REFERRAL_OC ) );

        return ocIsReferral;
    }


    /**
     * Handles search requests containing the persistent search decorator but
     * delegates to doSimpleSearch() if the changesOnly parameter of the
     * decorator is set to false.
     *
     * @param session the LdapSession for which this search is conducted
     * @param req the search request containing the persistent search decorator
     * @param psearchDecorator the persistent search decorator extracted
     * @throws Exception if failures are encountered while searching
     */
    private void handlePersistentSearch( LdapSession session, SearchRequest req,
        PersistentSearch psearch ) throws Exception
    {
        /*
         * We want the search to complete first before we start listening to
         * events when the decorator does NOT specify changes ONLY mode.
         */
        if ( !psearch.isChangesOnly() )
        {
            SearchResultDone done = doSimpleSearch( session, req );

            // ok if normal search beforehand failed somehow quickly abandon psearch
            if ( done.getLdapResult().getResultCode() != ResultCodeEnum.SUCCESS )
            {
                session.getIoSession().write( done );
                return;
            }
        }

        if ( req.isAbandoned() )
        {
            return;
        }

        // now we process entries forever as they change
        PersistentSearchListener persistentSearchListener = new PersistentSearchListener( session, req );

        // compose notification criteria and add the listener to the event
        // service using that notification criteria to determine which events
        // are to be delivered to the persistent search issuing client
        NotificationCriteria criteria = new NotificationCriteria();
        criteria.setAliasDerefMode( req.getDerefAliases() );
        criteria.setBase( req.getBase() );
        criteria.setFilter( req.getFilter() );
        criteria.setScope( req.getScope() );
        criteria.setEventMask( EventType.getEventTypes( psearch.getChangeTypes() ) );
        getLdapServer().getDirectoryService().getEventService().addListener( persistentSearchListener, criteria );
        req.addAbandonListener( new SearchAbandonListener( ldapServer, persistentSearchListener ) );
    }


    /**
     * {@inheritDoc}
     */
    public final void handle( LdapSession session, SearchRequest req ) throws Exception
    {
        if ( IS_DEBUG )
        {
            LOG.debug( "Handling single reply request: {}", req );
        }

        // check first for the syncrepl search request decorator
        if ( req.getControls().containsKey( SyncRequestValue.OID ) )
        {
            handleReplication( session, req );
        }
        // if we have the ManageDSAIt decorator, go directly
        // to the handling without pre-processing the request
        else if ( req.getControls().containsKey( ManageDsaIT.OID ) )
        {
            // If the ManageDsaIT decorator is present, we will
            // consider that the user wants to get entry which
            // are referrals as plain entry. We have to return
            // SearchResponseEntry elements instead of
            // SearchResponseReference elements.
            LOG.debug( "ManageDsaITControl detected." );
            handleIgnoringReferrals( session, req );
        }
        else
        {
            // No ManageDsaIT decorator. If the found entries is a referral,
            // we will return SearchResponseReference elements.
            LOG.debug( "ManageDsaITControl NOT detected." );

            switch ( req.getType() )
            {
                case SEARCH_REQUEST:
                    handleWithReferrals( session, req );
                    break;

                default:
                    throw new IllegalStateException( I18n.err( I18n.ERR_685, req ) );
            }
        }
    }


    /**
     * Handle the replication request.
     */
    private void handleReplication( LdapSession session, SearchRequest searchRequest ) throws LdapException
    {
        if ( replicationReqHandler != null )
        {
            replicationReqHandler.handleSyncRequest( session, searchRequest );
        }
        else
        {
            // Replication is not allowed on this server. generate a error message
            LOG.warn( "This server does not allow replication" );
            LdapResult result = searchRequest.getResultResponse().getLdapResult();

            result.setDiagnosticMessage( "Replication is not allowed on this server" );
            result.setResultCode( ResultCodeEnum.OTHER );
            session.getIoSession().write( searchRequest.getResultResponse() );

            return;
        }
    }


    /**
     * Handles a simple lookup, or a RootDSE lookup.
     *
     * @param session the LdapSession for which this search is conducted
     * @param req the search request on the RootDSE
     * @throws Exception if failures are encountered while searching
     */
    private void handleLookup( LdapSession session, SearchRequest req ) throws Exception
    {
        try
        {
            Map<String, Control> controlMap = req.getControls();
            Control[] controls = null;

            if ( controlMap != null )
            {
                Collection<Control> controlValues = controlMap.values();

                controls = new Control[controlValues.size()];
                int pos = 0;

                for ( Control control : controlMap.values() )
                {
                    controls[pos++] = control;
                }
            }

            Entry entry = session.getCoreSession().lookup(
                req.getBase(),
                controls,
                req.getAttributes().toArray( new String[]
                    {} ) );

            session.getIoSession().write( generateResponse( session, req, entry ) );

            // write the SearchResultDone message
            session.getIoSession().write( req.getResultResponse() );
        }
        finally
        {
        }
    }


    /**
     * Based on the server maximum time limits configured for search and the
     * requested time limits this method determines if at all to replace the
     * default ClosureMonitor of the result set Cursor with one that closes
     * the Cursor when either server mandated or request mandated time limits
     * are reached.
     *
     * @param req the {@link SearchRequest} issued
     * @param session the {@link LdapSession} on which search was requested
     * @param cursor the {@link EntryFilteringCursor} over the search results
     */
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


    private void writeResults( LdapSession session, SearchRequest req, LdapResult ldapResult,
        Cursor<Entry> cursor, long sizeLimit ) throws Exception
    {
        long count = 0;

        while ( ( count < sizeLimit ) && cursor.next() )
        {
            // Handle closed session
            if ( session.getIoSession().isClosing() )
            {
                // The client has closed the connection
                if ( IS_DEBUG )
                {
                    LOG.debug( "Request terminated for message {}, the client has closed the session",
                        req.getMessageId() );
                }

                break;
            }

            if ( req.isAbandoned() )
            {
                cursor.close( new OperationAbandonedException() );

                // The cursor has been closed by an abandon request.
                if ( IS_DEBUG )
                {
                    LOG.debug( "Request terminated by an AbandonRequest for message {}", req.getMessageId() );
                }

                break;
            }

            Entry entry = cursor.get();
            session.getIoSession().write( generateResponse( session, req, entry ) );

            if ( IS_DEBUG )
            {
                LOG.debug( "Sending {}", entry.getDn() );
            }

            count++;
        }

        // check if the result code is not already set
        // the result code might be set when sort control is present
        if ( ldapResult.getResultCode() == null )
        {
            // DO NOT WRITE THE RESPONSE - JUST RETURN IT
            ldapResult.setResultCode( ResultCodeEnum.SUCCESS );
        }

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


    private void readPagedResults( LdapSession session, SearchRequest req, LdapResult ldapResult,
        Cursor<Entry> cursor, long sizeLimit, int pagedLimit, PagedSearchContext pagedContext,
        PagedResultsDecorator pagedResultsControl ) throws Exception
    {
        req.addAbandonListener( new SearchAbandonListener( ldapServer, cursor ) );
        setTimeLimitsOnCursor( req, session, cursor );

        if ( IS_DEBUG )
        {
            LOG.debug( "using <{},{}> for size limit", sizeLimit, pagedLimit );
        }

        int cookieValue = 0;

        int count = pagedContext.getCurrentPosition();
        int pageCount = 0;

        while ( ( count < sizeLimit ) && ( pageCount < pagedLimit ) && cursor.next() )
        {
            if ( session.getIoSession().isClosing() )
            {
                break;
            }

            Entry entry = cursor.get();
            session.getIoSession().write( generateResponse( session, req, entry ) );
            count++;
            pageCount++;
        }

        // DO NOT WRITE THE RESPONSE - JUST RETURN IT
        ldapResult.setResultCode( ResultCodeEnum.SUCCESS );

        boolean hasMoreEntry = cursor.next();

        // We have some entry, move back to the first one, as we just moved forward 
        // to get the first entry
        if ( hasMoreEntry )
        {
            cursor.previous();
        }

        if ( !hasMoreEntry )
        {
            // That means we don't have anymore entry
            // If we are here, it means we have returned all the entries
            // We have to remove the cookie from the session
            cookieValue = pagedContext.getCookieValue();
            PagedSearchContext psCookie = session.removePagedSearchContext( cookieValue );

            // Close the cursor if there is one
            if ( psCookie != null )
            {
                cursor = psCookie.getCursor();

                if ( cursor != null )
                {
                    cursor.close();
                }
            }

            pagedResultsControl = new PagedResultsDecorator( ldapServer.getDirectoryService()
                .getLdapCodecService() );
            pagedResultsControl.setCritical( true );
            pagedResultsControl.setSize( 0 );
            req.getResultResponse().addControl( pagedResultsControl );

            return;
        }
        else
        {
            // We have reached one limit

            if ( count < sizeLimit )
            {
                // We stop here. We have to add a ResponseControl
                // DO NOT WRITE THE RESPONSE - JUST RETURN IT
                ldapResult.setResultCode( ResultCodeEnum.SUCCESS );
                req.getResultResponse().addControl( pagedResultsControl );

                // Stores the cursor current position
                pagedContext.incrementCurrentPosition( pageCount );
                return;
            }
            else
            {
                // Return an exception, close the cursor, and clean the session
                ldapResult.setResultCode( ResultCodeEnum.SIZE_LIMIT_EXCEEDED );

                if ( cursor != null )
                {
                    cursor.close();
                }

                session.removePagedSearchContext( cookieValue );

                return;
            }
        }
    }


    /**
     * Manage the abandoned Paged Search (when paged size = 0). We have to
     * remove the cookie and its associated cursor from the session.
     */
    private SearchResultDone abandonPagedSearch( LdapSession session, SearchRequest req ) throws Exception
    {
        PagedResults pagedSearchControl = ( PagedResults ) req.getControls().get( PagedResults.OID );
        byte[] cookie = pagedSearchControl.getCookie();

        if ( !Strings.isEmpty( cookie ) )
        {
            // If the cookie is not null, we have to destroy the associated
            // cursor stored into the session (if any)
            int cookieValue = pagedSearchControl.getCookieValue();
            PagedSearchContext psCookie = session.removePagedSearchContext( cookieValue );
            pagedSearchControl.setCookie( psCookie.getCookie() );
            pagedSearchControl.setSize( 0 );
            pagedSearchControl.setCritical( true );

            // Close the cursor
            Cursor<Entry> cursor = psCookie.getCursor();

            if ( cursor != null )
            {
                cursor.close();
            }
        }
        else
        {
            pagedSearchControl.setSize( 0 );
            pagedSearchControl.setCritical( true );
        }

        // and return
        // DO NOT WRITE THE RESPONSE - JUST RETURN IT
        LdapResult ldapResult = req.getResultResponse().getLdapResult();
        ldapResult.setResultCode( ResultCodeEnum.SUCCESS );
        req.getResultResponse().addControl( pagedSearchControl );

        return ( SearchResultDone ) req.getResultResponse();
    }


    /**
     * Remove a cookie instance from the session, if it exists.
     */
    private PagedSearchContext removeContext( LdapSession session, PagedSearchContext cookieInstance )
    {
        if ( cookieInstance == null )
        {
            return null;
        }

        int cookieValue = cookieInstance.getCookieValue();

        return session.removePagedSearchContext( cookieValue );
    }


    /**
     * Handle a Paged Search request.
     */
    private SearchResultDone doPagedSearch( LdapSession session, SearchRequest req, PagedResultsDecorator control )
        throws Exception
    {
        PagedResultsDecorator pagedSearchControl = control;
        PagedResultsDecorator pagedResultsControl = null;

        // Get the size limits
        // Don't bother setting size limits for administrators that don't ask for it
        long serverLimit = getServerSizeLimit( session, req );

        long requestLimit = req.getSizeLimit() == 0L ? Long.MAX_VALUE : req.getSizeLimit();
        long sizeLimit = min( serverLimit, requestLimit );

        int pagedLimit = pagedSearchControl.getSize();
        Cursor<Entry> cursor = null;
        PagedSearchContext pagedContext = null;

        // We have the following cases :
        // 1) The SIZE is 0 and the cookie is the same than the previous one : this
        // is a abandon request for this paged search.
        // 2) The cookie is empty : this is a new request. If the requested
        // size is above the serverLimit and the request limit, this is a normal
        // search
        // 3) The cookie is not empty and the request is the same, we return
        // the next SIZE elements
        // 4) The cookie is not empty, but the request is not the same : this is
        // a new request (we have to discard the cookie and do a new search from
        // the beginning)
        // 5) The SIZE is above the size-limit : the request is treated as if it
        // was a simple search

        // Case 1
        if ( pagedLimit == 0L )
        {
            // An abandoned paged search
            return abandonPagedSearch( session, req );
        }

        // Now, depending on the cookie, we will deal with case 2, 3, 4 and 5
        byte[] cookie = pagedSearchControl.getCookie();
        LdapResult ldapResult = req.getResultResponse().getLdapResult();

        if ( Strings.isEmpty( cookie ) )
        {
            // No cursor : do a search.
            cursor = session.getCoreSession().search( req );

            // Position the cursor at the beginning
            cursor.beforeFirst();

            // This is a new search. We have a special case when the paged size
            // is above the server size limit : in this case, we default to a
            // standard search
            if ( pagedLimit > sizeLimit )
            {
                // Normal search : create the cursor, and set pagedControl to false
                try
                {
                    // And write the entries
                    writeResults( session, req, ldapResult, cursor, sizeLimit );
                }
                finally
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

                // If we had a cookie in the session, remove it
                removeContext( session, pagedContext );

                return ( SearchResultDone ) req.getResultResponse();
            }
            else
            {
                // Case 2 : create the context
                pagedContext = new PagedSearchContext( req );

                session.addPagedSearchContext( pagedContext );
                cookie = pagedContext.getCookie();
                pagedResultsControl = new PagedResultsDecorator( ldapServer.getDirectoryService()
                    .getLdapCodecService() );
                pagedResultsControl.setCookie( cookie );
                pagedResultsControl.setSize( 0 );
                pagedResultsControl.setCritical( true );

                // And stores the cursor into the session
                pagedContext.setCursor( cursor );
            }
        }
        else
        {
            // We have a cookie
            // Either case 3, 4 or 5
            int cookieValue = pagedSearchControl.getCookieValue();
            pagedContext = session.getPagedSearchContext( cookieValue );

            if ( pagedContext == null )
            {
                // We didn't found the cookie into the session : it must be invalid
                // send an error.
                ldapResult.setDiagnosticMessage( "Invalid cookie for this PagedSearch request." );
                ldapResult.setResultCode( ResultCodeEnum.UNWILLING_TO_PERFORM );

                return ( SearchResultDone ) req.getResultResponse();
            }

            if ( pagedContext.hasSameRequest( req, session ) )
            {
                // Case 3 : continue the search
                cursor = pagedContext.getCursor();

                // get the cookie
                cookie = pagedContext.getCookie();
                pagedResultsControl = new PagedResultsDecorator( ldapServer.getDirectoryService()
                    .getLdapCodecService() );
                pagedResultsControl.setCookie( cookie );
                pagedResultsControl.setSize( 0 );
                pagedResultsControl.setCritical( true );

            }
            else
            {
                // case 2 : create a new cursor
                // We have to close the cursor
                cursor = pagedContext.getCursor();

                if ( cursor != null )
                {
                    cursor.close();
                }

                // Now create a new context and stores it into the session
                pagedContext = new PagedSearchContext( req );

                session.addPagedSearchContext( pagedContext );

                cookie = pagedContext.getCookie();
                pagedResultsControl = new PagedResultsDecorator( ldapServer.getDirectoryService()
                    .getLdapCodecService() );
                pagedResultsControl.setCookie( cookie );
                pagedResultsControl.setSize( 0 );
                pagedResultsControl.setCritical( true );
            }
        }

        // Now, do the real search
        /*
         * Iterate through all search results building and sending back responses
         * for each search result returned.
         */
        try
        {
            readPagedResults( session, req, ldapResult, cursor, sizeLimit, pagedLimit, pagedContext,
                pagedResultsControl );
        }
        catch ( Exception e )
        {
            if ( cursor != null )
            {
                try
                {
                    cursor.close();
                }
                catch ( Exception ne )
                {
                    LOG.error( I18n.err( I18n.ERR_168 ), ne );
                }
            }
        }

        return ( SearchResultDone ) req.getResultResponse();
    }


    /**
     * Conducts a simple search across the result set returning each entry
     * back except for the search response done.  This is calculated but not
     * returned so the persistent search mechanism can leverage this method
     * along with standard search.<br>
     * <br>
     * @param session the LDAP session object for this request
     * @param req the search request
     * @return the result done
     * @throws Exception if there are failures while processing the request
     */
    private SearchResultDone doSimpleSearch( LdapSession session, SearchRequest req ) throws Exception
    {
        LdapResult ldapResult = req.getResultResponse().getLdapResult();

        // Check if we are using the Paged Search Control
        Object control = req.getControls().get( PagedResults.OID );

        if ( control != null )
        {
            // Let's deal with the pagedControl
            return doPagedSearch( session, req, ( PagedResultsDecorator ) control );
        }

        // A normal search
        // Check that we have a cursor or not.
        // No cursor : do a search.
        Cursor<Entry> cursor = session.getCoreSession().search( req );

        // register the request in the session
        session.registerSearchRequest( req, cursor );

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

            if ( IS_DEBUG )
            {
                LOG.debug( "using <{},{}> for size limit", requestLimit, serverLimit );
            }

            long sizeLimit = min( requestLimit, serverLimit );

            writeResults( session, req, ldapResult, cursor, sizeLimit );
        }
        catch ( Exception e )
        {
            throw e;
        }
        finally
        {
            if ( ( cursor != null ) && !cursor.isClosed() )
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

        return ( SearchResultDone ) req.getResultResponse();
    }


    /**
     * Generates a response for an entry retrieved from the server core based
     * on the nature of the request with respect to referral handling.  This
     * method will either generate a SearchResponseEntry or a
     * SearchResponseReference depending on if the entry is a referral or if
     * the ManageDSAITControl has been enabled.
     *
     * @param req the search request
     * @param entry the entry to be handled
     * @return the response for the entry
     * @throws Exception if there are problems in generating the response
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

                    switch ( req.getScope() )
                    {
                        case SUBTREE:
                            ldapUrl.setScope( SearchScope.SUBTREE.getScope() );
                            break;

                        case ONELEVEL: // one level here is object level on remote server
                            ldapUrl.setScope( SearchScope.OBJECT.getScope() );
                            break;

                        default:
                            ldapUrl.setScope( SearchScope.OBJECT.getScope() );
                    }
                }
                catch ( LdapURLEncodingException e )
                {
                    LOG.error( I18n.err( I18n.ERR_165, url, entry ) );
                    ldapUrl = new LdapUrl();
                }

                respRef.getReferral().addLdapUrl( ldapUrl.toString() );
            }

            return respRef;
        }
        else
        {
            // The entry is not a referral, or the ManageDsaIt decorator is set
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
     * Alters the filter expression based on the presence of the
     * ManageDsaIT decorator.  If the decorator is not present, the search
     * filter will be altered to become a disjunction with two terms.
     * The first term is the original filter.  The second term is a
     * (objectClass=referral) assertion.  When OR'd together these will
     * make sure we get all referrals so we can process continuations
     * properly without having the filter remove them from the result
     * set.
     *
     * NOTE: original filter is first since most entries are not referrals
     * so it has a higher probability on average of accepting and shorting
     * evaluation before having to waste cycles trying to evaluate if the
     * entry is a referral.
     *
     * @param session the session to use to construct the filter (schema access)
     * @param req the request to get the original filter from
     * @throws Exception if there are schema access problems
     */
    private void modifyFilter( LdapSession session, SearchRequest req ) throws Exception
    {
        if ( req.hasControl( ManageDsaIT.OID ) )
        {
            return;
        }

        /*
         * Most of the time the search filter is just (objectClass=*) and if
         * this is the case then there's no reason at all to OR this with an
         * (objectClass=referral).  If we detect this case then we leave it
         * as is to represent the OR condition:
         *
         *  (| (objectClass=referral)(objectClass=*)) == (objectClass=*)
         */
        if ( req.getFilter() instanceof PresenceNode )
        {
            PresenceNode presenceNode = ( PresenceNode ) req.getFilter();

            if ( presenceNode.isSchemaAware() )
            {
                AttributeType attributeType = presenceNode.getAttributeType();

                if ( attributeType.equals( OBJECT_CLASS_AT ) )
                {
                    return;
                }
            }
            else
            {
                String attribute = presenceNode.getAttribute();

                if ( attribute.equalsIgnoreCase( SchemaConstants.OBJECT_CLASS_AT )
                    || attribute.equalsIgnoreCase( SchemaConstants.OBJECT_CLASS_AT_OID ) )
                {
                    return;
                }
            }
        }

        /*
         * Do not add the OR'd (objectClass=referral) expression if the user
         * searches for the subSchemaSubEntry as the SchemaIntercepter can't
         * handle an OR'd filter.
         */
        if ( isSubSchemaSubEntrySearch( session, req ) )
        {
            return;
        }

        // using varags to add two expressions to an OR node
        req.setFilter( new OrNode( req.getFilter(), newIsReferralEqualityNode( session ) ) );
    }


    /**
     * Handles the RootDSE and lookups searches
     */
    private boolean handleLookupAndRootDse( LdapSession session, SearchRequest req ) throws Exception
    {
        boolean isBaseScope = req.getScope() == SearchScope.OBJECT;
        boolean isObjectClassFilter = false;

        if ( req.getFilter() instanceof PresenceNode )
        {
            ExprNode filter = req.getFilter();

            if ( filter.isSchemaAware() )
            {
                AttributeType attributeType = ( ( PresenceNode ) req.getFilter() ).getAttributeType();
                isObjectClassFilter = attributeType.equals( OBJECT_CLASS_AT );
            }
            else
            {
                String attribute = ( ( PresenceNode ) req.getFilter() ).getAttribute();
                isObjectClassFilter = attribute.equalsIgnoreCase( SchemaConstants.OBJECT_CLASS_AT )
                    || attribute.equals( SchemaConstants.OBJECT_CLASS_AT_OID );
            }
        }

        /*
                if ( isBaseScope && isObjectClassFilter )
                {
                    // This is a lookup
                    handleLookup( session, req );

                    return true;
                }
                else
                {
                    // a standard search
                    return false;
                }
        */
        boolean isBaseIsRoot = req.getBase().isEmpty();

        if ( isBaseScope && isObjectClassFilter )
        {
            if ( isBaseIsRoot )
            {
                // This is a rootDse lookup
                handleLookup( session, req );

                return true;
            }
            else
            {
                // This is a lookup
                //handleLookup( session, req );

                return false;
            }
        }
        else
        {
            // a standard search
            return false;
        }
    }


    /**
     * Main message handing method for search requests.  This will be called
     * even if the ManageDsaIT decorator is present because the super class does
     * not know that the search operation has more to do after finding the
     * base.  The call to this means that finding the base can ignore
     * referrals.
     *
     * @param session the associated session
     * @param req the received SearchRequest
     */
    private void handleIgnoringReferrals( LdapSession session, SearchRequest req )
    {
        if ( IS_DEBUG )
        {
            LOG.debug( "Message received:  {}", req.toString() );
        }

        // A flag set if we have a persistent search
        boolean isPersistentSearch = false;

        // A flag set when we've got an exception while processing a
        // persistent search
        boolean persistentSearchException = false;

        // add the search request to the registry of outstanding requests for this session
        session.registerOutstandingRequest( req );

        try
        {
            // ===============================================================
            // Handle search in rootDSE and simple lookups differently.
            // ===============================================================
            if ( handleLookupAndRootDse( session, req ) )
            {
                return;
            }

            // modify the filter to affect continuation support
            modifyFilter( session, req );

            // ===============================================================
            // Handle psearch differently
            // ===============================================================

            PersistentSearch psearch = ( PersistentSearch ) req.getControls().get( PersistentSearch.OID );

            if ( psearch != null )
            {
                // Set the flag to avoid the request being removed
                // from the session
                isPersistentSearch = true;

                handlePersistentSearch( session, req, psearch );

                return;
            }

            // ===============================================================
            // Handle regular search requests from here down
            // ===============================================================

            boolean isLogSearchTime = SEARCH_TIME_LOG.isDebugEnabled();
            
            long t0 = 0;
            String filter = null;
            
            if ( isLogSearchTime )
            {
                t0 = System.nanoTime();
                filter = req.getFilter().toString();
            }
            
            SearchResultDone done = doSimpleSearch( session, req );
            session.getIoSession().write( done );
            
            if ( isLogSearchTime )
            {
                long t1 = System.nanoTime();
                SEARCH_TIME_LOG.debug( "Search with filter {} took {}ms. Filter with assigned counts is {}", filter, ((t1-t0)/1000000), req.getFilter() );
            }
        }
        catch ( Exception e )
        {
            /*
             * From RFC 2251 Section 4.11:
             *
             * In the event that a server receives an Abandon Request on a Search
             * operation in the midst of transmitting responses to the Search, that
             * server MUST cease transmitting entry responses to the abandoned
             * request immediately, and MUST NOT send the SearchResultDone. Of
             * course, the server MUST ensure that only properly encoded LDAPMessage
             * PDUs are transmitted.
             *
             * SO DON'T SEND BACK ANYTHING!!!!!
             */
            if ( e instanceof OperationAbandonedException )
            {
                return;
            }

            // If it was a persistent search and if we had an exception,
            // we set the flag to remove the request from the session
            if ( isPersistentSearch )
            {
                persistentSearchException = true;
            }

            handleException( session, req, e );
        }
        finally
        {

            // remove the request from the session, except if
            // we didn't got an exception for a Persistent search
            if ( !isPersistentSearch || persistentSearchException )
            {
                session.unregisterOutstandingRequest( req );
            }
        }
    }


    /**
     * Handles processing with referrals without ManageDsaIT decorator.
     */
    private void handleWithReferrals( LdapSession session, SearchRequest req ) throws LdapException
    {
        LdapResult result = req.getResultResponse().getLdapResult();
        Entry entry = null;
        boolean isReferral = false;
        boolean isparentReferral = false;
        DirectoryService directoryService = session.getCoreSession().getDirectoryService();
        ReferralManager referralManager = directoryService.getReferralManager();
        Dn reqTargetDn = req.getBase();

        reqTargetDn.apply( directoryService.getSchemaManager() );

        // Check if the entry itself is a referral
        referralManager.lockRead();

        try
        {
            isReferral = referralManager.isReferral( reqTargetDn );

            if ( !isReferral )
            {
                // Check if the entry has a parent which is a referral
                isparentReferral = referralManager.hasParentReferral( reqTargetDn );
            }
        }
        finally
        {
            // Unlock the ReferralManager
            referralManager.unlock();
        }

        if ( !isReferral && !isparentReferral )
        {
            // This is not a referral and it does not have a parent which
            // is a referral : standard case, just deal with the request
            if ( IS_DEBUG )
            {
                LOG.debug( "Entry {} is NOT a referral.", reqTargetDn );
            }

            handleIgnoringReferrals( session, req );

            return;
        }
        else
        {
            // -------------------------------------------------------------------
            // Lookup Entry
            // -------------------------------------------------------------------

            // try to lookup the entry but ignore exceptions when it does not
            // exist since entry may not exist but may have an ancestor that is a
            // referral - would rather attempt a lookup that fails then do check
            // for existence than have to do another lookup to get entry info
            try
            {
                entry = session.getCoreSession().lookup( reqTargetDn );

                if ( IS_DEBUG )
                {
                    LOG.debug( "Entry for {} was found: ", reqTargetDn, entry );
                }
            }
            catch ( LdapException e )
            {
                /* ignore */
                LOG.debug( "Entry for {} not found.", reqTargetDn );
            }
            catch ( Exception e )
            {
                /* serious and needs handling */
                handleException( session, req, e );

                return;
            }

            // -------------------------------------------------------------------
            // Handle Existing Entry
            // -------------------------------------------------------------------

            if ( entry != null )
            {
                try
                {
                    if ( IS_DEBUG )
                    {
                        LOG.debug( "Entry is a referral: {}", entry );
                    }

                    handleReferralEntryForSearch( session, req, entry );

                    return;
                }
                catch ( Exception e )
                {
                    handleException( session, req, e );
                }
            }

            // -------------------------------------------------------------------
            // Handle Non-existing Entry
            // -------------------------------------------------------------------

            // if the entry is null we still have to check for a referral ancestor
            // also the referrals need to be adjusted based on the ancestor's ref
            // values to yield the correct path to the entry in the target DSAs

            else
            {
                // The entry is null : it has a parent referral.
                Entry referralAncestor = null;

                try
                {
                    referralAncestor = getFarthestReferralAncestor( session, reqTargetDn );
                }
                catch ( Exception e )
                {
                    handleException( session, req, e );

                    return;
                }

                if ( referralAncestor == null )
                {
                    result.setDiagnosticMessage( "Entry not found." );
                    result.setResultCode( ResultCodeEnum.NO_SUCH_OBJECT );
                    session.getIoSession().write( req.getResultResponse() );

                    return;
                }

                // if we get here then we have a valid referral ancestor
                try
                {
                    Referral referral = getReferralOnAncestorForSearch( session, req, referralAncestor );

                    result.setResultCode( ResultCodeEnum.REFERRAL );
                    result.setReferral( referral );
                    session.getIoSession().write( req.getResultResponse() );
                }
                catch ( Exception e )
                {
                    handleException( session, req, e );
                }
            }
        }
    }


    /**
     * Handles processing a referral response on a target entry which is a
     * referral.  It will for any request that returns an LdapResult in it's
     * response.
     *
     * @param session the session to use for processing
     * @param req the request
     * @param entry the entry associated with the request
     */
    private void handleReferralEntryForSearch( LdapSession session, SearchRequest req, Entry entry )
        throws Exception
    {
        LdapResult result = req.getResultResponse().getLdapResult();
        ReferralImpl referral = new ReferralImpl();
        result.setReferral( referral );
        result.setResultCode( ResultCodeEnum.REFERRAL );
        result.setDiagnosticMessage( "Encountered referral attempting to handle request." );
        result.setMatchedDn( req.getBase() );

        Attribute refAttr = ( ( ClonedServerEntry ) entry ).getOriginalEntry().get( SchemaConstants.REF_AT );

        for ( Value<?> refval : refAttr )
        {
            String refstr = refval.getString();

            // need to add non-ldap URLs as-is
            if ( !refstr.startsWith( "ldap" ) )
            {
                referral.addLdapUrl( refstr );
                continue;
            }

            // parse the ref value and normalize the Dn
            LdapUrl ldapUrl = null;

            try
            {
                ldapUrl = new LdapUrl( refstr );
            }
            catch ( LdapURLEncodingException e )
            {
                LOG.error( I18n.err( I18n.ERR_165, refstr, entry ) );
                continue;
            }

            ldapUrl.setForceScopeRendering( true );
            ldapUrl.setAttributes( req.getAttributes() );
            ldapUrl.setScope( req.getScope().getScope() );
            referral.addLdapUrl( ldapUrl.toString() );
        }

        session.getIoSession().write( req.getResultResponse() );
    }


    /**
     * <p>
     * Determines if a search request is a subSchemaSubEntry search.
     * </p>
     * <p>
     * It is a schema search if:
     * - the base Dn is the Dn of the subSchemaSubEntry of the root DSE
     * - and the scope is BASE OBJECT
     * - and the filter is (objectClass=subschema)
     * (RFC 4512, 4.4,)
     * </p>
     * <p>
     * However in this method we only check the first condition to avoid
     * performance issues.
     * </p>
     *
     * @param session the LDAP session
     * @param req the request issued
     *
     * @return true if the search is on the subSchemaSubEntry, false otherwise
     *
     * @throws Exception the exception
     */
    private boolean isSubSchemaSubEntrySearch( LdapSession session, SearchRequest req ) throws Exception
    {
        Dn base = req.getBase();
        String baseNormForm = ( base.isSchemaAware() ? base.getNormName() : base.getNormName() );

        DirectoryService ds = session.getCoreSession().getDirectoryService();
        PartitionNexus nexus = ds.getPartitionNexus();

        if ( SUBSCHEMA_SUBENTRY_AT == null )
        {
            SUBSCHEMA_SUBENTRY_AT = session.getCoreSession().getDirectoryService().getSchemaManager().getAttributeType(
                SchemaConstants.SUBSCHEMA_SUBENTRY_AT );
        }

        Value<?> subschemaSubentry = nexus.getRootDseValue( SUBSCHEMA_SUBENTRY_AT );
        Dn subschemaSubentryDn = ds.getDnFactory().create( subschemaSubentry.getString() );
        String subschemaSubentryDnNorm = subschemaSubentryDn.getNormName();

        return subschemaSubentryDnNorm.equals( baseNormForm );
    }


    /**
     * Handles processing with referrals without ManageDsaIT decorator and with
     * an ancestor that is a referral.  The original entry was not found and
     * the walk of the ancestry returned a referral.
     *
     * @param referralAncestor the farthest referral ancestor of the missing
     * entry
     */
    public Referral getReferralOnAncestorForSearch( LdapSession session, SearchRequest req,
        Entry referralAncestor ) throws Exception
    {
        if ( IS_DEBUG )
        {
            LOG.debug( "Inside getReferralOnAncestor()" );
        }

        Attribute refAttr = ( ( ClonedServerEntry ) referralAncestor ).getOriginalEntry().get( SchemaConstants.REF_AT );
        Referral referral = new ReferralImpl();

        for ( Value<?> value : refAttr )
        {
            String ref = value.getString();

            if ( IS_DEBUG )
            {
                LOG.debug( "Calculating LdapURL for referrence value {}", ref );
            }

            // need to add non-ldap URLs as-is
            if ( !ref.startsWith( "ldap" ) )
            {
                referral.addLdapUrl( ref );
                continue;
            }

            // Parse the ref value
            LdapUrl ldapUrl = null;

            try
            {
                ldapUrl = new LdapUrl( ref );
            }
            catch ( LdapURLEncodingException e )
            {
                LOG.error( I18n.err( I18n.ERR_165, ref, referralAncestor ) );
                ldapUrl = new LdapUrl();
            }

            // Normalize the Dn to check for same dn
            Dn urlDn = new Dn( session.getCoreSession().getDirectoryService()
                .getSchemaManager(), ldapUrl.getDn().getName() );

            if ( urlDn.getNormName().equals( req.getBase().getNormName() ) )
            {
                ldapUrl.setForceScopeRendering( true );
                ldapUrl.setAttributes( req.getAttributes() );
                ldapUrl.setScope( req.getScope().getScope() );
                referral.addLdapUrl( ldapUrl.toString() );
                continue;
            }

            /*
             * If we get here then the Dn of the referral was not the same as the
             * Dn of the ref LDAP URL.  We must calculate the remaining (difference)
             * name past the farthest referral Dn which the target name extends.
             */
            Dn suffix = req.getBase().getDescendantOf( referralAncestor.getDn() );
            Dn refDn = urlDn.add( suffix );

            ldapUrl.setDn( refDn );
            ldapUrl.setForceScopeRendering( true );
            ldapUrl.setAttributes( req.getAttributes() );
            ldapUrl.setScope( req.getScope().getScope() );
            referral.addLdapUrl( ldapUrl.toString() );
        }

        return referral;
    }


    /**
     * Handles processing with referrals without ManageDsaIT decorator and with
     * an ancestor that is a referral.  The original entry was not found and
     * the walk of the ancestry returned a referral.
     *
     * @param referralAncestor the farthest referral ancestor of the missing
     * entry
     */
    public Referral getReferralOnAncestor( LdapSession session, Dn reqTargetDn, SearchRequest req,
        Entry referralAncestor ) throws Exception
    {
        if ( IS_DEBUG )
        {
            LOG.debug( "Inside getReferralOnAncestor()" );
        }

        Attribute refAttr = ( ( ClonedServerEntry ) referralAncestor ).getOriginalEntry().get( SchemaConstants.REF_AT );
        Referral referral = new ReferralImpl();

        for ( Value<?> value : refAttr )
        {
            String ref = value.getString();

            if ( IS_DEBUG )
            {
                LOG.debug( "Calculating LdapURL for referrence value {}", ref );
            }

            // need to add non-ldap URLs as-is
            if ( !ref.startsWith( "ldap" ) )
            {
                referral.addLdapUrl( ref );
                continue;
            }

            // parse the ref value and normalize the Dn
            LdapUrl ldapUrl = null;

            try
            {
                ldapUrl = new LdapUrl( ref );
            }
            catch ( LdapURLEncodingException e )
            {
                LOG.error( I18n.err( I18n.ERR_165, ref, referralAncestor ) );
                ldapUrl = new LdapUrl();
            }

            Dn urlDn = new Dn( session.getCoreSession().getDirectoryService()
                .getSchemaManager(), ldapUrl.getDn().getName() );

            if ( urlDn.getNormName().equals( referralAncestor.getDn().getNormName() ) )
            {
                // according to the protocol there is no need for the dn since it is the same as this request
                StringBuilder buf = new StringBuilder();
                buf.append( ldapUrl.getScheme() );
                buf.append( ldapUrl.getHost() );

                if ( ldapUrl.getPort() > 0 )
                {
                    buf.append( ":" );
                    buf.append( ldapUrl.getPort() );
                }

                referral.addLdapUrl( buf.toString() );
                continue;
            }

            /*
             * If we get here then the Dn of the referral was not the same as the
             * Dn of the ref LDAP URL.  We must calculate the remaining (difference)
             * name past the farthest referral Dn which the target name extends.
             */
            Dn suffix = req.getBase().getDescendantOf( referralAncestor.getDn() );
            urlDn = urlDn.add( suffix );

            StringBuilder buf = new StringBuilder();
            buf.append( ldapUrl.getScheme() );
            buf.append( ldapUrl.getHost() );

            if ( ldapUrl.getPort() > 0 )
            {
                buf.append( ":" );
                buf.append( ldapUrl.getPort() );
            }

            buf.append( "/" );
            buf.append( LdapUrl.urlEncode( urlDn.getName(), false ) );
            referral.addLdapUrl( buf.toString() );
        }

        return referral;
    }


    /**
     * Handles processing with referrals without ManageDsaIT decorator.
     */
    public void handleException( LdapSession session, ResultResponseRequest req, Exception e )
    {
        LdapResult result = req.getResultResponse().getLdapResult();
        Exception cause = null;

        /*
         * Set the result code or guess the best option.
         */
        ResultCodeEnum code;

        if ( e instanceof CursorClosedException )
        {
            cause = ( Exception ) ( ( CursorClosedException ) e ).getCause();

            if ( cause == null )
            {
                cause = e;
            }
        }
        else
        {
            cause = e;
        }

        if ( cause instanceof LdapOperationException )
        {
            code = ( ( LdapOperationException ) cause ).getResultCode();
        }
        else
        {
            code = ResultCodeEnum.getBestEstimate( cause, req.getType() );
        }

        result.setResultCode( code );

        /*
         * Setup the error message to put into the request and put entire
         * exception into the message if we are in debug mode.  Note we
         * embed the result code name into the message.
         */
        String msg = code.toString() + ": failed for " + req + ": " + cause.getLocalizedMessage();

        if ( IS_DEBUG )
        {
            LOG.debug( msg, cause );
            msg += ":\n" + ExceptionUtils.getStackTrace( cause );
        }

        result.setDiagnosticMessage( msg );

        if ( cause instanceof LdapOperationException )
        {
            LdapOperationException ne = ( LdapOperationException ) cause;

            // Add the matchedDN if necessary
            boolean setMatchedDn = code == ResultCodeEnum.NO_SUCH_OBJECT || code == ResultCodeEnum.ALIAS_PROBLEM
                || code == ResultCodeEnum.INVALID_DN_SYNTAX || code == ResultCodeEnum.ALIAS_DEREFERENCING_PROBLEM;

            if ( ( ne.getResolvedDn() != null ) && setMatchedDn )
            {
                result.setMatchedDn( ne.getResolvedDn() );
            }
        }

        session.getIoSession().write( req.getResultResponse() );
    }


    /**
     * Searches up the ancestry of a Dn searching for the farthest referral
     * ancestor.  This is required to properly handle referrals.  Note that
     * this function is quite costly since it attempts to lookup all the
     * ancestors up the hierarchy just to see if they represent referrals.
     * Techniques can be employed later to improve this performance hit by
     * having an intelligent referral cache.
     *
     * @return the farthest referral ancestor or null
     * @throws Exception if there are problems during this search
     */
    // This will suppress PMD.EmptyCatchBlock warnings in this method
    @SuppressWarnings("PMD.EmptyCatchBlock")
    public static final Entry getFarthestReferralAncestor( LdapSession session, Dn target ) throws Exception
    {
        Entry entry;
        Entry farthestReferralAncestor = null;
        Dn dn = target;

        dn = dn.getParent();

        while ( !dn.isEmpty() )
        {
            if ( IS_DEBUG )
            {
                LOG.debug( "Walking ancestors of {} to find referrals.", dn );
            }

            try
            {
                entry = session.getCoreSession().lookup( dn );

                boolean isReferral = ( ( ClonedServerEntry ) entry ).getOriginalEntry().contains(
                    SchemaConstants.OBJECT_CLASS_AT, SchemaConstants.REFERRAL_OC );

                if ( isReferral )
                {
                    farthestReferralAncestor = entry;
                }

                dn = dn.getParent();
            }
            catch ( LdapException e )
            {
                if ( IS_DEBUG )
                {
                    LOG.debug( "Entry for {} not found.", dn );
                }

                // update the Dn as we strip last component
                dn = dn.getParent();
            }
        }

        return farthestReferralAncestor;
    }


    /**
     * Install the replication handler when it's allowed by this server
     * @param replicationReqHandler The replication handler provider
     */
    public void setReplicationReqHandler( ReplicationRequestHandler replicationReqHandler )
    {
        this.replicationReqHandler = replicationReqHandler;
    }
}
