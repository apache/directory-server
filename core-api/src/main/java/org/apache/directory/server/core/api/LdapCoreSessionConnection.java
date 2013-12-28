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
package org.apache.directory.server.core.api;


import static org.apache.directory.api.ldap.model.message.ResultCodeEnum.processResponse;

import java.io.IOException;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;

import org.apache.directory.api.asn1.util.Oid;
import org.apache.directory.api.ldap.codec.api.BinaryAttributeDetector;
import org.apache.directory.api.ldap.codec.api.LdapApiService;
import org.apache.directory.api.ldap.model.constants.SchemaConstants;
import org.apache.directory.api.ldap.model.cursor.Cursor;
import org.apache.directory.api.ldap.model.cursor.EmptyCursor;
import org.apache.directory.api.ldap.model.cursor.EntryCursor;
import org.apache.directory.api.ldap.model.cursor.SearchCursor;
import org.apache.directory.api.ldap.model.entry.Attribute;
import org.apache.directory.api.ldap.model.entry.DefaultModification;
import org.apache.directory.api.ldap.model.entry.Entry;
import org.apache.directory.api.ldap.model.entry.Modification;
import org.apache.directory.api.ldap.model.entry.ModificationOperation;
import org.apache.directory.api.ldap.model.entry.Value;
import org.apache.directory.api.ldap.model.exception.LdapException;
import org.apache.directory.api.ldap.model.exception.LdapNoPermissionException;
import org.apache.directory.api.ldap.model.exception.LdapOperationException;
import org.apache.directory.api.ldap.model.message.AbandonRequest;
import org.apache.directory.api.ldap.model.message.AddRequest;
import org.apache.directory.api.ldap.model.message.AddRequestImpl;
import org.apache.directory.api.ldap.model.message.AddResponse;
import org.apache.directory.api.ldap.model.message.AddResponseImpl;
import org.apache.directory.api.ldap.model.message.AliasDerefMode;
import org.apache.directory.api.ldap.model.message.BindRequest;
import org.apache.directory.api.ldap.model.message.BindRequestImpl;
import org.apache.directory.api.ldap.model.message.BindResponse;
import org.apache.directory.api.ldap.model.message.BindResponseImpl;
import org.apache.directory.api.ldap.model.message.CompareRequest;
import org.apache.directory.api.ldap.model.message.CompareRequestImpl;
import org.apache.directory.api.ldap.model.message.CompareResponse;
import org.apache.directory.api.ldap.model.message.CompareResponseImpl;
import org.apache.directory.api.ldap.model.message.Control;
import org.apache.directory.api.ldap.model.message.DeleteRequest;
import org.apache.directory.api.ldap.model.message.DeleteRequestImpl;
import org.apache.directory.api.ldap.model.message.DeleteResponse;
import org.apache.directory.api.ldap.model.message.DeleteResponseImpl;
import org.apache.directory.api.ldap.model.message.ExtendedRequest;
import org.apache.directory.api.ldap.model.message.ExtendedResponse;
import org.apache.directory.api.ldap.model.message.LdapResult;
import org.apache.directory.api.ldap.model.message.Message;
import org.apache.directory.api.ldap.model.message.ModifyDnRequest;
import org.apache.directory.api.ldap.model.message.ModifyDnRequestImpl;
import org.apache.directory.api.ldap.model.message.ModifyDnResponse;
import org.apache.directory.api.ldap.model.message.ModifyDnResponseImpl;
import org.apache.directory.api.ldap.model.message.ModifyRequest;
import org.apache.directory.api.ldap.model.message.ModifyRequestImpl;
import org.apache.directory.api.ldap.model.message.ModifyResponse;
import org.apache.directory.api.ldap.model.message.ModifyResponseImpl;
import org.apache.directory.api.ldap.model.message.ResultCodeEnum;
import org.apache.directory.api.ldap.model.message.ResultResponseRequest;
import org.apache.directory.api.ldap.model.message.SearchRequest;
import org.apache.directory.api.ldap.model.message.SearchRequestImpl;
import org.apache.directory.api.ldap.model.message.SearchScope;
import org.apache.directory.api.ldap.model.name.Dn;
import org.apache.directory.api.ldap.model.name.Rdn;
import org.apache.directory.api.ldap.model.schema.SchemaManager;
import org.apache.directory.ldap.client.api.AbstractLdapConnection;
import org.apache.directory.ldap.client.api.EntryCursorImpl;
import org.apache.directory.server.core.api.interceptor.context.BindOperationContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 *  An implementation of LdapConnection based on the CoreSession.
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 */
public class LdapCoreSessionConnection extends AbstractLdapConnection
{
    /** The logger for this class */
    private static final Logger LOG = LoggerFactory.getLogger( LdapCoreSessionConnection.class );

    /** the CoreSession object */
    private CoreSession session;

    /** the session's DirectoryService */
    private DirectoryService directoryService;


    public LdapCoreSessionConnection()
    {
        super();
    }


    public LdapCoreSessionConnection( DirectoryService directoryService )
    {
        super();
        setDirectoryService( directoryService );
    }


    public LdapCoreSessionConnection( CoreSession session )
    {
        super();
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
            IOException ioe = new IOException( e.getMessage() );
            ioe.initCause( e );
            throw ioe;
        }

        return true;
    }


    /**
     * {@inheritDoc}
     */
    public boolean connect() throws LdapException
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
            resp.getLdapResult().setDiagnosticMessage( e.getMessage() );
        }

        addResponseControls( addRequest, resp );
        return resp;
    }


    /**
     * {@inheritDoc}
     */
    public void add( Entry entry ) throws LdapException
    {
        if ( entry == null )
        {
            String msg = "Cannot add an empty entry";
            LOG.debug( msg );
            throw new IllegalArgumentException( msg );
        }

        AddRequest addRequest = new AddRequestImpl();
        addRequest.setEntry( entry );
        addRequest.setEntryDn( entry.getDn() );

        AddResponse addResponse = add( addRequest );

        processResponse( addResponse );
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

        addResponseControls( compareRequest, resp );
        return resp;
    }


    /**
     * {@inheritDoc}
     */
    public boolean compare( Dn dn, String attributeName, byte[] value ) throws LdapException
    {
        CompareRequest compareRequest = new CompareRequestImpl();
        compareRequest.setName( dn );
        compareRequest.setAttributeId( attributeName );
        compareRequest.setAssertionValue( value );

        CompareResponse compareResponse = compare( compareRequest );

        return processResponse( compareResponse );
    }


    /**
     * {@inheritDoc}
     */
    public boolean compare( Dn dn, String attributeName, String value ) throws LdapException
    {
        CompareRequest compareRequest = new CompareRequestImpl();
        compareRequest.setName( dn );
        compareRequest.setAttributeId( attributeName );
        compareRequest.setAssertionValue( value );

        CompareResponse compareResponse = compare( compareRequest );

        return processResponse( compareResponse );
    }


    /**
     * {@inheritDoc}
     */
    public boolean compare( String dn, String attributeName, byte[] value ) throws LdapException
    {
        return compare( new Dn( schemaManager, dn ), attributeName, value );
    }


    /**
     * {@inheritDoc}
     */
    public boolean compare( String dn, String attributeName, String value ) throws LdapException
    {
        return compare( new Dn( schemaManager, dn ), attributeName, value );
    }


    /**
     * {@inheritDoc}
     */
    public boolean compare( Dn dn, String attributeName, Value<?> value ) throws LdapException
    {
        CompareRequest compareRequest = new CompareRequestImpl();
        compareRequest.setName( dn );
        compareRequest.setAttributeId( attributeName );

        if ( value.isHumanReadable() )
        {
            compareRequest.setAssertionValue( value.getString() );
        }
        else
        {
            compareRequest.setAssertionValue( value.getBytes() );
        }

        CompareResponse compareResponse = compare( compareRequest );

        return processResponse( compareResponse );
    }


    /**
     * {@inheritDoc}
     */
    public boolean compare( String dn, String attributeName, Value<?> value ) throws LdapException
    {
        return compare( new Dn( schemaManager, dn ), attributeName, value );
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
            resp.getLdapResult().setDiagnosticMessage( e.getMessage() );
        }

        addResponseControls( deleteRequest, resp );

        return resp;
    }


    /**
     * {@inheritDoc}
     */
    public void delete( Dn dn ) throws LdapException
    {
        DeleteRequest deleteRequest = new DeleteRequestImpl();
        deleteRequest.setName( dn );

        DeleteResponse deleteResponse = delete( deleteRequest );

        processResponse( deleteResponse );
    }


    /**
     * {@inheritDoc}
     */
    public void delete( String dn ) throws LdapException
    {
        delete( new Dn( schemaManager, dn ) );
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
    public LdapApiService getCodecService()
    {
        return codec;
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
    public Entry lookup( Dn dn, String... attributes ) throws LdapException
    {
        return lookup( dn, null, attributes );
    }


    /**
     * {@inheritDoc}
     */
    public Entry lookup( Dn dn, Control[] controls, String... attributes ) throws LdapException
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
    public Entry lookup( String dn, String... attributes ) throws LdapException
    {
        Dn baseDn = new Dn( schemaManager, dn );

        return lookup( baseDn, null, attributes );
    }


    /**
     * {@inheritDoc}
     */
    public Entry lookup( String dn, Control[] controls, String... attributes ) throws LdapException
    {
        Dn baseDn = new Dn( schemaManager, dn );

        return lookup( baseDn, controls, attributes );
    }


    /**
     * {@inheritDoc}
     */
    public boolean exists( String dn ) throws LdapException
    {
        return exists( new Dn( schemaManager, dn ) );
    }


    /**
     * {@inheritDoc}
     */
    public boolean exists( Dn dn ) throws LdapException
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
    public Entry getRootDse() throws LdapException
    {
        return lookup( Dn.ROOT_DSE, SchemaConstants.ALL_USER_ATTRIBUTES_ARRAY );
    }


    /**
     * {@inheritDoc}
     */
    public Entry getRootDse( String... attributes ) throws LdapException
    {
        return lookup( Dn.ROOT_DSE, attributes );
    }


    /**
     * {@inheritDoc}
     */
    public Entry lookup( Dn dn ) throws LdapException
    {
        return lookup( dn, ( String[] ) null );
    }


    /**
     * {@inheritDoc}
     */
    public Entry lookup( String dn ) throws LdapException
    {
        return lookup( new Dn( schemaManager, dn ), ( String[] ) null );
    }


    /**
     * {@inheritDoc}
     */
    public void modify( Dn dn, Modification... modifications ) throws LdapException
    {
        if ( dn == null )
        {
            LOG.debug( "received a null dn for modification" );
            throw new IllegalArgumentException( "The Dn to be modified cannot be null" );
        }

        if ( ( modifications == null ) || ( modifications.length == 0 ) )
        {
            String msg = "Cannot process a ModifyRequest without any modification";
            LOG.debug( msg );
            throw new IllegalArgumentException( msg );
        }

        int newId = messageId.incrementAndGet();

        ModifyRequest modifyRequest = new ModifyRequestImpl();
        modifyRequest.setMessageId( newId );

        modifyRequest.setName( dn );

        for ( Modification modification : modifications )
        {
            modifyRequest.addModification( modification );
        }

        ModifyResponse modifyResponse = modify( modifyRequest );

        processResponse( modifyResponse );
    }


    /**
     * {@inheritDoc}
     */
    public void modify( String dn, Modification... modifications ) throws LdapException
    {
        modify( new Dn( schemaManager, dn ), modifications );
    }


    /**
     * {@inheritDoc}
     */
    public void modify( Entry entry, ModificationOperation modOp ) throws LdapException
    {
        if ( entry == null )
        {
            LOG.debug( "received a null entry for modification" );
            throw new IllegalArgumentException( "Entry to be modified cannot be null" );
        }

        int newId = messageId.incrementAndGet();
        ModifyRequest modifyRequest = new ModifyRequestImpl();
        modifyRequest.setMessageId( newId );

        modifyRequest.setName( entry.getDn() );

        Iterator<Attribute> itr = entry.iterator();

        while ( itr.hasNext() )
        {
            modifyRequest.addModification( new DefaultModification( modOp, itr.next() ) );
        }

        ModifyResponse modifyResponse = modify( modifyRequest );

        processResponse( modifyResponse );
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
            resp.getLdapResult().setDiagnosticMessage( e.getMessage() );
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
            String msg = "Modify Dn is not allowed on Root DSE.";
            result.setResultCode( ResultCodeEnum.PROTOCOL_ERROR );
            result.setDiagnosticMessage( msg );
            return resp;
        }

        try
        {
            Dn newRdn = null;

            if ( modDnRequest.getNewRdn() != null )
            {
                newRdn = new Dn( schemaManager, modDnRequest.getNewRdn().getName() );
            }

            Dn oldRdn = new Dn( schemaManager, modDnRequest.getName().getRdn().getName() );

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
                // This might be a simple change, we will update the DN and the entry
                // with the new provided value by using a modify operation later on
                session.rename( modDnRequest );
            }

        }
        catch ( LdapException e )
        {
            LOG.warn( e.getMessage(), e );

            resp.getLdapResult().setResultCode( ResultCodeEnum.getResultCode( e ) );
            resp.getLdapResult().setDiagnosticMessage( e.getMessage() );
        }

        addResponseControls( modDnRequest, resp );
        return resp;
    }


    /**
     * {@inheritDoc}
     */
    public void move( Dn entryDn, Dn newSuperiorDn ) throws LdapException
    {
        if ( entryDn == null )
        {
            String msg = "Cannot process a move of a null Dn";
            LOG.debug( msg );
            throw new IllegalArgumentException( msg );
        }

        if ( newSuperiorDn == null )
        {
            String msg = "Cannot process a move to a null Dn";
            LOG.debug( msg );
            throw new IllegalArgumentException( msg );
        }

        ModifyDnRequest iModDnReq = new ModifyDnRequestImpl();
        iModDnReq.setName( entryDn );
        iModDnReq.setNewSuperior( newSuperiorDn );

        ModifyDnResponse modifyDnResponse = modifyDn( iModDnReq );
        processResponse( modifyDnResponse );
    }


    /**
     * {@inheritDoc}
     */
    public void move( String entryDn, String newSuperiorDn ) throws LdapException
    {
        if ( entryDn == null )
        {
            String msg = "Cannot process a move of a null Dn";
            LOG.debug( msg );
            throw new IllegalArgumentException( msg );
        }

        if ( newSuperiorDn == null )
        {
            String msg = "Cannot process a move to a null Dn";
            LOG.debug( msg );
            throw new IllegalArgumentException( msg );
        }

        move( new Dn( schemaManager, entryDn ), new Dn( schemaManager, newSuperiorDn ) );
    }


    /**
     * {@inheritDoc}
     */
    public void rename( Dn entryDn, Rdn newRdn, boolean deleteOldRdn ) throws LdapException
    {
        if ( entryDn == null )
        {
            String msg = "Cannot process a rename of a null Dn";
            LOG.debug( msg );
            throw new IllegalArgumentException( msg );
        }

        if ( newRdn == null )
        {
            String msg = "Cannot process a rename with a null Rdn";
            LOG.debug( msg );
            throw new IllegalArgumentException( msg );
        }

        ModifyDnRequest modifyDnRequest = new ModifyDnRequestImpl();
        modifyDnRequest.setName( entryDn );
        modifyDnRequest.setNewRdn( newRdn );
        modifyDnRequest.setDeleteOldRdn( deleteOldRdn );

        ModifyDnResponse modifyDnResponse = modifyDn( modifyDnRequest );
        processResponse( modifyDnResponse );

    }


    /**
     * {@inheritDoc}
     */
    public void rename( Dn entryDn, Rdn newRdn ) throws LdapException
    {
        rename( entryDn, newRdn, false );
    }


    /**
     * {@inheritDoc}
     */
    public void rename( String entryDn, String newRdn, boolean deleteOldRdn ) throws LdapException
    {
        rename( new Dn( schemaManager, entryDn ), new Rdn( newRdn ), deleteOldRdn );
    }


    /**
     * {@inheritDoc}
     */
    public void rename( String entryDn, String newRdn ) throws LdapException
    {
        if ( entryDn == null )
        {
            String msg = "Cannot process a rename of a null Dn";
            LOG.debug( msg );
            throw new IllegalArgumentException( msg );
        }

        if ( newRdn == null )
        {
            String msg = "Cannot process a rename with a null Rdn";
            LOG.debug( msg );
            throw new IllegalArgumentException( msg );
        }

        rename( new Dn( schemaManager, entryDn ), new Rdn( newRdn ) );
    }


    /**
     * Moves and renames the given entryDn.The old Rdn will be deleted
     *
     * @see #moveAndRename(org.apache.directory.api.ldap.model.name.Dn, org.apache.directory.api.ldap.model.name.Dn, boolean)
     */
    public void moveAndRename( Dn entryDn, Dn newDn ) throws LdapException
    {
        moveAndRename( entryDn, newDn, true );
    }


    /**
     * Moves and renames the given entryDn.The old Rdn will be deleted
     *
     * @see #moveAndRename(org.apache.directory.api.ldap.model.name.Dn, org.apache.directory.api.ldap.model.name.Dn, boolean)
     */
    public void moveAndRename( String entryDn, String newDn ) throws LdapException
    {
        moveAndRename( new Dn( schemaManager, entryDn ), new Dn( schemaManager, newDn ), true );
    }


    /**
     * Moves and renames the given entryDn.The old Rdn will be deleted if requested
     *
     * @param entryDn The original entry Dn
     * @param newDn The new Entry Dn
     * @param deleteOldRdn Tells if the old Rdn must be removed
     */
    public void moveAndRename( Dn entryDn, Dn newDn, boolean deleteOldRdn ) throws LdapException
    {
        // Check the parameters first
        if ( entryDn == null )
        {
            throw new IllegalArgumentException( "The entry Dn must not be null" );
        }

        if ( entryDn.isRootDse() )
        {
            throw new IllegalArgumentException( "The RootDSE cannot be moved" );
        }

        if ( newDn == null )
        {
            throw new IllegalArgumentException( "The new Dn must not be null" );
        }

        if ( newDn.isRootDse() )
        {
            throw new IllegalArgumentException( "The RootDSE cannot be the target" );
        }

        ModifyDnResponse resp = new ModifyDnResponseImpl();
        resp.getLdapResult().setResultCode( ResultCodeEnum.SUCCESS );

        ModifyDnRequest modifyDnRequest = new ModifyDnRequestImpl();

        modifyDnRequest.setName( entryDn );
        modifyDnRequest.setNewRdn( newDn.getRdn() );
        modifyDnRequest.setNewSuperior( newDn.getParent() );
        modifyDnRequest.setDeleteOldRdn( deleteOldRdn );

        ModifyDnResponse modifyDnResponse = modifyDn( modifyDnRequest );
        processResponse( modifyDnResponse );
    }


    /**
     * Moves and renames the given entryDn.The old Rdn will be deleted if requested
     *
     * @param entryDn The original entry Dn
     * @param newDn The new Entry Dn
     * @param deleteOldRdn Tells if the old Rdn must be removed
     */
    public void moveAndRename( String entryDn, String newDn, boolean deleteOldRdn ) throws LdapException
    {
        moveAndRename( new Dn( schemaManager, entryDn ), new Dn( schemaManager, newDn ), deleteOldRdn );
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

            Cursor<Entry> entryCursor = session.search( searchRequest );
            entryCursor.beforeFirst();

            //TODO enforce the size and time limits, similar in the way SearchHandler does
            return new EntryToResponseCursor( newId, entryCursor );
        }
        catch ( Exception e )
        {
            LOG.warn( e.getMessage(), e );
        }

        return new EntryToResponseCursor( -1, new EmptyCursor<Entry>() );
    }


    /**
     * {@inheritDoc}
     */
    public EntryCursor search( Dn baseDn, String filter, SearchScope scope, String... attributes )
        throws LdapException
    {
        if ( baseDn == null )
        {
            LOG.debug( "received a null dn for a search" );
            throw new IllegalArgumentException( "The base Dn cannot be null" );
        }

        // generate some random operation number
        SearchRequest searchRequest = new SearchRequestImpl();

        searchRequest.setBase( baseDn );
        searchRequest.setFilter( filter );
        searchRequest.setScope( scope );
        searchRequest.addAttributes( attributes );
        searchRequest.setDerefAliases( AliasDerefMode.DEREF_ALWAYS );

        return new EntryCursorImpl( search( searchRequest ) );
    }


    /**
     * {@inheritDoc}
     */
    public EntryCursor search( String baseDn, String filter, SearchScope scope, String... attributes )
        throws LdapException
    {
        return search( new Dn( schemaManager, baseDn ), filter, scope, attributes );
    }


    /**
     * {@inheritDoc}
     */
    public void unBind() throws LdapException
    {
        messageId.set( 0 );

        if ( session != null )
        {
            // No need to unbind if the session is anonymous
            if ( !session.isAnonymous() )
            {
                session.unbind();
            }

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
    public ExtendedResponse extended( Oid oid, byte[] value ) throws LdapException
    {
        return extended( ( String ) null );
    }


    /**
     * {@inheritDoc}
     */
    public ExtendedResponse extended( Oid oid ) throws LdapException
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
    public void bind() throws LdapException
    {
        throw new UnsupportedOperationException(
            "Bind operation using LdapConnectionConfig are not supported on CoreSession based connection" );
    }


    /**
     * {@inheritDoc}
     */
    public void anonymousBind() throws LdapException
    {
        BindRequest bindRequest = new BindRequestImpl();
        bindRequest.setName( "" );
        bindRequest.setCredentials( ( byte[] ) null );

        BindResponse bindResponse = bind( bindRequest );

        processResponse( bindResponse );
    }


    /**
     * {@inheritDoc}
     */
    public BindResponse bind( BindRequest bindRequest ) throws LdapException
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

        bindContext.setDn( bindRequest.getDn().apply( directoryService.getSchemaManager() ) );
        bindContext.setInterceptors( directoryService.getInterceptors( OperationEnum.BIND ) );

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
            res.setDiagnosticMessage( e.getMessage() );
            res.setResultCode( e.getResultCode() );
        }

        return bindResp;
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
        this.session = directoryService.getAdminSession();
    }


    /**
     * {@inheritDoc}
     */
    @Override
    public BinaryAttributeDetector getBinaryAttributeDetector()
    {
        return null;
    }


    /**
     * {@inheritDoc}
     */
    @Override
    public void setBinaryAttributeDetector( BinaryAttributeDetector binaryAttributeDetector )
    {
        // Does nothing
    }


    /**
     * {@inheritDoc}
     */
    @Override
    public void setSchemaManager( SchemaManager schemaManager )
    {
        this.schemaManager = schemaManager;
    }
}
