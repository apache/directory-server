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

package org.apache.directory.server.core;


import java.io.IOException;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import org.apache.directory.ldap.client.api.LdapConnection;
import org.apache.directory.ldap.client.api.message.AbandonRequest;
import org.apache.directory.ldap.client.api.message.AddRequest;
import org.apache.directory.ldap.client.api.message.BindRequest;
import org.apache.directory.ldap.client.api.message.CompareRequest;
import org.apache.directory.ldap.client.api.message.DeleteRequest;
import org.apache.directory.ldap.client.api.message.ExtendedRequest;
import org.apache.directory.ldap.client.api.message.ModifyDnRequest;
import org.apache.directory.ldap.client.api.message.ModifyRequest;
import org.apache.directory.ldap.client.api.message.SearchRequest;
import org.apache.directory.server.core.filtering.EntryFilteringCursor;
import org.apache.directory.server.core.interceptor.context.BindOperationContext;
import org.apache.directory.shared.asn1.primitives.OID;
import org.apache.directory.shared.ldap.cursor.Cursor;
import org.apache.directory.shared.ldap.cursor.EmptyCursor;
import org.apache.directory.shared.ldap.entry.DefaultModification;
import org.apache.directory.shared.ldap.entry.Entry;
import org.apache.directory.shared.ldap.entry.EntryAttribute;
import org.apache.directory.shared.ldap.entry.Modification;
import org.apache.directory.shared.ldap.entry.ModificationOperation;
import org.apache.directory.shared.ldap.entry.Value;
import org.apache.directory.shared.ldap.exception.LdapException;
import org.apache.directory.shared.ldap.exception.LdapOperationException;
import org.apache.directory.shared.ldap.filter.FilterParser;
import org.apache.directory.shared.ldap.filter.SearchScope;
import org.apache.directory.shared.ldap.message.AddRequestImpl;
import org.apache.directory.shared.ldap.message.AddResponseImpl;
import org.apache.directory.shared.ldap.message.AliasDerefMode;
import org.apache.directory.shared.ldap.message.BindResponseImpl;
import org.apache.directory.shared.ldap.message.CompareRequestImpl;
import org.apache.directory.shared.ldap.message.CompareResponseImpl;
import org.apache.directory.shared.ldap.message.DeleteRequestImpl;
import org.apache.directory.shared.ldap.message.DeleteResponseImpl;
import org.apache.directory.shared.ldap.message.LdapResultImpl;
import org.apache.directory.shared.ldap.message.ModifyDnRequestImpl;
import org.apache.directory.shared.ldap.message.ModifyDnResponseImpl;
import org.apache.directory.shared.ldap.message.ModifyRequestImpl;
import org.apache.directory.shared.ldap.message.ModifyResponseImpl;
import org.apache.directory.shared.ldap.message.ResultCodeEnum;
import org.apache.directory.shared.ldap.message.SearchRequestImpl;
import org.apache.directory.shared.ldap.message.SearchResultEntryImpl;
import org.apache.directory.shared.ldap.message.control.Control;
import org.apache.directory.shared.ldap.message.internal.InternalAddRequest;
import org.apache.directory.shared.ldap.message.internal.InternalAddResponse;
import org.apache.directory.shared.ldap.message.internal.InternalBindResponse;
import org.apache.directory.shared.ldap.message.internal.InternalCompareRequest;
import org.apache.directory.shared.ldap.message.internal.InternalCompareResponse;
import org.apache.directory.shared.ldap.message.internal.InternalDeleteRequest;
import org.apache.directory.shared.ldap.message.internal.InternalDeleteResponse;
import org.apache.directory.shared.ldap.message.internal.InternalExtendedResponse;
import org.apache.directory.shared.ldap.message.internal.InternalLdapResult;
import org.apache.directory.shared.ldap.message.internal.InternalMessage;
import org.apache.directory.shared.ldap.message.internal.InternalModifyDnRequest;
import org.apache.directory.shared.ldap.message.internal.InternalModifyDnResponse;
import org.apache.directory.shared.ldap.message.internal.InternalModifyRequest;
import org.apache.directory.shared.ldap.message.internal.InternalModifyResponse;
import org.apache.directory.shared.ldap.message.internal.InternalResponse;
import org.apache.directory.shared.ldap.message.internal.InternalResultResponseRequest;
import org.apache.directory.shared.ldap.message.internal.InternalSearchRequest;
import org.apache.directory.shared.ldap.message.internal.InternalSearchResultEntry;
import org.apache.directory.shared.ldap.name.DN;
import org.apache.directory.shared.ldap.name.RDN;
import org.apache.directory.shared.ldap.schema.SchemaManager;
import org.apache.directory.shared.ldap.util.StringTools;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 *  An implementation of LdapConnection based on the CoreSession.
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 */
public class LdapCoreSessionConnection implements LdapConnection
{

    /** the CoreSession object */
    private CoreSession session;

    /** the SchemaManager */
    private SchemaManager schemaManager;

    /** the session's DirectoryService */
    private DirectoryService directoryService;

    private AtomicInteger messageId = new AtomicInteger( 0 );

    private static final Logger LOG = LoggerFactory.getLogger( LdapCoreSessionConnection.class );


    public LdapCoreSessionConnection()
    {
    }


    public LdapCoreSessionConnection( DirectoryService directoryService )
    {
        setDirectoryService( directoryService );
    }


    public LdapCoreSessionConnection( CoreSession session )
    {
        this.session = session;
        setDirectoryService( session.getDirectoryService() );

        // treat the session was already bound, hence increment the message ID
        messageId.incrementAndGet();
    }


    /**
     * {@inheritDoc}
     */
    public boolean close() throws IOException
    {
        try
        {
            unBind();
        }
        catch ( Exception e )
        {
            throw new IOException( e.getMessage() );
        }

        return true;
    }


    /**
     * {@inheritDoc}
     */
    public boolean connect() throws LdapException, IOException
    {
        return true;
    }


    private InternalLdapResult getDefaultResult()
    {
        InternalLdapResult result = new LdapResultImpl();
        result.setResultCode( ResultCodeEnum.SUCCESS );
        return result;
    }


    private InternalLdapResult getDefaultCompareResult()
    {
        InternalLdapResult result = new LdapResultImpl();
        result.setResultCode( ResultCodeEnum.COMPARE_TRUE );
        return result;
    }


    /**
     * {@inheritDoc}
     */
    public InternalAddResponse add( AddRequest addRequest ) throws LdapException
    {
        int newId = messageId.incrementAndGet();

        InternalAddRequest iadd = new AddRequestImpl( newId );
        iadd.setEntry( addRequest.getEntry() );

        InternalAddResponse resp = new AddResponseImpl( newId );
        resp.getLdapResult().setResultCode( ResultCodeEnum.SUCCESS );

        try
        {
            session.add( iadd );
        }
        catch ( LdapException e )
        {
            LOG.warn( e.getMessage(), e );

            resp.getLdapResult().setResultCode( ResultCodeEnum.getResultCode( e ) );
            resp.getLdapResult().setErrorMessage( e.getMessage() );
        }

        addResponseControls( iadd, resp );
        return resp;
    }


    /**
     * {@inheritDoc}
     */
    public InternalAddResponse add( Entry entry ) throws LdapException
    {
        return add( new AddRequest( entry ) );
    }


    /**
     * {@inheritDoc}
     */
    public InternalCompareResponse compare( CompareRequest compareRequest ) throws LdapException
    {
        int newId = messageId.incrementAndGet();

        InternalCompareResponse resp = new CompareResponseImpl( newId );
        resp.getLdapResult().setResultCode( ResultCodeEnum.COMPARE_TRUE );

        InternalCompareRequest icompare = new CompareRequestImpl( newId );

        try
        {
            Object obj = compareRequest.getValue();
            if ( obj instanceof byte[] )
            {
                icompare.setAssertionValue( ( byte[] ) obj );
            }
            else
            {
                icompare.setAssertionValue( ( String ) obj );
            }

            icompare.setAttributeId( compareRequest.getAttrName() );
            icompare.setName( compareRequest.getEntryDn() );

            session.compare( icompare );
        }
        catch ( LdapException e )
        {
            LOG.warn( e.getMessage(), e );

            resp.getLdapResult().setResultCode( ResultCodeEnum.getResultCode( e ) );
            resp.getLdapResult().setErrorMessage( e.getMessage() );
        }

        addResponseControls( icompare, resp );
        return resp;
    }


    /**
     * {@inheritDoc}
     */
    public InternalCompareResponse compare( DN dn, String attributeName, byte[] value ) throws LdapException
    {
        CompareRequest compareRequest = new CompareRequest();
        compareRequest.setEntryDn( dn );
        compareRequest.setAttrName( attributeName );
        compareRequest.setValue( value );

        return compare( compareRequest );
    }


    /**
     * {@inheritDoc}
     */
    public InternalCompareResponse compare( DN dn, String attributeName, String value ) throws LdapException
    {
        CompareRequest compareRequest = new CompareRequest();
        compareRequest.setEntryDn( dn );
        compareRequest.setAttrName( attributeName );
        compareRequest.setValue( value );

        return compare( compareRequest );
    }


    /**
     * {@inheritDoc}
     */
    public InternalCompareResponse compare( String dn, String attributeName, byte[] value ) throws LdapException
    {
        return compare( new DN( dn ), attributeName, value );
    }


    /**
     * {@inheritDoc}
     */
    public InternalCompareResponse compare( String dn, String attributeName, String value ) throws LdapException
    {
        return compare( new DN( dn ), attributeName, value );
    }


    /**
     * {@inheritDoc}
     */
    public InternalCompareResponse compare( DN dn, String attributeName, Value<?> value ) throws LdapException
    {
        CompareRequest compareRequest = new CompareRequest();
        compareRequest.setEntryDn( dn );
        compareRequest.setAttrName( attributeName );
        compareRequest.setValue( value.get() );

        return compare( compareRequest );
    }


    /**
     * {@inheritDoc}
     */
    public InternalCompareResponse compare( String dn, String attributeName, Value<?> value ) throws LdapException
    {
        return compare( new DN( dn ), attributeName, value );
    }


    /**
     * {@inheritDoc}
     */
    public InternalDeleteResponse delete( DeleteRequest deleteRequest ) throws LdapException
    {
        int newId = messageId.incrementAndGet();

        InternalDeleteResponse resp = new DeleteResponseImpl( newId );
        resp.getLdapResult().setResultCode( ResultCodeEnum.SUCCESS );

        InternalDeleteRequest idelete = new DeleteRequestImpl( newId );

        try
        {
            idelete.setName( deleteRequest.getTargetDn() );
            session.delete( idelete );
        }
        catch ( LdapException e )
        {
            LOG.warn( e.getMessage(), e );

            resp.getLdapResult().setResultCode( ResultCodeEnum.getResultCode( e ) );
            resp.getLdapResult().setErrorMessage( e.getMessage() );
        }

        addResponseControls( idelete, resp );
        return resp;
    }


    /**
     * {@inheritDoc}
     */
    public InternalDeleteResponse delete( DN dn ) throws LdapException
    {
        DeleteRequest delReq = new DeleteRequest( dn );
        return delete( delReq );
    }


    /**
     * {@inheritDoc}
     */
    public InternalDeleteResponse delete( String dn ) throws LdapException
    {
        return delete( new DN( dn ) );
    }


    /**
     * {@inheritDoc}
     */
    public boolean doesFutureExistFor( Integer messageId )
    {
        return false;
    }


    /**
     * {@inheritDoc}
     */
    public SchemaManager getSchemaManager()
    {
        return schemaManager;
    }


    /**
     * {@inheritDoc}
     */
    public List<String> getSupportedControls() throws LdapException
    {
        return null;
    }


    /**
     * {@inheritDoc}
     */
    public boolean isAuthenticated()
    {
        return ( session != null );
    }


    /**
     * {@inheritDoc}
     */
    public boolean isConnected()
    {
        return true;
    }


    /**
     * {@inheritDoc}
     */
    public boolean isControlSupported( String controlOID ) throws LdapException
    {
        return false;
    }


    /**
     * {@inheritDoc}
     */
    public void loadSchema() throws LdapException
    {
        // do nothing, cause we already have SchemaManager in the session's DirectoryService
    }


    /**
     * {@inheritDoc}
     */
    public InternalResponse lookup( DN dn, String... attributes ) throws LdapException
    {
        return _lookup( dn, attributes );
    }


    /*
     * this method exists solely for the purpose of calling from
     * lookup(DN dn) avoiding the varargs,
     */
    private InternalResponse _lookup( DN dn, String... attributes )
    {
        int newId = messageId.incrementAndGet();

        InternalSearchResultEntry resp = null;

        try
        {
            Entry entry = null;

            if ( attributes == null )
            {
                entry = session.lookup( dn );
            }
            else
            {
                entry = session.lookup( dn, attributes );
            }

            resp = new SearchResultEntryImpl( newId );
            resp.setEntry( entry );
        }
        catch ( LdapException e )
        {
            LOG.warn( e.getMessage(), e );
        }

        return resp;
    }


    /**
     * {@inheritDoc}
     */
    public InternalResponse lookup( String dn, String... attributes ) throws LdapException
    {
        return _lookup( new DN( dn ), attributes );
    }


    /**
     * {@inheritDoc}
     */
    public InternalResponse lookup( DN dn ) throws LdapException
    {
        return _lookup( dn );
    }


    /**
     * {@inheritDoc}
     */
    public InternalResponse lookup( String dn ) throws LdapException
    {
        return _lookup( new DN( dn ) );
    }


    /**
     * {@inheritDoc}
     */
    public InternalModifyResponse modify( DN dn, Modification... modifications ) throws LdapException
    {
        int newId = messageId.incrementAndGet();

        InternalModifyResponse resp = new ModifyResponseImpl( newId );
        resp.getLdapResult().setResultCode( ResultCodeEnum.SUCCESS );

        InternalModifyRequest iModReq = new ModifyRequestImpl( newId );

        try
        {
            iModReq.setName( dn );

            for ( Modification modification : modifications )
            {
                iModReq.addModification( modification );
            }

            session.modify( iModReq );
        }
        catch ( LdapException e )
        {
            LOG.warn( e.getMessage(), e );

            resp.getLdapResult().setResultCode( ResultCodeEnum.getResultCode( e ) );
            resp.getLdapResult().setErrorMessage( e.getMessage() );
        }

        addResponseControls( iModReq, resp );
        return resp;
    }


    /**
     * {@inheritDoc}
     */
    public InternalModifyResponse modify( String dn, Modification... modifications ) throws LdapException
    {
        return modify( new DN( dn ), modifications );
    }


    /**
     * {@inheritDoc}
     */
    public InternalModifyResponse modify( Entry entry, ModificationOperation modOp ) throws LdapException
    {
        int newId = messageId.incrementAndGet();
        InternalModifyResponse resp = new ModifyResponseImpl( newId );
        resp.getLdapResult().setResultCode( ResultCodeEnum.SUCCESS );

        InternalModifyRequest iModReq = new ModifyRequestImpl( newId );

        try
        {
            iModReq.setName( entry.getDn() );

            Iterator<EntryAttribute> itr = entry.iterator();
            while ( itr.hasNext() )
            {
                iModReq.addModification( new DefaultModification( modOp, itr.next() ) );
            }

            session.modify( iModReq );
        }
        catch ( LdapException e )
        {
            LOG.warn( e.getMessage(), e );

            resp.getLdapResult().setResultCode( ResultCodeEnum.getResultCode( e ) );
            resp.getLdapResult().setErrorMessage( e.getMessage() );
        }

        addResponseControls( iModReq, resp );
        return resp;
    }


    /**
     * {@inheritDoc}
     */
    public InternalModifyResponse modify( ModifyRequest modRequest ) throws LdapException
    {
        int newId = messageId.incrementAndGet();

        InternalModifyResponse resp = new ModifyResponseImpl( newId );
        resp.getLdapResult().setResultCode( ResultCodeEnum.SUCCESS );

        InternalModifyRequest iModReq = new ModifyRequestImpl( newId );

        try
        {
            iModReq.setName( modRequest.getDn() );

            Iterator<Modification> itr = modRequest.getMods().iterator();
            while ( itr.hasNext() )
            {
                iModReq.addModification( itr.next() );
            }

            session.modify( iModReq );
        }
        catch ( LdapException e )
        {
            LOG.warn( e.getMessage(), e );

            resp.getLdapResult().setResultCode( ResultCodeEnum.getResultCode( e ) );
            resp.getLdapResult().setErrorMessage( e.getMessage() );
        }

        addResponseControls( iModReq, resp );
        return resp;
    }


    /**
     * {@inheritDoc}
     * WARNING: this method is not in compatible with CoreSession API in some cases
     *          cause this we call {@link CoreSession#move(InternalModifyDnRequest)} always from this method.
     *          Instead use other specific modifyDn operations like {@link #move(DN, DN)}, {@link #rename(DN, RDN)} etc..
     */
    public InternalModifyDnResponse modifyDn( ModifyDnRequest modDnRequest ) throws LdapException
    {
        int newId = messageId.incrementAndGet();

        InternalModifyDnResponse resp = new ModifyDnResponseImpl( newId );
        resp.getLdapResult().setResultCode( ResultCodeEnum.SUCCESS );

        InternalModifyDnRequest iModDnReq = new ModifyDnRequestImpl( newId );

        try
        {
            iModDnReq.setDeleteOldRdn( modDnRequest.isDeleteOldRdn() );
            iModDnReq.setName( modDnRequest.getEntryDn() );
            iModDnReq.setNewRdn( modDnRequest.getNewRdn() );
            iModDnReq.setNewSuperior( modDnRequest.getNewSuperior() );

            session.move( iModDnReq );
        }
        catch ( LdapException e )
        {
            LOG.warn( e.getMessage(), e );

            resp.getLdapResult().setResultCode( ResultCodeEnum.getResultCode( e ) );
            resp.getLdapResult().setErrorMessage( e.getMessage() );
        }

        addResponseControls( iModDnReq, resp );
        return resp;
    }


    /**
     * {@inheritDoc}
     */
    public InternalModifyDnResponse move( DN entryDn, DN newSuperiorDn ) throws LdapException
    {
        int newId = messageId.incrementAndGet();
        InternalModifyDnResponse resp = new ModifyDnResponseImpl( newId );
        resp.getLdapResult().setResultCode( ResultCodeEnum.SUCCESS );

        InternalModifyDnRequest iModDnReq = new ModifyDnRequestImpl( newId );

        try
        {
            iModDnReq.setName( entryDn );
            iModDnReq.setNewSuperior( newSuperiorDn );

            session.move( iModDnReq );
        }
        catch ( LdapException e )
        {
            LOG.warn( e.getMessage(), e );

            resp.getLdapResult().setResultCode( ResultCodeEnum.getResultCode( e ) );
            resp.getLdapResult().setErrorMessage( e.getMessage() );
        }

        addResponseControls( iModDnReq, resp );
        return resp;
    }


    /**
     * {@inheritDoc}
     */
    public InternalModifyDnResponse move( String entryDn, String newSuperiorDn ) throws LdapException
    {
        return move( new DN( entryDn ), new DN( newSuperiorDn ) );
    }


    /**
     * {@inheritDoc}
     */
    public InternalModifyDnResponse rename( DN entryDn, RDN newRdn, boolean deleteOldRdn ) throws LdapException
    {
        int newId = messageId.incrementAndGet();

        InternalModifyDnResponse resp = new ModifyDnResponseImpl( newId );
        resp.getLdapResult().setResultCode( ResultCodeEnum.SUCCESS );

        InternalModifyDnRequest iModDnReq = new ModifyDnRequestImpl( newId );

        try
        {
            iModDnReq.setName( entryDn );
            iModDnReq.setNewRdn( newRdn );
            iModDnReq.setDeleteOldRdn( deleteOldRdn );

            session.rename( iModDnReq );
        }
        catch ( LdapException e )
        {
            LOG.warn( e.getMessage(), e );

            resp.getLdapResult().setResultCode( ResultCodeEnum.getResultCode( e ) );
            resp.getLdapResult().setErrorMessage( e.getMessage() );
        }

        addResponseControls( iModDnReq, resp );
        return resp;
    }


    /**
     * {@inheritDoc}
     */
    public InternalModifyDnResponse rename( DN entryDn, RDN newRdn ) throws LdapException
    {
        return rename( entryDn, newRdn, false );
    }


    /**
     * {@inheritDoc}
     */
    public InternalModifyDnResponse rename( String entryDn, String newRdn, boolean deleteOldRdn ) throws LdapException
    {
        return rename( new DN( entryDn ), new RDN( newRdn ), deleteOldRdn );
    }


    /**
     * {@inheritDoc}
     */
    public InternalModifyDnResponse rename( String entryDn, String newRdn ) throws LdapException
    {
        return rename( new DN( entryDn ), new RDN( newRdn ) );
    }


    /**
     * Moves and renames the given entryDn.The old RDN will be deleted
     *
     * @see #moveAndRename(DN, DN, boolean)
     */
    public InternalModifyDnResponse moveAndRename( DN entryDn, DN newDn ) throws LdapException
    {
        return moveAndRename( entryDn, newDn, true );
    }


    /**
     * Moves and renames the given entryDn.The old RDN will be deleted
     *
     * @see #moveAndRename(DN, DN, boolean)
     */
    public InternalModifyDnResponse moveAndRename( String entryDn, String newDn ) throws LdapException
    {
        return moveAndRename( new DN( entryDn ), new DN( newDn ), true );
    }


    /**
     * Moves and renames the given entryDn.The old RDN will be deleted if requested
     *
     * @param entryDn The original entry DN
     * @param newDn The new Entry DN
     * @param deleteOldRdn Tells if the old RDN must be removed
     */
    public InternalModifyDnResponse moveAndRename( DN entryDn, DN newDn, boolean deleteOldRdn ) throws LdapException
    {
        // Check the parameters first
        if ( entryDn == null )
        {
            throw new IllegalArgumentException( "The entry DN must not be null" );
        }

        if ( entryDn.isRootDSE() )
        {
            throw new IllegalArgumentException( "The RootDSE cannot be moved" );
        }

        if ( newDn == null )
        {
            throw new IllegalArgumentException( "The new DN must not be null" );
        }

        if ( newDn.isRootDSE() )
        {
            throw new IllegalArgumentException( "The RootDSE cannot be the target" );
        }

        int newId = messageId.incrementAndGet();

        InternalModifyDnResponse resp = new ModifyDnResponseImpl( newId );
        resp.getLdapResult().setResultCode( ResultCodeEnum.SUCCESS );

        InternalModifyDnRequest iModDnReq = new ModifyDnRequestImpl( newId );

        try
        {
            iModDnReq.setName( entryDn );
            iModDnReq.setNewSuperior( newDn );
            iModDnReq.setDeleteOldRdn( deleteOldRdn );

            session.moveAndRename( iModDnReq );
        }
        catch ( LdapException e )
        {
            LOG.warn( e.getMessage(), e );

            resp.getLdapResult().setResultCode( ResultCodeEnum.getResultCode( e ) );
            resp.getLdapResult().setErrorMessage( e.getMessage() );
        }

        addResponseControls( iModDnReq, resp );
        return resp;
    }


    /**
     * Moves and renames the given entryDn.The old RDN will be deleted if requested
     *
     * @param entryDn The original entry DN
     * @param newDn The new Entry DN
     * @param deleteOldRdn Tells if the old RDN must be removed
     */
    public InternalModifyDnResponse moveAndRename( String entryDn, String newDn, boolean deleteOldRdn )
        throws LdapException
    {
        return moveAndRename( new DN( entryDn ), new DN( newDn ), deleteOldRdn );
    }


    /**
     * {@inheritDoc}
     */
    public Cursor<InternalResponse> search( SearchRequest searchRequest ) throws LdapException
    {
        try
        {
            int newId = messageId.incrementAndGet();

            InternalSearchRequest iSearchReq = new SearchRequestImpl( newId );
            iSearchReq.setBase( new DN( searchRequest.getBaseDn() ) );
            iSearchReq.setDerefAliases( searchRequest.getDerefAliases() );
            iSearchReq.setFilter( FilterParser.parse( schemaManager, searchRequest.getFilter() ) );
            iSearchReq.setScope( searchRequest.getScope() );
            iSearchReq.setSizeLimit( searchRequest.getSizeLimit() );
            iSearchReq.setTimeLimit( searchRequest.getTimeLimit() );
            iSearchReq.setTypesOnly( searchRequest.getTypesOnly() );

            if ( searchRequest.getAttributes() != null )
            {
                for ( String at : searchRequest.getAttributes() )
                {
                    iSearchReq.addAttribute( at );
                }
            }

            EntryFilteringCursor entryCursor = session.search( iSearchReq );
            entryCursor.beforeFirst();

            //TODO enforce the size and time limits, similar in the way SearchHandler does
            return new EntryToResponseCursor( newId, entryCursor );
        }
        catch ( Exception e )
        {
            LOG.warn( e.getMessage(), e );
        }

        return new EntryToResponseCursor<InternalResponse>( -1, new EmptyCursor<InternalResponse>() );
    }


    /**
     * {@inheritDoc}
     */
    public Cursor<InternalResponse> search( DN baseDn, String filter, SearchScope scope, String... attributes )
        throws LdapException
    {
        // generate some random operation number
        SearchRequest searchRequest = new SearchRequest();
        searchRequest.setMessageId( ( int ) System.currentTimeMillis() );
        searchRequest.setBaseDn( baseDn );
        searchRequest.setFilter( filter );
        searchRequest.setScope( scope );
        searchRequest.addAttributes( attributes );
        searchRequest.setDerefAliases( AliasDerefMode.DEREF_ALWAYS );

        return search( searchRequest );
    }


    /**
     * {@inheritDoc}
     */
    public Cursor<InternalResponse> search( String baseDn, String filter, SearchScope scope, String... attributes )
        throws LdapException
    {
        return search( new DN( baseDn ), filter, scope, attributes );
    }


    /**
     * {@inheritDoc}
     */
    public void unBind() throws Exception
    {
        messageId.set( 0 );
        if ( session != null )
        {
            session.unbind();
            session = null;
        }
    }


    /**
     * {@inheritDoc}
     */
    public InternalExtendedResponse extended( String oid ) throws LdapException
    {
        throw new UnsupportedOperationException(
            "extended operations are not supported on CoreSession based connection" );
    }


    /**
     * {@inheritDoc}
     */
    public InternalExtendedResponse extended( ExtendedRequest extendedRequest ) throws LdapException
    {
        return extended( ( String ) null );

    }


    /**
     * {@inheritDoc}
     */
    public InternalExtendedResponse extended( OID oid, byte[] value ) throws LdapException
    {
        return extended( ( String ) null );
    }


    /**
     * {@inheritDoc}
     */
    public InternalExtendedResponse extended( OID oid ) throws LdapException
    {
        return extended( ( String ) null );
    }


    /**
     * {@inheritDoc}
     */
    public InternalExtendedResponse extended( String oid, byte[] value ) throws LdapException
    {
        return extended( ( String ) null );
    }


    /**
     * {@inheritDoc}
     */
    public void setTimeOut( long timeOut )
    {
        throw new UnsupportedOperationException( "setting timeout is not supported on CoreSession" );
    }


    /**
     * {@inheritDoc}
     */
    public void abandon( AbandonRequest abandonRequest )
    {
        throw new UnsupportedOperationException( "abandon operation is not supported" );
    }


    /**
     * {@inheritDoc}
     */
    public void abandon( int messageId )
    {
        abandon( null );
    }


    /**
     * {@inheritDoc}
     */
    public InternalBindResponse bind() throws LdapException, IOException
    {
        BindRequest bindReq = new BindRequest();
        bindReq.setName( "" );
        bindReq.setCredentials( ( byte[] ) null );

        return bind( bindReq );
    }


    /**
     * {@inheritDoc}
     */
    public InternalBindResponse bind( BindRequest bindRequest ) throws LdapException, IOException
    {
        int newId = messageId.incrementAndGet();

        BindOperationContext bindContext = new BindOperationContext( null );
        bindContext.setCredentials( bindRequest.getCredentials() );
        bindContext.setDn( new DN( bindRequest.getName() ) );

        OperationManager operationManager = directoryService.getOperationManager();

        InternalBindResponse bindResp = new BindResponseImpl( newId );
        bindResp.getLdapResult().setResultCode( ResultCodeEnum.SUCCESS );

        try
        {
            if ( !bindRequest.isSimple() )
            {
                bindContext.setSaslMechanism( bindRequest.getSaslMechanism() );
            }

            operationManager.bind( bindContext );
            session = bindContext.getSession();

            bindResp.addAll( bindContext.getResponseControls() );
        }
        catch ( LdapOperationException e )
        {
            LOG.warn( e.getMessage(), e );
            InternalLdapResult res = bindResp.getLdapResult();
            res.setErrorMessage( e.getMessage() );
            res.setResultCode( e.getResultCode() );
        }

        return bindResp;
    }


    /**
     * {@inheritDoc}
     */
    public InternalBindResponse bind( DN name, String credentials ) throws LdapException, IOException
    {
        byte[] credBytes = ( credentials == null ? StringTools.EMPTY_BYTES : StringTools.getBytesUtf8( credentials ) );

        BindRequest bindReq = new BindRequest();
        bindReq.setName( name.getName() );
        bindReq.setCredentials( credBytes );

        return bind( bindReq );
    }


    /**
     * {@inheritDoc}
     */
    public InternalBindResponse bind( String name, String credentials ) throws LdapException, IOException
    {
        return bind( new DN( name ), credentials );
    }


    private void addResponseControls( InternalResultResponseRequest iReq, InternalMessage clientResp )
    {
        Collection<Control> ctrlSet = iReq.getResultResponse().getControls().values();

        for ( Control c : ctrlSet )
        {
            clientResp.add( c );
        }
    }


    public DirectoryService getDirectoryService()
    {
        return directoryService;
    }


    public void setDirectoryService( DirectoryService directoryService )
    {
        this.directoryService = directoryService;
        this.schemaManager = directoryService.getSchemaManager();
    }

}
