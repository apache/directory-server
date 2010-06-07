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
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.apache.directory.ldap.client.api.LdapConnection;
import org.apache.directory.ldap.client.api.message.AbandonRequest;
import org.apache.directory.ldap.client.api.message.AddRequest;
import org.apache.directory.ldap.client.api.message.AddResponse;
import org.apache.directory.ldap.client.api.message.BindRequest;
import org.apache.directory.ldap.client.api.message.BindResponse;
import org.apache.directory.ldap.client.api.message.CompareRequest;
import org.apache.directory.ldap.client.api.message.CompareResponse;
import org.apache.directory.ldap.client.api.message.DeleteRequest;
import org.apache.directory.ldap.client.api.message.DeleteResponse;
import org.apache.directory.ldap.client.api.message.ExtendedRequest;
import org.apache.directory.ldap.client.api.message.ExtendedResponse;
import org.apache.directory.ldap.client.api.message.LdapResult;
import org.apache.directory.ldap.client.api.message.ModifyDnRequest;
import org.apache.directory.ldap.client.api.message.ModifyDnResponse;
import org.apache.directory.ldap.client.api.message.ModifyRequest;
import org.apache.directory.ldap.client.api.message.ModifyResponse;
import org.apache.directory.ldap.client.api.message.SearchRequest;
import org.apache.directory.ldap.client.api.message.SearchResponse;
import org.apache.directory.ldap.client.api.message.SearchResultEntry;
import org.apache.directory.server.core.filtering.EntryFilteringCursor;
import org.apache.directory.shared.asn1.primitives.OID;
import org.apache.directory.shared.ldap.NotImplementedException;
import org.apache.directory.shared.ldap.cursor.Cursor;
import org.apache.directory.shared.ldap.cursor.EmptyCursor;
import org.apache.directory.shared.ldap.entry.DefaultEntry;
import org.apache.directory.shared.ldap.entry.DefaultModification;
import org.apache.directory.shared.ldap.entry.Entry;
import org.apache.directory.shared.ldap.entry.EntryAttribute;
import org.apache.directory.shared.ldap.entry.Modification;
import org.apache.directory.shared.ldap.entry.ModificationOperation;
import org.apache.directory.shared.ldap.entry.Value;
import org.apache.directory.shared.ldap.exception.LdapException;
import org.apache.directory.shared.ldap.exception.LdapInvalidDnException;
import org.apache.directory.shared.ldap.filter.FilterParser;
import org.apache.directory.shared.ldap.filter.SearchScope;
import org.apache.directory.shared.ldap.message.AddRequestImpl;
import org.apache.directory.shared.ldap.message.AliasDerefMode;
import org.apache.directory.shared.ldap.message.CompareRequestImpl;
import org.apache.directory.shared.ldap.message.DeleteRequestImpl;
import org.apache.directory.shared.ldap.message.ModifyDnRequestImpl;
import org.apache.directory.shared.ldap.message.ModifyRequestImpl;
import org.apache.directory.shared.ldap.message.ResultCodeEnum;
import org.apache.directory.shared.ldap.message.SearchRequestImpl;
import org.apache.directory.shared.ldap.message.internal.InternalAddRequest;
import org.apache.directory.shared.ldap.message.internal.InternalCompareRequest;
import org.apache.directory.shared.ldap.message.internal.InternalDeleteRequest;
import org.apache.directory.shared.ldap.message.internal.InternalModifyDnRequest;
import org.apache.directory.shared.ldap.message.internal.InternalModifyRequest;
import org.apache.directory.shared.ldap.message.internal.InternalSearchRequest;
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
    private SchemaManager sm;

    /** the session's DirectoryService */
    private DirectoryService directoryService;

    private static final Logger LOG = LoggerFactory.getLogger( LdapCoreSessionConnection.class );
    
    public LdapCoreSessionConnection()
    {

    }


    public LdapCoreSessionConnection( CoreSession session )
    {
        setSession( session );
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
        catch( Exception e )
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


    private LdapResult getDefaultResult()
    {
        LdapResult result = new LdapResult();
        result.setResultCode( ResultCodeEnum.SUCCESS );
        return result;
    }

    
    private LdapResult getDefaultCompareResult()
    {
        LdapResult result = new LdapResult();
        result.setResultCode( ResultCodeEnum.COMPARE_TRUE );
        return result;
    }

    
    /**
     * {@inheritDoc}
     */
    public AddResponse add( AddRequest addRequest ) throws LdapException
    {
        InternalAddRequest iadd = new AddRequestImpl( addRequest.getMessageId() );
        iadd.setEntry( addRequest.getEntry() );

        AddResponse resp = new AddResponse();
        resp.setLdapResult( getDefaultResult() );

        try
        {
            session.add( iadd );
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
    public AddResponse add( Entry entry ) throws LdapException
    {
        AddResponse resp = new AddResponse();
        resp.setLdapResult( getDefaultResult() );

        try
        {
            Entry se = new DefaultEntry( sm, entry );

            session.add( se );
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
    public CompareResponse compare( CompareRequest compareRequest ) throws LdapException
    {
        CompareResponse resp = new CompareResponse();
        resp.setLdapResult( getDefaultCompareResult() );

        try
        {
            InternalCompareRequest icompare = new CompareRequestImpl( compareRequest.getMessageId() );

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
        CompareResponse resp = new CompareResponse();
        resp.setLdapResult( getDefaultCompareResult() );

        try
        {
            session.compare( dn, attributeName, value );
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
    public CompareResponse compare( DN dn, String attributeName, String value ) throws LdapException
    {
        CompareResponse resp = new CompareResponse();
        resp.setLdapResult( getDefaultCompareResult() );

        try
        {
            session.compare( dn, attributeName, value );
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
    public CompareResponse compare( String dn, String attributeName, byte[] value ) throws LdapException
    {
        CompareResponse resp = new CompareResponse();
        resp.setLdapResult( getDefaultCompareResult() );

        try
        {
            session.compare( new DN( dn ), attributeName, value );
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
    public CompareResponse compare( String dn, String attributeName, String value ) throws LdapException
    {
        CompareResponse resp = new CompareResponse();
        resp.setLdapResult( getDefaultCompareResult() );

        try
        {
            session.compare( new DN( dn ), attributeName, value );
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
    public CompareResponse compare( DN dn, String attributeName, Value<?> value ) throws LdapException
    {
        CompareResponse resp = new CompareResponse();
        resp.setLdapResult( getDefaultCompareResult() );

        try
        {
            session.compare( dn, attributeName, value.get() );
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
    public CompareResponse compare( String dn, String attributeName, Value<?> value ) throws LdapException
    {
        CompareResponse resp = new CompareResponse();
        resp.setLdapResult( getDefaultCompareResult() );

        try
        {
            session.compare( new DN( dn ), attributeName, value.get() );
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
    public DeleteResponse delete( DeleteRequest deleteRequest ) throws LdapException
    {
        DeleteResponse resp = new DeleteResponse();
        resp.setLdapResult( getDefaultResult() );
        try
        {
            InternalDeleteRequest idelete = new DeleteRequestImpl( deleteRequest.getMessageId() );
            idelete.setName( deleteRequest.getTargetDn() );
            session.delete( idelete );
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
    public DeleteResponse delete( DN dn ) throws LdapException
    {
        DeleteResponse resp = new DeleteResponse();
        resp.setLdapResult( getDefaultResult() );
        
        try
        {
            session.delete( dn );
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
    public DeleteResponse delete( String dn ) throws LdapException
    {
        DeleteResponse resp = new DeleteResponse();
        resp.setLdapResult( getDefaultResult() );
        try
        {
            session.delete( new DN( dn ) );
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
    public boolean doesFutureExistFor( Integer messageId )
    {
        return false;
    }


    /**
     * {@inheritDoc}
     */
    public SchemaManager getSchemaManager()
    {
        return sm;
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
    public SearchResponse lookup( String dn, String... attributes ) throws LdapException
    {
        SearchResultEntry resp = null;
        
        try
        {
            Entry entry = session.lookup( new DN( dn ), attributes );
            resp = new SearchResultEntry();
            resp.setEntry( entry );
        }
        catch ( Exception e )
        {
            // do nothing
            LOG.warn( e.getMessage(), e );
        }

        return resp;
    }


    /**
     * {@inheritDoc}
     */
    public SearchResponse lookup( String dn ) throws LdapException
    {
        SearchResultEntry resp = null;
        try
        {
            Entry entry = session.lookup( new DN( dn ) );
            resp = new SearchResultEntry();
            resp.setEntry( entry );
        }
        catch ( Exception e )
        {
            // do nothing
            LOG.warn( e.getMessage(), e );
        }

        return resp;
    }


    /**
     * {@inheritDoc}
     */
    public ModifyResponse modify( Entry entry, ModificationOperation modOp ) throws LdapException
    {
        ModifyResponse resp = new ModifyResponse();
        resp.setLdapResult( getDefaultResult() );

        try
        {
            List<Modification> mods = new ArrayList<Modification>();
            Iterator<EntryAttribute> itr = entry.iterator();
            while ( itr.hasNext() )
            {
                mods.add( new DefaultModification( modOp, itr.next() ) );
            }
            session.modify( entry.getDn(), mods );
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
    public ModifyResponse modify( ModifyRequest modRequest ) throws LdapException
    {
        ModifyResponse resp = new ModifyResponse();
        resp.setLdapResult( getDefaultResult() );

        try
        {
            InternalModifyRequest iModReq = new ModifyRequestImpl( modRequest.getMessageId() );
            iModReq.setName( modRequest.getDn() );

            Iterator<Modification> itr = modRequest.getMods().iterator();
            while ( itr.hasNext() )
            {
                iModReq.addModification( itr.next() );
            }

            session.modify( iModReq );
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
    public ModifyDnResponse modifyDn( ModifyDnRequest modDnRequest ) throws LdapException
    {
        ModifyDnResponse resp = new ModifyDnResponse();
        resp.setLdapResult( getDefaultResult() );
        try
        {
            InternalModifyDnRequest iModDnReq = new ModifyDnRequestImpl( modDnRequest.getMessageId() );
            iModDnReq.setDeleteOldRdn( modDnRequest.isDeleteOldRdn() );
            iModDnReq.setName( modDnRequest.getEntryDn() );
            iModDnReq.setNewRdn( modDnRequest.getNewRdn() );
            iModDnReq.setNewSuperior( modDnRequest.getNewSuperior() );

            session.move( iModDnReq );
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
    public ModifyDnResponse move( DN entryDn, DN newSuperiorDn ) throws LdapException
    {
        ModifyDnResponse resp = new ModifyDnResponse();
        resp.setLdapResult( getDefaultResult() );
        try
        {
            session.move( entryDn, newSuperiorDn );
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
    public ModifyDnResponse move( String entryDn, String newSuperiorDn ) throws LdapException
    {
        ModifyDnResponse resp = new ModifyDnResponse();
        resp.setLdapResult( getDefaultResult() );
        try
        {
            session.move( new DN( entryDn ), new DN( newSuperiorDn ) );
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
    public ModifyDnResponse rename( DN entryDn, RDN newRdn, boolean deleteOldRdn ) throws LdapException
    {
        ModifyDnResponse resp = new ModifyDnResponse();
        resp.setLdapResult( getDefaultResult() );
        try
        {
            session.rename( entryDn, newRdn, deleteOldRdn );
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
    public ModifyDnResponse rename( DN entryDn, RDN newRdn ) throws LdapException
    {
        ModifyDnResponse resp = new ModifyDnResponse();
        resp.setLdapResult( getDefaultResult() );
        try
        {
            session.rename( entryDn, newRdn, false );
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
    public ModifyDnResponse rename( String entryDn, String newRdn, boolean deleteOldRdn ) throws LdapException
    {
        ModifyDnResponse resp = new ModifyDnResponse();
        resp.setLdapResult( getDefaultResult() );
        try
        {
            session.rename( new DN( entryDn ), new RDN( newRdn ), deleteOldRdn );
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
    public ModifyDnResponse rename( String entryDn, String rdn ) throws LdapException
    {
        ModifyDnResponse resp = new ModifyDnResponse();
        resp.setLdapResult( getDefaultResult() );
        
        try
        {
            DN newDn = new DN( entryDn );
            RDN newRdn = new RDN( rdn );
            session.rename( newDn, newRdn, false );
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
    public Cursor<SearchResponse> search( SearchRequest searchRequest ) throws LdapException
    {
        try
        {
            InternalSearchRequest iSearchReq = new SearchRequestImpl( searchRequest.getMessageId() );
            iSearchReq.setBase( new DN( searchRequest.getBaseDn() ) );
            iSearchReq.setDerefAliases( searchRequest.getDerefAliases() );
            iSearchReq.setFilter( FilterParser.parse( searchRequest.getFilter() ) );
            iSearchReq.setScope( searchRequest.getScope() );
            iSearchReq.setSizeLimit( searchRequest.getSizeLimit() );
            iSearchReq.setTimeLimit( searchRequest.getTimeLimit() );
            iSearchReq.setTypesOnly( searchRequest.getTypesOnly() );

            if( searchRequest.getAttributes() != null )
            {
                for( String at : searchRequest.getAttributes() )
                {
                    iSearchReq.addAttribute( at );
                }
            }

            EntryFilteringCursor entryCursor = session.search( iSearchReq );
            entryCursor.beforeFirst();
            return new EntryToResponseCursor<SearchResponse>( entryCursor );
        }
        catch ( Exception e )
        {
            LOG.warn( e.getMessage(), e );
        }

        return new EmptyCursor<SearchResponse>();
    }


    /**
     * {@inheritDoc}
     */
    public Cursor<SearchResponse> search( String baseDn, String filter, SearchScope scope, String... attributes )
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
    public void unBind() throws Exception
    {
        if( session != null )
        {
            session.unbind();
            session = null;
        }
    }


    // unsupported operations

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
        try
        {
            setSession( directoryService.getSession() );
        }
        catch( Exception e )
        {
            LOG.warn( e.getMessage(), e );
        }
        
        return null;
    }


    /**
     * {@inheritDoc}
     */
    public BindResponse bind( BindRequest bindRequest ) throws LdapException, IOException
    {
        try
        {
            if( bindRequest.isSimple() )
            {
                setSession( directoryService.getSession( new DN( bindRequest.getName() ), bindRequest.getCredentials() ) );
            }
            else
            {
                throw new NotImplementedException( "getting coresession based on SASL mechanism is not implemented yet" );
                // session = directoryService.getSession( new DN( bindRequest.getName() ), bindRequest.getCredentials(), bindRequest.getSaslMechanism() );
            }
        }
        catch( Exception e )
        {
            LOG.warn( e.getMessage(), e );
        }
        
        return null;
    }


    /**
     * {@inheritDoc}
     */
    public BindResponse bind( DN name, String credentials ) throws LdapException, IOException
    {
        try
        {
            byte[] credBytes = ( credentials == null ? StringTools.EMPTY_BYTES : StringTools.getBytesUtf8( credentials ) );

            setSession( directoryService.getSession( name, credBytes ) );
        }
        catch ( LdapException e )
        {
            LOG.warn( e.getMessage(), e );
            throw e;
        }
        
        return null;
    }


    /**
     * {@inheritDoc}
     */
    public BindResponse bind( String name, String credentials ) throws LdapException, IOException
    {
        try
        {
            return bind( new DN( name ), credentials );
        }
        catch( LdapInvalidDnException e )
        {
            throw new LdapException( e );
        }
    }


    public void setSession( CoreSession session )
    {
        this.session = session;
        this.directoryService = session.getDirectoryService();
        this.sm = directoryService.getSchemaManager();
    }

}
