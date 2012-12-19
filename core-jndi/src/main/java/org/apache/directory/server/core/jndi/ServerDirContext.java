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
package org.apache.directory.server.core.jndi;


import java.io.Serializable;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Hashtable;
import java.util.List;

import javax.naming.Name;
import javax.naming.NamingEnumeration;
import javax.naming.NamingException;
import javax.naming.Reference;
import javax.naming.Referenceable;
import javax.naming.directory.Attributes;
import javax.naming.directory.DirContext;
import javax.naming.directory.InvalidAttributeIdentifierException;
import javax.naming.directory.InvalidAttributeValueException;
import javax.naming.directory.InvalidAttributesException;
import javax.naming.directory.InvalidSearchFilterException;
import javax.naming.directory.ModificationItem;
import javax.naming.directory.SearchControls;
import javax.naming.directory.SearchResult;
import javax.naming.event.EventDirContext;
import javax.naming.event.NamingListener;
import javax.naming.ldap.LdapName;
import javax.naming.spi.DirStateFactory;
import javax.naming.spi.DirectoryManager;

import org.apache.directory.api.util.Strings;
import org.apache.directory.server.core.api.CoreSession;
import org.apache.directory.server.core.api.DirectoryService;
import org.apache.directory.server.core.api.LdapPrincipal;
import org.apache.directory.server.core.api.entry.ServerEntryUtils;
import org.apache.directory.server.core.api.event.DirectoryListener;
import org.apache.directory.server.core.api.event.NotificationCriteria;
import org.apache.directory.server.core.api.filtering.EntryFilteringCursor;
import org.apache.directory.server.core.api.interceptor.context.HasEntryOperationContext;
import org.apache.directory.server.i18n.I18n;
import org.apache.directory.shared.ldap.model.entry.Attribute;
import org.apache.directory.shared.ldap.model.entry.AttributeUtils;
import org.apache.directory.shared.ldap.model.entry.BinaryValue;
import org.apache.directory.shared.ldap.model.entry.Entry;
import org.apache.directory.shared.ldap.model.entry.Modification;
import org.apache.directory.shared.ldap.model.exception.LdapException;
import org.apache.directory.shared.ldap.model.exception.LdapInvalidAttributeTypeException;
import org.apache.directory.shared.ldap.model.exception.LdapNoSuchAttributeException;
import org.apache.directory.shared.ldap.model.filter.AndNode;
import org.apache.directory.shared.ldap.model.filter.BranchNode;
import org.apache.directory.shared.ldap.model.filter.EqualityNode;
import org.apache.directory.shared.ldap.model.filter.ExprNode;
import org.apache.directory.shared.ldap.model.filter.FilterParser;
import org.apache.directory.shared.ldap.model.filter.PresenceNode;
import org.apache.directory.shared.ldap.model.filter.SimpleNode;
import org.apache.directory.shared.ldap.model.message.AliasDerefMode;
import org.apache.directory.shared.ldap.model.message.SearchScope;
import org.apache.directory.shared.ldap.model.name.Dn;
import org.apache.directory.shared.ldap.util.JndiUtils;


/**
 * The DirContext implementation for the Server Side JNDI LDAP provider.
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 */
public abstract class ServerDirContext extends ServerContext implements EventDirContext
{
    // ------------------------------------------------------------------------
    // Constructors
    // ------------------------------------------------------------------------

    /**
     * Creates a new ServerDirContext by reading the PROVIDER_URL to resolve the
     * distinguished name for this context.
     *
     * @param service the parent service that manages this context
     * @param env the environment used for this context
     * @throws NamingException if something goes wrong
     */
    public ServerDirContext( DirectoryService service, Hashtable<String, Object> env ) throws Exception
    {
        super( service, env );
    }


    /**
     * Creates a new ServerDirContext with a distinguished name which is used to
     * set the PROVIDER_URL to the distinguished name for this context.
     *
     * @param principal the principal which is propagated
     * @param dn the distinguished name of this context
     */
    public ServerDirContext( DirectoryService service, LdapPrincipal principal, Name dn ) throws Exception
    {
        super( service, principal, dn );
    }


    // ------------------------------------------------------------------------
    // DirContext Implementations
    // ------------------------------------------------------------------------

    public ServerDirContext( DirectoryService service, CoreSession session, Name bindDn ) throws Exception
    {
        super( service, session, bindDn );
    }


    /**
     * @see javax.naming.directory.DirContext#getAttributes(java.lang.String)
     */
    public Attributes getAttributes( String name ) throws NamingException
    {
        return getAttributes( new LdapName( name ) );
    }


    /**
     * @see javax.naming.directory.DirContext#getAttributes(javax.naming.Name)
     */
    public Attributes getAttributes( Name name ) throws NamingException
    {
        Attributes attrs = null;

        try
        {
            attrs = ServerEntryUtils.toBasicAttributes( doLookupOperation( buildTarget( JndiUtils.fromName( name ) ) ) );
        }
        catch ( Exception e )
        {
            JndiUtils.wrap( e );
        }

        return attrs;
    }


    /**
     * @see javax.naming.directory.DirContext#getAttributes(java.lang.String,
     *      java.lang.String[])
     */
    public Attributes getAttributes( String name, String[] attrIds ) throws NamingException
    {
        return getAttributes( new LdapName( name ), attrIds );
    }


    /**
     * @see javax.naming.directory.DirContext#getAttributes(javax.naming.Name,
     *      java.lang.String[])
     */
    public Attributes getAttributes( Name name, String[] attrIds ) throws NamingException
    {
        Attributes attrs = null;
        try
        {
            attrs = ServerEntryUtils.toBasicAttributes( doLookupOperation( buildTarget( JndiUtils.fromName( name ) ),
                attrIds ) );
        }
        catch ( Exception e )
        {
            JndiUtils.wrap( e );
        }

        return attrs;
    }


    /**
     * @see javax.naming.directory.DirContext#modifyAttributes(java.lang.String,
     *      int, javax.naming.directory.Attributes)
     */
    public void modifyAttributes( String name, int modOp, Attributes attrs ) throws NamingException
    {
        modifyAttributes( new LdapName( name ), modOp, AttributeUtils.toCaseInsensitive( attrs ) );
    }


    /**
     * @see javax.naming.directory.DirContext#modifyAttributes(java.lang.String,
     *      int, javax.naming.directory.Attributes)
     */
    public void modifyAttributes( Name name, int modOp, Attributes attrs ) throws NamingException
    {
        List<ModificationItem> modItems = null;

        if ( attrs != null )
        {
            modItems = new ArrayList<ModificationItem>( attrs.size() );
            NamingEnumeration<? extends javax.naming.directory.Attribute> e = attrs.getAll();

            while ( e.hasMore() )
            {
                modItems.add( new ModificationItem( modOp, e.next() ) );
            }
        }

        List<Modification> newMods = null;

        try
        {
            newMods = ServerEntryUtils.convertToServerModification(
                modItems,
                getDirectoryService().getSchemaManager() );
        }
        catch ( LdapNoSuchAttributeException lnsae )
        {
            throw new InvalidAttributesException( lnsae.getMessage() );
        }
        catch ( LdapException le )
        {
            throw new InvalidAttributeValueException( le.getMessage() );
        }

        try
        {
            doModifyOperation( buildTarget( JndiUtils.fromName( name ) ), newMods );
        }
        catch ( Exception e )
        {
            JndiUtils.wrap( e );
        }
    }


    /**
     * @see javax.naming.directory.DirContext#modifyAttributes(java.lang.String,
     *      javax.naming.directory.ModificationItem[])
     */
    public void modifyAttributes( String name, ModificationItem[] mods ) throws NamingException
    {
        modifyAttributes( new LdapName( name ), mods );
    }


    /**
     * @see javax.naming.directory.DirContext#modifyAttributes(
     * javax.naming.Name, javax.naming.directory.ModificationItem[])
     */
    public void modifyAttributes( Name name, ModificationItem[] mods ) throws NamingException
    {
        List<Modification> newMods;

        try
        {
            newMods = ServerEntryUtils
                .toServerModification( mods, getDirectoryService().getSchemaManager() );
        }
        catch ( LdapException le )
        {
            throw new InvalidAttributesException( le.getMessage() );
        }

        try
        {
            doModifyOperation( buildTarget( JndiUtils.fromName( name ) ), newMods );
        }
        catch ( Exception e )
        {
            JndiUtils.wrap( e );
        }
    }


    /**
     * @see javax.naming.directory.DirContext#modifyAttributes(
     * javax.naming.Name, javax.naming.directory.ModificationItem[])
     */
    public void modifyAttributes( Name name, List<ModificationItem> mods ) throws NamingException
    {
        List<Modification> newMods;
        try
        {
            newMods = ServerEntryUtils
                .convertToServerModification( mods,
                    getDirectoryService().getSchemaManager() );
        }
        catch ( LdapException le )
        {
            throw new InvalidAttributesException( le.getMessage() );
        }

        try
        {
            doModifyOperation( buildTarget( JndiUtils.fromName( name ) ), newMods );
        }
        catch ( Exception e )
        {
            JndiUtils.wrap( e );
        }
    }


    /**
     * @see javax.naming.directory.DirContext#bind(java.lang.String,
     *      java.lang.Object, javax.naming.directory.Attributes)
     */
    public void bind( String name, Object obj, Attributes attrs ) throws NamingException
    {
        bind( new LdapName( name ), obj, AttributeUtils.toCaseInsensitive( attrs ) );
    }


    /**
     * @see javax.naming.directory.DirContext#bind(javax.naming.Name,
     *      java.lang.Object, javax.naming.directory.Attributes)
     */
    public void bind( Name name, Object obj, Attributes attrs ) throws NamingException
    {
        if ( ( null == obj ) && ( null == attrs ) )
        {
            throw new NamingException( I18n.err( I18n.ERR_499 ) );
        }

        // A null attrs defaults this to the Context.bind() operation
        if ( null == attrs )
        {
            super.bind( name, obj );
            return;
        }

        Dn target = buildTarget( JndiUtils.fromName( name ) );

        Entry serverEntry = null;

        try
        {
            serverEntry = ServerEntryUtils.toServerEntry( AttributeUtils.toCaseInsensitive( attrs ), target,
                getDirectoryService().getSchemaManager() );
        }
        catch ( LdapInvalidAttributeTypeException liate )
        {
            throw new InvalidAttributesException( liate.getMessage() );
        }

        // No object binding so we just add the attributes
        if ( null == obj )
        {
            Entry clone = serverEntry.clone();
            try
            {
                doAddOperation( target, clone );
            }
            catch ( Exception e )
            {
                JndiUtils.wrap( e );
            }
            return;
        }

        // First, use state factories to do a transformation
        DirStateFactory.Result res = DirectoryManager.getStateToBind( obj, name, this, getEnvironment(), attrs );
        Entry outServerEntry = null;

        try
        {
            outServerEntry = ServerEntryUtils.toServerEntry(
                res.getAttributes(), target, getDirectoryService().getSchemaManager() );
        }
        catch ( LdapInvalidAttributeTypeException le )
        {
            throw new InvalidAttributesException( le.getMessage() );
        }

        if ( outServerEntry != serverEntry )
        {
            Entry clone = serverEntry.clone();

            if ( ( outServerEntry != null ) && ( outServerEntry.size() > 0 ) )
            {
                for ( Attribute attribute : outServerEntry )
                {
                    try
                    {
                        clone.put( attribute );
                    }
                    catch ( LdapException e )
                    {
                        // TODO Auto-generated catch block
                    }
                }
            }

            try
            {
                // setup the op context
                doAddOperation( target, clone );
            }
            catch ( Exception e )
            {
                JndiUtils.wrap( e );
            }

            return;
        }

        // Check for Referenceable
        if ( obj instanceof Referenceable )
        {
            throw new NamingException( I18n.err( I18n.ERR_493 ) );
        }

        // Store different formats
        if ( obj instanceof Reference )
        {
            // Store as ref and add outAttrs
            throw new NamingException( I18n.err( I18n.ERR_494 ) );
        }
        else if ( obj instanceof Serializable )
        {
            // Serialize and add outAttrs
            Entry clone = serverEntry.clone();

            if ( outServerEntry != null && outServerEntry.size() > 0 )
            {
                for ( Attribute attribute : outServerEntry )
                {
                    try
                    {
                        clone.put( attribute );
                    }
                    catch ( LdapException le )
                    {
                        throw new InvalidAttributesException( le.getMessage() );
                    }
                }
            }

            try
            {
                // Serialize object into entry attributes and add it.
                JavaLdapSupport.serialize( serverEntry, obj, getDirectoryService().getSchemaManager() );

                // setup the op context
                doAddOperation( target, clone );
            }
            catch ( Exception e )
            {
                JndiUtils.wrap( e );
            }
        }
        else if ( obj instanceof DirContext )
        {
            // Grab attributes and merge with outAttrs
            Entry entry = null;

            try
            {
                entry = ServerEntryUtils.toServerEntry( ( ( DirContext ) obj ).getAttributes( "" ), target,
                    getDirectoryService().getSchemaManager() );
            }
            catch ( LdapInvalidAttributeTypeException liate )
            {
                throw new InvalidAttributeIdentifierException( liate.getMessage() );
            }

            if ( ( outServerEntry != null ) && ( outServerEntry.size() > 0 ) )
            {
                for ( Attribute attribute : outServerEntry )
                {
                    try
                    {
                        entry.put( attribute );
                    }
                    catch ( LdapException le )
                    {
                        throw new InvalidAttributeValueException( le.getMessage() );
                    }
                }
            }

            try
            {
                // setup the op context
                doAddOperation( target, entry );
            }
            catch ( Exception e )
            {
                JndiUtils.wrap( e );
            }
        }
        else
        {
            throw new NamingException( I18n.err( I18n.ERR_495, obj ) );
        }
    }


    /**
     * @see javax.naming.directory.DirContext#rebind(java.lang.String,
     *      java.lang.Object, javax.naming.directory.Attributes)
     */
    public void rebind( String name, Object obj, Attributes attrs ) throws NamingException
    {
        rebind( new LdapName( name ), obj, AttributeUtils.toCaseInsensitive( attrs ) );
    }


    /**
     * @see javax.naming.directory.DirContext#rebind(javax.naming.Name,
     *      java.lang.Object, javax.naming.directory.Attributes)
     */
    public void rebind( Name name, Object obj, Attributes attrs ) throws NamingException
    {
        Dn target = buildTarget( JndiUtils.fromName( name ) );

        try
        {
            HasEntryOperationContext hasEntryContext = new HasEntryOperationContext( getSession(), target );

            if ( getDirectoryService().getOperationManager().hasEntry( hasEntryContext ) )
            {
                doDeleteOperation( target );
            }
        }
        catch ( Exception e )
        {
            JndiUtils.wrap( e );
        }

        bind( name, obj, AttributeUtils.toCaseInsensitive( attrs ) );
    }


    /**
     * @see javax.naming.directory.DirContext#createSubcontext(java.lang.String,
     *      javax.naming.directory.Attributes)
     */
    public DirContext createSubcontext( String name, Attributes attrs ) throws NamingException
    {
        Attributes attributes = AttributeUtils.toCaseInsensitive( attrs );
        return createSubcontext( new LdapName( name ), attributes );
    }


    /**
     * @see javax.naming.directory.DirContext#createSubcontext(
     * javax.naming.Name, javax.naming.directory.Attributes)
     */
    public DirContext createSubcontext( Name name, Attributes attrs ) throws NamingException
    {
        if ( null == attrs )
        {
            return ( DirContext ) super.createSubcontext( name );
        }

        Dn target = buildTarget( JndiUtils.fromName( name ) );

        attrs = AttributeUtils.toCaseInsensitive( attrs );
        Attributes attributes = ( Attributes ) attrs.clone();

        // Add the new context to the server which as a side effect adds
        try
        {
            Entry serverEntry = ServerEntryUtils.toServerEntry( attributes,
                target, getDirectoryService().getSchemaManager() );
            doAddOperation( target, serverEntry );
        }
        catch ( Exception e )
        {
            JndiUtils.wrap( e );
        }

        // Initialize the new context
        ServerLdapContext ctx = null;

        try
        {
            ctx = new ServerLdapContext( getService(), getSession().getEffectivePrincipal(), JndiUtils.toName( target ) );
        }
        catch ( Exception e )
        {
            JndiUtils.wrap( e );
        }

        return ctx;
    }


    /**
     * Presently unsupported operation!
     */
    public DirContext getSchema( Name name ) throws NamingException
    {
        throw new UnsupportedOperationException();
    }


    /**
     * Presently unsupported operation!
     */
    public DirContext getSchema( String name ) throws NamingException
    {
        throw new UnsupportedOperationException();
    }


    /**
     * Presently unsupported operation!
     */
    public DirContext getSchemaClassDefinition( Name name ) throws NamingException
    {
        throw new UnsupportedOperationException();
    }


    /**
     * Presently unsupported operation!
     */
    public DirContext getSchemaClassDefinition( String name ) throws NamingException
    {
        throw new UnsupportedOperationException();
    }


    // ------------------------------------------------------------------------
    // Search Operation Implementations
    // ------------------------------------------------------------------------

    /**
     * @see javax.naming.directory.DirContext#search(java.lang.String,
     *      javax.naming.directory.Attributes)
     */
    public NamingEnumeration<SearchResult> search( String name, Attributes matchingAttributes ) throws NamingException
    {
        return search( new LdapName( name ), matchingAttributes, null );
    }


    /**
     * @see javax.naming.directory.DirContext#search(javax.naming.Name,
     *      javax.naming.directory.Attributes)
     */
    public NamingEnumeration<SearchResult> search( Name name, Attributes matchingAttributes ) throws NamingException
    {
        return search( name, AttributeUtils.toCaseInsensitive( matchingAttributes ), null );
    }


    /**
     * @see javax.naming.directory.DirContext#search(java.lang.String,
     *      javax.naming.directory.Attributes, java.lang.String[])
     */
    public NamingEnumeration<SearchResult> search( String name, Attributes matchingAttributes,
        String[] attributesToReturn ) throws NamingException
    {
        return search( new LdapName( name ), AttributeUtils.toCaseInsensitive( matchingAttributes ), attributesToReturn );
    }


    /**
     * @see javax.naming.directory.DirContext#search(javax.naming.Name,
     *      javax.naming.directory.Attributes, java.lang.String[])
     */
    public NamingEnumeration<SearchResult> search( Name name, Attributes matchingAttributes, String[] attributesToReturn )
        throws NamingException
    {
        SearchControls ctls = new SearchControls();
        Dn target = buildTarget( JndiUtils.fromName( name ) );

        // If we need to return specific attributes add em to the SearchControls
        if ( null != attributesToReturn )
        {
            ctls.setReturningAttributes( attributesToReturn );
        }

        // If matchingAttributes is null/empty use a match for everything filter
        matchingAttributes = AttributeUtils.toCaseInsensitive( matchingAttributes );

        if ( ( null == matchingAttributes ) || ( matchingAttributes.size() <= 0 ) )
        {
            PresenceNode filter = new PresenceNode( OBJECT_CLASS_AT );
            AliasDerefMode aliasDerefMode = AliasDerefMode.getEnum( getEnvironment() );
            try
            {
                EntryFilteringCursor cursor = doSearchOperation( target, aliasDerefMode, filter, ctls );
                return new NamingEnumerationAdapter( cursor );
            }
            catch ( Exception e )
            {
                JndiUtils.wrap( e );
            }
        }

        // Handle simple filter expressions without multiple terms
        if ( matchingAttributes.size() == 1 )
        {
            NamingEnumeration<? extends javax.naming.directory.Attribute> list = matchingAttributes.getAll();
            javax.naming.directory.Attribute attr = list.next();
            list.close();

            if ( attr.size() == 1 )
            {
                Object value = attr.get();
                SimpleNode<?> node;
                String attributeType = attr.getID();

                if ( value instanceof byte[] )
                {
                    node = new EqualityNode<byte[]>( attributeType, new BinaryValue( ( byte[] ) value ) );
                }
                else
                {
                    node = new EqualityNode<String>( attributeType,
                        new org.apache.directory.shared.ldap.model.entry.StringValue( ( String ) value ) );
                }

                AliasDerefMode aliasDerefMode = AliasDerefMode.getEnum( getEnvironment() );

                try
                {
                    EntryFilteringCursor cursor = doSearchOperation( target, aliasDerefMode, node, ctls );
                    return new NamingEnumerationAdapter( cursor );
                }
                catch ( Exception e )
                {
                    JndiUtils.wrap( e );
                    return null; // shut compiler up
                }
            }
        }

        /*
         * Go through the set of attributes using each attribute value pair as
         * an attribute value assertion within one big AND filter expression.
         */
        javax.naming.directory.Attribute attr;
        SimpleNode node;
        BranchNode filter = new AndNode();
        NamingEnumeration<? extends javax.naming.directory.Attribute> list = matchingAttributes.getAll();

        // Loop through each attribute value pair
        while ( list.hasMore() )
        {
            attr = list.next();

            /*
             * According to JNDI if an attribute in the matchingAttributes
             * list does not have any values then we match for just the presence
             * of the attribute in the entry
             */
            if ( attr.size() == 0 )
            {
                filter.addNode( new PresenceNode( attr.getID() ) );
                continue;
            }

            /*
             * With 1 or more value we build a set of simple nodes and add them
             * to the AND node - each attribute value pair is a simple Ava node.
             */
            for ( int ii = 0; ii < attr.size(); ii++ )
            {
                Object val = attr.get( ii );

                // Add simpel Ava node if its value is a String
                if ( val instanceof String )
                {
                    node = new EqualityNode<String>( attr.getID(),
                        new org.apache.directory.shared.ldap.model.entry.StringValue( ( String ) val ) );
                    filter.addNode( node );
                }
            }
        }

        AliasDerefMode aliasDerefMode = AliasDerefMode.getEnum( getEnvironment() );
        try
        {
            EntryFilteringCursor cursor = doSearchOperation( target, aliasDerefMode, filter, ctls );
            return new NamingEnumerationAdapter( cursor );
        }
        catch ( Exception e )
        {
            JndiUtils.wrap( e );
            return null; // shut compiler up
        }
    }


    /**
     * @see javax.naming.directory.DirContext#search(java.lang.String,
     *      java.lang.String, javax.naming.directory.SearchControls)
     */
    public NamingEnumeration<SearchResult> search( String name, String filter, SearchControls cons )
        throws NamingException
    {
        return search( new LdapName( name ), filter, cons );
    }


    /**
     * A search overload that is used for optimizing search handling in the
     * LDAP protocol provider which deals with an ExprNode instance rather than
     * a String for the filter.
     *
     * @param name the relative name of the object serving as the search base
     * @param filter the search filter as an expression tree
     * @param cons the search controls to use
     * @return an enumeration over the SearchResults
     * @throws NamingException if there are problems performing the search
     */
    public NamingEnumeration<SearchResult> search( Name name, ExprNode filter, SearchControls cons )
        throws NamingException
    {
        Dn target = buildTarget( JndiUtils.fromName( name ) );
        AliasDerefMode aliasDerefMode = AliasDerefMode.getEnum( getEnvironment() );
        try
        {
            return new NamingEnumerationAdapter( doSearchOperation( target, aliasDerefMode, filter, cons ) );
        }
        catch ( Exception e )
        {
            JndiUtils.wrap( e );
            return null; // shut compiler up
        }
    }


    /**
     * @see javax.naming.directory.DirContext#search(javax.naming.Name,
     *      java.lang.String, javax.naming.directory.SearchControls)
     */
    public NamingEnumeration<SearchResult> search( Name name, String filter, SearchControls cons )
        throws NamingException
    {
        ExprNode filterNode;
        Dn target = buildTarget( JndiUtils.fromName( name ) );

        try
        {
            filterNode = FilterParser.parse( schemaManager, filter );
        }
        catch ( ParseException pe )
        {
            InvalidSearchFilterException isfe = new InvalidSearchFilterException( I18n.err( I18n.ERR_500, filter ) );
            isfe.setRootCause( pe );
            throw isfe;
        }

        AliasDerefMode aliasDerefMode = AliasDerefMode.getEnum( getEnvironment() );

        try
        {
            EntryFilteringCursor cursor = doSearchOperation( target, aliasDerefMode, filterNode, cons );
            return new NamingEnumerationAdapter( cursor );
        }
        catch ( Exception e )
        {
            JndiUtils.wrap( e );
            return null; // shut compiler up
        }
    }


    /**
     * @see javax.naming.directory.DirContext#search(java.lang.String,
     *      java.lang.String, java.lang.Object[],
     *      javax.naming.directory.SearchControls)
     */
    public NamingEnumeration<SearchResult> search( String name, String filterExpr, Object[] filterArgs,
        SearchControls cons ) throws NamingException
    {
        return search( new LdapName( name ), filterExpr, filterArgs, cons );
    }


    /**
     * @see javax.naming.directory.DirContext#search(javax.naming.Name,
     *      java.lang.String, java.lang.Object[],
     *      javax.naming.directory.SearchControls)
     */
    public NamingEnumeration<SearchResult> search( Name name, String filterExpr, Object[] filterArgs,
        SearchControls cons ) throws NamingException
    {
        int start;
        int index;

        StringBuffer buf = new StringBuffer( filterExpr );

        // Scan until we hit the end of the string buffer
        for ( int ii = 0; ii < buf.length(); ii++ )
        {
            try
            {
                // Advance until we hit the start of a variable
                while ( ii < buf.length() && '{' != buf.charAt( ii ) )
                {
                    ii++;
                }

                // Record start of variable at '{'
                start = ii;

                // Advance to the end of a variable at '}'
                while ( '}' != buf.charAt( ii ) )
                {
                    ii++;
                }
            }
            catch ( IndexOutOfBoundsException e )
            {
                // End of filter so done.
                break;
            }

            // Parse index
            index = Integer.parseInt( buf.substring( start + 1, ii ) );

            if ( filterArgs[index] instanceof String )
            {
                /*
                 * Replace the '{ i }' with the string representation of the value
                 * held in the filterArgs array at index index.
                 */
                buf.replace( start, ii + 1, ( String ) filterArgs[index] );
            }
            else if ( filterArgs[index] instanceof byte[] )
            {
                String hexstr = "#" + Strings.toHexString( ( byte[] ) filterArgs[index] );
                buf.replace( start, ii + 1, hexstr );
            }
            else
            {
                /*
                 * Replace the '{ i }' with the string representation of the value
                 * held in the filterArgs array at index index.
                 */
                buf.replace( start, ii + 1, filterArgs[index].toString() );
            }
        }

        return search( name, buf.toString(), cons );
    }


    // ------------------------------------------------------------------------
    // EventDirContext implementations
    // ------------------------------------------------------------------------

    public void addNamingListener( Name name, String filterStr, SearchControls searchControls,
        NamingListener namingListener ) throws NamingException
    {
        ExprNode filter;

        try
        {
            filter = FilterParser.parse( schemaManager, filterStr );
        }
        catch ( Exception e )
        {
            NamingException e2 = new NamingException( I18n.err( I18n.ERR_501, filterStr ) );
            e2.setRootCause( e );
            throw e2;
        }

        try
        {
            DirectoryListener listener = new EventListenerAdapter( ( ServerLdapContext ) this, namingListener );
            NotificationCriteria criteria = new NotificationCriteria();
            criteria.setFilter( filter );
            criteria.setScope( SearchScope.getSearchScope( searchControls.getSearchScope() ) );
            criteria.setAliasDerefMode( AliasDerefMode.getEnum( getEnvironment() ) );
            criteria.setBase( buildTarget( JndiUtils.fromName( name ) ) );

            getDirectoryService().getEventService().addListener( listener, criteria );
            getListeners().put( namingListener, listener );
        }
        catch ( Exception e )
        {
            JndiUtils.wrap( e );
        }
    }


    public void addNamingListener( String name, String filter, SearchControls searchControls,
        NamingListener namingListener ) throws NamingException
    {
        addNamingListener( new LdapName( name ), filter, searchControls, namingListener );
    }


    public void addNamingListener( Name name, String filterExpr, Object[] filterArgs, SearchControls searchControls,
        NamingListener namingListener ) throws NamingException
    {
        int start;
        StringBuffer buf = new StringBuffer( filterExpr );

        // Scan until we hit the end of the string buffer
        for ( int ii = 0; ii < buf.length(); ii++ )
        {
            // Advance until we hit the start of a variable
            while ( '{' != buf.charAt( ii ) )
            {
                ii++;
            }

            // Record start of variable at '{'
            start = ii;

            // Advance to the end of a variable at '}'
            while ( '}' != buf.charAt( ii ) )
            {
                ii++;
            }

            /*
             * Replace the '{ i }' with the string representation of the value
             * held in the filterArgs array at index index.
             */
            buf.replace( start, ii + 1, filterArgs[ii].toString() );
        }

        addNamingListener( name, buf.toString(), searchControls, namingListener );
    }


    public void addNamingListener( String name, String filter, Object[] objects, SearchControls searchControls,
        NamingListener namingListener ) throws NamingException
    {
        addNamingListener( new LdapName( name ), filter, objects, searchControls, namingListener );
    }
}
