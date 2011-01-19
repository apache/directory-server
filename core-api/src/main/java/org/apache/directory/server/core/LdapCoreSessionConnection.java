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
import org.apache.directory.server.core.entry.ClonedServerEntry;
import org.apache.directory.server.core.filtering.EntryFilteringCursor;
import org.apache.directory.server.core.interceptor.context.BindOperationContext;
import org.apache.directory.shared.asn1.primitives.OID;
import org.apache.directory.shared.ldap.constants.SchemaConstants;
import org.apache.directory.shared.ldap.cursor.EmptyCursor;
import org.apache.directory.shared.ldap.cursor.SearchCursor;
import org.apache.directory.shared.ldap.entry.DefaultModification;
import org.apache.directory.shared.ldap.entry.Entry;
import org.apache.directory.shared.ldap.entry.EntryAttribute;
import org.apache.directory.shared.ldap.entry.Modification;
import org.apache.directory.shared.ldap.entry.ModificationOperation;
import org.apache.directory.shared.ldap.entry.Value;
import org.apache.directory.shared.ldap.exception.LdapException;
import org.apache.directory.shared.ldap.exception.LdapNoPermissionException;
import org.apache.directory.shared.ldap.exception.LdapOperationException;
import org.apache.directory.shared.ldap.filter.SearchScope;
import org.apache.directory.shared.ldap.message.AbandonRequest;
import org.apache.directory.shared.ldap.message.AddRequest;
import org.apache.directory.shared.ldap.message.AddRequestImpl;
import org.apache.directory.shared.ldap.message.AddResponse;
import org.apache.directory.shared.ldap.message.AddResponseImpl;
import org.apache.directory.shared.ldap.message.AliasDerefMode;
import org.apache.directory.shared.ldap.message.BindRequest;
import org.apache.directory.shared.ldap.message.BindRequestImpl;
import org.apache.directory.shared.ldap.message.BindResponse;
import org.apache.directory.shared.ldap.message.BindResponseImpl;
import org.apache.directory.shared.ldap.message.CompareRequest;
import org.apache.directory.shared.ldap.message.CompareRequestImpl;
import org.apache.directory.shared.ldap.message.CompareResponse;
import org.apache.directory.shared.ldap.message.CompareResponseImpl;
import org.apache.directory.shared.ldap.message.DeleteRequest;
import org.apache.directory.shared.ldap.message.DeleteRequestImpl;
import org.apache.directory.shared.ldap.message.DeleteResponse;
import org.apache.directory.shared.ldap.message.DeleteResponseImpl;
import org.apache.directory.shared.ldap.message.ExtendedRequest;
import org.apache.directory.shared.ldap.message.ExtendedResponse;
import org.apache.directory.shared.ldap.message.LdapResult;
import org.apache.directory.shared.ldap.message.Message;
import org.apache.directory.shared.ldap.message.ModifyDnRequest;
import org.apache.directory.shared.ldap.message.ModifyDnRequestImpl;
import org.apache.directory.shared.ldap.message.ModifyDnResponse;
import org.apache.directory.shared.ldap.message.ModifyDnResponseImpl;
import org.apache.directory.shared.ldap.message.ModifyRequest;
import org.apache.directory.shared.ldap.message.ModifyRequestImpl;
import org.apache.directory.shared.ldap.message.ModifyResponse;
import org.apache.directory.shared.ldap.message.ModifyResponseImpl;
import org.apache.directory.shared.ldap.message.ResultCodeEnum;
import org.apache.directory.shared.ldap.message.ResultResponseRequest;
import org.apache.directory.shared.ldap.message.SearchRequest;
import org.apache.directory.shared.ldap.message.SearchRequestImpl;
import org.apache.directory.shared.ldap.message.control.Control;
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
    /** The logger for this class */
    private static final Logger LOG = LoggerFactory.getLogger( LdapCoreSessionConnection.class );

    /** the CoreSession object */
    private CoreSession session;

    /** the SchemaManager */
    private SchemaManager schemaManager;

    /** the session's DirectoryService */
    private DirectoryService directoryService;

    /** The MessageId counter */
    private AtomicInteger messageId = new AtomicInteger( 0 );


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


    /**
     * {@inheritDoc}
     */
    public AddResponse add( AddRequest addRequest ) throws LdapException
    {
        if ( addRequest == null )
        {
            String msg = "Cannot process a null addRequest";
            LOG.debug( msg );
            throw new IllegalArgumentException( msg );
        }

        if ( addRequest.getEntry() == null )
        {
            String msg = "Cannot add a null entry";
            LOG.debug( msg );
            throw new IllegalArgumentException( msg );
        }

        int newId = messageId.incrementAndGet();

        addRequest.setMessageId( newId );

        AddResponse resp = new AddResponseImpl( newId );
        resp.getLdapResult().setResultCode( ResultCodeEnum.SUCCESS );

        try
        {
            session.add( addRequest );
        }
        catch ( LdapException e )
        {
            LOG.warn( e.getMessage(), e );

            resp.getLdapResult().setResultCode( ResultCodeEnum.getResultCode( e ) );
            resp.getLdapResult().setErrorMessage( e.getMessage() );
        }

        addResponseControls( addRequest, resp );
        return resp;
    }


    /**
     * {@inheritDoc}
     */
    public AddResponse add( Entry entry ) throws LdapException
    {
        if ( entry == null )
        {
            String msg = "Cannot add an empty entry";
            LOG.debug( msg );
            throw new IllegalArgumentException( msg );
        }

        AddRequest addRequest = new AddRequestImpl();
        addRequest.setEntry( entry );

        return add( addRequest );
    }


    /**
     * {@inheritDoc}
     */
    public CompareResponse compare( CompareRequest compareRequest ) throws LdapException
    {
        if ( compareRequest == null )
        {
            String msg = "Cannot process a null compareRequest";
            LOG.debug( msg );
            throw new IllegalArgumentException( msg );
        }

        int newId = messageId.incrementAndGet();

        CompareResponse resp = new CompareResponseImpl( newId );
        resp.getLdapResult().setResultCode( ResultCodeEnum.COMPARE_TRUE );

        try
        {
            session.compare( compareRequest );
        }
        catch ( Exception e )
        {
            resp.getLdapResult().setResultCode( ResultCodeEnum.getResultCode( e ) );
        }

        return resp;
    }


    /**
     * {@inheritDoc}
     */
    public CompareResponse compare( DN dn, String attributeName, byte[] value ) throws LdapException
    {
        CompareRequest compareRequest = new CompareRequestImpl();
        compareRequest.setName( dn );
        compareRequest.setAttributeId( attributeName );
        compareRequest.setAssertionValue( value );

        return compare( compareRequest );
    }


    /**
     * {@inheritDoc}
     */
    public CompareResponse compare( DN dn, String attributeName, String value ) throws LdapException
    {
        CompareRequest compareRequest = new CompareRequestImpl();
        compareRequest.setName( dn );
        compareRequest.setAttributeId( attributeName );
        compareRequest.setAssertionValue( value );

        return compare( compareRequest );
    }


    /**
     * {@inheritDoc}
     */
    public CompareResponse compare( String dn, String attributeName, byte[] value ) throws LdapException
    {
        return compare( new DN( dn ), attributeName, value );
    }


    /**
     * {@inheritDoc}
     */
    public CompareResponse compare( String dn, String attributeName, String value ) throws LdapException
    {
        return compare( new DN( dn ), attributeName, value );
    }


    /**
     * {@inheritDoc}
     */
    public CompareResponse compare( DN dn, String attributeName, Value<?> value ) throws LdapException
    {
        CompareRequest compareRequest = new CompareRequestImpl();
        compareRequest.setName( dn );
        compareRequest.setAttributeId( attributeName );

        if ( value.isBinary() )
        {
            compareRequest.setAssertionValue( value.getBytes() );
        }
        else
        {
            compareRequest.setAssertionValue( value.getString() );
        }

        return compare( compareRequest );
    }


    /**
     * {@inheritDoc}
     */
    public CompareResponse compare( String dn, String attributeName, Value<?> value ) throws LdapException
    {
        return compare( new DN( dn ), attributeName, value );
    }


    /**
     * {@inheritDoc}
     */
    public DeleteResponse delete( DeleteRequest deleteRequest ) throws LdapException
    {
        if ( deleteRequest == null )
        {
            String msg = "Cannot process a null deleteRequest";
            LOG.debug( msg );
            throw new IllegalArgumentException( msg );
        }

        int newId = messageId.incrementAndGet();

        DeleteResponse resp = new DeleteResponseImpl( newId );
        resp.getLdapResult().setResultCode( ResultCodeEnum.SUCCESS );

        try
        {
            session.delete( deleteRequest );
        }
        catch ( LdapException e )
        {
            LOG.warn( e.getMessage(), e );

            resp.getLdapResult().setResultCode( ResultCodeEnum.getResultCode( e ) );
            resp.getLdapResult().setErrorMessage( e.getMessage() );
        }

        addResponseControls( deleteRequest, resp );

        return resp;
    }


    /**
     * {@inheritDoc}
     */
    public DeleteResponse delete( DN dn ) throws LdapException
    {
        DeleteRequest deleteRequest = new DeleteRequestImpl();
        deleteRequest.setName( dn );

        return delete( deleteRequest );
    }


    /**
     * {@inheritDoc}
     */
    public DeleteResponse delete( String dn ) throws LdapException
    {
        return delete( new DN( dn ) );
    }


    /**
     * {@inheritDoc}
     */
    public boolean doesFutureExistFor( int messageId )
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
    public Entry lookup( DN dn, String... attributes ) throws LdapException
    {
        return _lookup( dn, null, attributes );
    }


    /**
     * {@inheritDoc}
     */
    public Entry lookup( DN dn, Control[] controls, String... attributes ) throws LdapException
    {
        return _lookup( dn, controls, attributes );
    }


    /**
     * {@inheritDoc}
     */
    public Entry lookup( String dn, String... attributes ) throws LdapException
    {
        return _lookup( new DN( dn ), null, attributes );
    }


    /**
     * {@inheritDoc}
     */
    public Entry lookup( String dn, Control[] controls, String... attributes ) throws LdapException
    {
        return _lookup( new DN( dn ), controls, attributes );
    }


    /*
     * this method exists solely for the purpose of calling from
     * lookup(DN dn) avoiding the varargs,
     */
    private Entry _lookup( DN dn, Control[] controls, String... attributes )
    {
        messageId.incrementAndGet();

        Entry entry = null;

        try
        {
            entry = session.lookup( dn, controls, attributes );
        }
        catch ( LdapException e )
        {
            LOG.warn( e.getMessage(), e );
        }

        return entry;
    }


    /**
     * {@inheritDoc}
     */
    public boolean exists( String dn ) throws LdapException
    {
        return exists( new DN( dn ) );
    }


    /**
     * {@inheritDoc}
     */
    public boolean exists( DN dn ) throws LdapException
    {
        try
        {
            Entry entry = lookup( dn, SchemaConstants.NO_ATTRIBUTE );

            return entry != null;
        }
        catch ( LdapNoPermissionException lnpe )
        {
            // Special case to deal with insufficient permissions 
            return false;
        }
        catch ( LdapException le )
        {
            throw le;
        }
    }


    /**
     * {@inheritDoc}
     */
    public Entry lookup( DN dn ) throws LdapException
    {
        return _lookup( dn, null );
    }


    /**
     * {@inheritDoc}
     */
    public Entry lookup( String dn ) throws LdapException
    {
        return _lookup( new DN( dn ), null );
    }


    /**
     * {@inheritDoc}
     */
    public ModifyResponse modify( DN dn, Modification... modifications ) throws LdapException
    {
        if ( dn == null )
        {
            LOG.debug( "received a null dn for modification" );
            throw new IllegalArgumentException( "The DN to be modified cannot be null" );
        }

        if ( ( modifications == null ) || ( modifications.length == 0 ) )
        {
            String msg = "Cannot process a ModifyRequest without any modification";
            LOG.debug( msg );
            throw new IllegalArgumentException( msg );
        }

        int newId = messageId.incrementAndGet();

        ModifyResponse resp = new ModifyResponseImpl( newId );
        resp.getLdapResult().setResultCode( ResultCodeEnum.SUCCESS );

        ModifyRequest iModReq = new ModifyRequestImpl( newId );

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
    public ModifyResponse modify( String dn, Modification... modifications ) throws LdapException
    {
        return modify( new DN( dn ), modifications );
    }


    /**
     * {@inheritDoc}
     */
    public ModifyResponse modify( Entry entry, ModificationOperation modOp ) throws LdapException
    {
        if ( entry == null )
        {
            LOG.debug( "received a null entry for modification" );
            throw new IllegalArgumentException( "Entry to be modified cannot be null" );
        }

        int newId = messageId.incrementAndGet();
        ModifyResponse resp = new ModifyResponseImpl( newId );
        resp.getLdapResult().setResultCode( ResultCodeEnum.SUCCESS );

        ModifyRequest iModReq = new ModifyRequestImpl( newId );

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
    public ModifyResponse modify( ModifyRequest modRequest ) throws LdapException
    {
        if ( modRequest == null )
        {
            String msg = "Cannot process a null modifyRequest";
            LOG.debug( msg );
            throw new IllegalArgumentException( msg );
        }

        int newId = messageId.incrementAndGet();

        modRequest.setMessageId( newId );
        ModifyResponse resp = new ModifyResponseImpl( newId );
        resp.getLdapResult().setResultCode( ResultCodeEnum.SUCCESS );

        try
        {
            session.modify( modRequest );
        }
        catch ( LdapException e )
        {
            LOG.warn( e.getMessage(), e );

            resp.getLdapResult().setResultCode( ResultCodeEnum.getResultCode( e ) );
            resp.getLdapResult().setErrorMessage( e.getMessage() );
        }

        addResponseControls( modRequest, resp );
        return resp;
    }


    /**
     * {@inheritDoc}
     */
    public ModifyDnResponse modifyDn( ModifyDnRequest modDnRequest ) throws LdapException
    {
        if ( modDnRequest == null )
        {
            String msg = "Cannot process a null modDnRequest";
            LOG.debug( msg );
            throw new IllegalArgumentException( msg );
        }

        int newId = messageId.incrementAndGet();

        ModifyDnResponse resp = new ModifyDnResponseImpl( newId );
        LdapResult result = resp.getLdapResult();
        result.setResultCode( ResultCodeEnum.SUCCESS );

        if ( modDnRequest.getName().isEmpty() )
        {
            // it is not allowed to modify the name of the Root DSE
            String msg = "Modify DN is not allowed on Root DSE.";
            result.setResultCode( ResultCodeEnum.PROTOCOL_ERROR );
            result.setErrorMessage( msg );
            return resp;
        }

        try
        {
            DN newRdn = null;

            if ( modDnRequest.getNewRdn() != null )
            {
                newRdn = new DN( modDnRequest.getNewRdn().getName(), schemaManager );
            }

            DN oldRdn = new DN( modDnRequest.getName().getRdn().getName(), schemaManager );

            boolean rdnChanged = modDnRequest.getNewRdn() != null
                && !newRdn.getNormName().equals( oldRdn.getNormName() );

            if ( rdnChanged )
            {
                if ( modDnRequest.getNewSuperior() != null )
                {
                    session.moveAndRename( modDnRequest );
                }
                else
                {
                    session.rename( modDnRequest );
                }
            }
            else if ( modDnRequest.getNewSuperior() != null )
            {
                modDnRequest.setNewRdn( null );
                session.move( modDnRequest );
            }
            else
            {
                result.setErrorMessage( "Attempt to move entry onto itself." );
                result.setResultCode( ResultCodeEnum.ENTRY_ALREADY_EXISTS );
                result.setMatchedDn( modDnRequest.getName() );
            }

        }
        catch ( LdapException e )
        {
            LOG.warn( e.getMessage(), e );

            resp.getLdapResult().setResultCode( ResultCodeEnum.getResultCode( e ) );
            resp.getLdapResult().setErrorMessage( e.getMessage() );
        }

        addResponseControls( modDnRequest, resp );
        return resp;
    }


    /**
     * {@inheritDoc}
     */
    public ModifyDnResponse move( DN entryDn, DN newSuperiorDn ) throws LdapException
    {
        if ( entryDn == null )
        {
            String msg = "Cannot process a move of a null DN";
            LOG.debug( msg );
            throw new IllegalArgumentException( msg );
        }

        if ( newSuperiorDn == null )
        {
            String msg = "Cannot process a move to a null DN";
            LOG.debug( msg );
            throw new IllegalArgumentException( msg );
        }

        ModifyDnRequest iModDnReq = new ModifyDnRequestImpl();
        iModDnReq.setName( entryDn );
        iModDnReq.setNewSuperior( newSuperiorDn );

        return modifyDn( iModDnReq );
    }


    /**
     * {@inheritDoc}
     */
    public ModifyDnResponse move( String entryDn, String newSuperiorDn ) throws LdapException
    {
        if ( entryDn == null )
        {
            String msg = "Cannot process a move of a null DN";
            LOG.debug( msg );
            throw new IllegalArgumentException( msg );
        }

        if ( newSuperiorDn == null )
        {
            String msg = "Cannot process a move to a null DN";
            LOG.debug( msg );
            throw new IllegalArgumentException( msg );
        }

        return move( new DN( entryDn ), new DN( newSuperiorDn ) );
    }


    /**
     * {@inheritDoc}
     */
    public ModifyDnResponse rename( DN entryDn, RDN newRdn, boolean deleteOldRdn ) throws LdapException
    {
        if ( entryDn == null )
        {
            String msg = "Cannot process a rename of a null DN";
            LOG.debug( msg );
            throw new IllegalArgumentException( msg );
        }

        if ( newRdn == null )
        {
            String msg = "Cannot process a rename with a null RDN";
            LOG.debug( msg );
            throw new IllegalArgumentException( msg );
        }

        ModifyDnRequest iModDnReq = new ModifyDnRequestImpl();
        iModDnReq.setName( entryDn );
        iModDnReq.setNewRdn( newRdn );
        iModDnReq.setDeleteOldRdn( deleteOldRdn );

        return modifyDn( iModDnReq );
    }


    /**
     * {@inheritDoc}
     */
    public ModifyDnResponse rename( DN entryDn, RDN newRdn ) throws LdapException
    {
        return rename( entryDn, newRdn, false );
    }


    /**
     * {@inheritDoc}
     */
    public ModifyDnResponse rename( String entryDn, String newRdn, boolean deleteOldRdn ) throws LdapException
    {
        return rename( new DN( entryDn ), new RDN( newRdn ), deleteOldRdn );
    }


    /**
     * {@inheritDoc}
     */
    public ModifyDnResponse rename( String entryDn, String newRdn ) throws LdapException
    {
        if ( entryDn == null )
        {
            String msg = "Cannot process a rename of a null DN";
            LOG.debug( msg );
            throw new IllegalArgumentException( msg );
        }

        if ( newRdn == null )
        {
            String msg = "Cannot process a rename with a null RDN";
            LOG.debug( msg );
            throw new IllegalArgumentException( msg );
        }

        return rename( new DN( entryDn ), new RDN( newRdn ) );
    }


    /**
     * Moves and renames the given entryDn.The old RDN will be deleted
     *
     * @see #moveAndRename(DN, DN, boolean)
     */
    public ModifyDnResponse moveAndRename( DN entryDn, DN newDn ) throws LdapException
    {
        return moveAndRename( entryDn, newDn, true );
    }


    /**
     * Moves and renames the given entryDn.The old RDN will be deleted
     *
     * @see #moveAndRename(DN, DN, boolean)
     */
    public ModifyDnResponse moveAndRename( String entryDn, String newDn ) throws LdapException
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
    public ModifyDnResponse moveAndRename( DN entryDn, DN newDn, boolean deleteOldRdn ) throws LdapException
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

        ModifyDnResponse resp = new ModifyDnResponseImpl();
        resp.getLdapResult().setResultCode( ResultCodeEnum.SUCCESS );

        ModifyDnRequest iModDnReq = new ModifyDnRequestImpl();

        iModDnReq.setName( entryDn );
        iModDnReq.setNewRdn( newDn.getRdn() );
        iModDnReq.setNewSuperior( newDn.getParent() );
        iModDnReq.setDeleteOldRdn( deleteOldRdn );

        return modifyDn( iModDnReq );
    }


    /**
     * Moves and renames the given entryDn.The old RDN will be deleted if requested
     *
     * @param entryDn The original entry DN
     * @param newDn The new Entry DN
     * @param deleteOldRdn Tells if the old RDN must be removed
     */
    public ModifyDnResponse moveAndRename( String entryDn, String newDn, boolean deleteOldRdn ) throws LdapException
    {
        return moveAndRename( new DN( entryDn ), new DN( newDn ), deleteOldRdn );
    }


    /**
     * {@inheritDoc}
     */
    public SearchCursor search( SearchRequest searchRequest ) throws LdapException
    {
        if ( searchRequest == null )
        {
            String msg = "Cannot process a null searchRequest";
            LOG.debug( msg );
            throw new IllegalArgumentException( msg );
        }

        try
        {
            int newId = messageId.incrementAndGet();

            searchRequest.setMessageId( newId );

            EntryFilteringCursor entryCursor = session.search( searchRequest );
            entryCursor.beforeFirst();

            //TODO enforce the size and time limits, similar in the way SearchHandler does
            return new EntryToResponseCursor( newId, entryCursor );
        }
        catch ( Exception e )
        {
            LOG.warn( e.getMessage(), e );
        }

        return new EntryToResponseCursor( -1, new EmptyCursor<ClonedServerEntry>() );
    }


    /**
     * {@inheritDoc}
     */
    public SearchCursor search( DN baseDn, String filter, SearchScope scope, String... attributes )
        throws LdapException
    {
        if ( baseDn == null )
        {
            LOG.debug( "received a null dn for a search" );
            throw new IllegalArgumentException( "The base DN cannot be null" );
        }

        // generate some random operation number
        SearchRequest searchRequest = new SearchRequestImpl();

        searchRequest.setBase( baseDn );
        searchRequest.setFilter( filter );
        searchRequest.setScope( scope );
        searchRequest.addAttributes( attributes );
        searchRequest.setDerefAliases( AliasDerefMode.DEREF_ALWAYS );

        return search( searchRequest );
    }


    /**
     * {@inheritDoc}
     */
    public SearchCursor search( String baseDn, String filter, SearchScope scope, String... attributes )
        throws LdapException
    {
        return search( new DN( baseDn ), filter, scope, attributes );
    }


    /**
     * {@inheritDoc}
     */
    public void unBind() throws LdapException
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
    public ExtendedResponse extended( String oid ) throws LdapException
    {
        throw new UnsupportedOperationException(
            "extended operations are not supported on CoreSession based connection" );
    }


    /**
     * {@inheritDoc}
     */
    public ExtendedResponse extended( ExtendedRequest extendedRequest ) throws LdapException
    {
        if ( extendedRequest == null )
        {
            String msg = "Cannot process a null extendedRequest";
            LOG.debug( msg );
            throw new IllegalArgumentException( msg );
        }

        return extended( ( String ) null );

    }


    /**
     * {@inheritDoc}
     */
    public ExtendedResponse extended( OID oid, byte[] value ) throws LdapException
    {
        return extended( ( String ) null );
    }


    /**
     * {@inheritDoc}
     */
    public ExtendedResponse extended( OID oid ) throws LdapException
    {
        return extended( ( String ) null );
    }


    /**
     * {@inheritDoc}
     */
    public ExtendedResponse extended( String oid, byte[] value ) throws LdapException
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
    public BindResponse bind() throws LdapException, IOException
    {
        BindRequest bindReq = new BindRequestImpl();
        bindReq.setName( DN.EMPTY_DN );
        bindReq.setCredentials( ( byte[] ) null );

        return bind( bindReq );
    }


    /**
     * {@inheritDoc}
     */
    public BindResponse bind( BindRequest bindRequest ) throws LdapException, IOException
    {
        if ( bindRequest == null )
        {
            String msg = "Cannot process a null bindRequest";
            LOG.debug( msg );
            throw new IllegalArgumentException( msg );
        }

        int newId = messageId.incrementAndGet();

        BindOperationContext bindContext = new BindOperationContext( null );
        bindContext.setCredentials( bindRequest.getCredentials() );
        bindContext.setDn( new DN( bindRequest.getName() ) );

        OperationManager operationManager = directoryService.getOperationManager();

        BindResponse bindResp = new BindResponseImpl( newId );
        bindResp.getLdapResult().setResultCode( ResultCodeEnum.SUCCESS );

        try
        {
            if ( !bindRequest.isSimple() )
            {
                bindContext.setSaslMechanism( bindRequest.getSaslMechanism() );
            }

            operationManager.bind( bindContext );
            session = bindContext.getSession();

            bindResp.addAllControls( bindContext.getResponseControls() );
        }
        catch ( LdapOperationException e )
        {
            LOG.warn( e.getMessage(), e );
            LdapResult res = bindResp.getLdapResult();
            res.setErrorMessage( e.getMessage() );
            res.setResultCode( e.getResultCode() );
        }

        return bindResp;
    }


    /**
     * {@inheritDoc}
     */
    public BindResponse bind( DN name, String credentials ) throws LdapException, IOException
    {
        byte[] credBytes = ( credentials == null ? StringTools.EMPTY_BYTES : StringTools.getBytesUtf8( credentials ) );

        BindRequest bindReq = new BindRequestImpl();
        bindReq.setName( name );
        bindReq.setCredentials( credBytes );

        return bind( bindReq );
    }


    /**
     * {@inheritDoc}
     */
    public BindResponse bind( String name, String credentials ) throws LdapException, IOException
    {
        return bind( new DN( name ), credentials );
    }


    private void addResponseControls( ResultResponseRequest iReq, Message clientResp )
    {
        Collection<Control> ctrlSet = iReq.getResultResponse().getControls().values();

        for ( Control c : ctrlSet )
        {
            clientResp.addControl( c );
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
