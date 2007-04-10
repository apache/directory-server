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
package org.apache.directory.server.core.operational;


import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.naming.NamingEnumeration;
import javax.naming.NamingException;
import javax.naming.directory.Attribute;
import javax.naming.directory.Attributes;
import javax.naming.directory.DirContext;
import javax.naming.directory.SearchControls;
import javax.naming.directory.SearchResult;

import org.apache.directory.server.constants.ApacheSchemaConstants;
import org.apache.directory.server.core.DirectoryServiceConfiguration;
import org.apache.directory.server.core.configuration.InterceptorConfiguration;
import org.apache.directory.server.core.enumeration.SearchResultFilter;
import org.apache.directory.server.core.enumeration.SearchResultFilteringEnumeration;
import org.apache.directory.server.core.interceptor.BaseInterceptor;
import org.apache.directory.server.core.interceptor.Interceptor;
import org.apache.directory.server.core.interceptor.NextInterceptor;
import org.apache.directory.server.core.interceptor.context.AddServiceContext;
import org.apache.directory.server.core.interceptor.context.LookupServiceContext;
import org.apache.directory.server.core.interceptor.context.ModifyServiceContext;
import org.apache.directory.server.core.interceptor.context.ServiceContext;
import org.apache.directory.server.core.invocation.Invocation;
import org.apache.directory.server.core.invocation.InvocationStack;
import org.apache.directory.server.core.partition.PartitionNexus;
import org.apache.directory.server.schema.registries.AttributeTypeRegistry;
import org.apache.directory.shared.ldap.constants.SchemaConstants;
import org.apache.directory.shared.ldap.filter.ExprNode;
import org.apache.directory.shared.ldap.schema.AttributeType;
import org.apache.directory.shared.ldap.schema.UsageEnum;
import org.apache.directory.shared.ldap.util.AttributeUtils;
import org.apache.directory.shared.ldap.util.DateUtils;
import org.apache.directory.shared.ldap.message.AttributeImpl;
import org.apache.directory.shared.ldap.message.AttributesImpl;
import org.apache.directory.shared.ldap.message.ModificationItemImpl;
import org.apache.directory.shared.ldap.name.AttributeTypeAndValue;
import org.apache.directory.shared.ldap.name.LdapDN;
import org.apache.directory.shared.ldap.name.Rdn;


/**
 * An {@link Interceptor} that adds or modifies the default attributes
 * of entries. There are four default attributes for now;
 * <tt>'creatorsName'</tt>, <tt>'createTimestamp'</tt>, <tt>'modifiersName'</tt>,
 * and <tt>'modifyTimestamp'</tt>.
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 * @version $Rev$, $Date$
 */
public class OperationalAttributeService extends BaseInterceptor
{
    private final SearchResultFilter DENORMALIZING_SEARCH_FILTER = new SearchResultFilter()
    {
        public boolean accept( Invocation invocation, SearchResult result, SearchControls controls ) 
            throws NamingException
        {
            if ( controls.getReturningAttributes() != null )
            {
                return filterDenormalized( result.getAttributes() );
            }
            
            return true;
        }
    };

    /**
     * the database search result filter to register with filter service
     */
    private final SearchResultFilter SEARCH_FILTER = new SearchResultFilter()
    {
        public boolean accept( Invocation invocation, SearchResult result, SearchControls controls )
            throws NamingException
        {
            if ( controls.getReturningAttributes() == null )
            {
                return filter( result.getAttributes() );
            }

            return true;
        }
    };

    /**
     * the root nexus of the system
     */
    private PartitionNexus nexus;

    private AttributeTypeRegistry registry;

    private boolean isDenormalizeOpAttrsEnabled;

    /**
     * subschemaSubentry attribute's value from Root DSE
     */
    private LdapDN subschemaSubentryDn;

    /**
     * Creates the operational attribute management service interceptor.
     */
    public OperationalAttributeService()
    {
    }


    public void init( DirectoryServiceConfiguration factoryCfg, InterceptorConfiguration cfg ) throws NamingException
    {
        nexus = factoryCfg.getPartitionNexus();
        registry = factoryCfg.getRegistries().getAttributeTypeRegistry();
        isDenormalizeOpAttrsEnabled = factoryCfg.getStartupConfiguration().isDenormalizeOpAttrsEnabled();

        // stuff for dealing with subentries (garbage for now)
        String subschemaSubentry = ( String ) nexus.getRootDSE().get( "subschemaSubentry" ).get();
        subschemaSubentryDn = new LdapDN( subschemaSubentry );
        subschemaSubentryDn.normalize( factoryCfg.getRegistries().getAttributeTypeRegistry().getNormalizerMapping() );
    }


    public void destroy()
    {
    }


    /**
     * Adds extra operational attributes to the entry before it is added.
     */
    public void add(NextInterceptor nextInterceptor, ServiceContext addContext )
        throws NamingException
    {
        String principal = getPrincipal().getName();
        Attributes entry = ((AddServiceContext)addContext).getEntry();

        Attribute attribute = new AttributeImpl( SchemaConstants.CREATORS_NAME_AT );
        attribute.add( principal );
        entry.put( attribute );

        attribute = new AttributeImpl( SchemaConstants.CREATE_TIMESTAMP_AT );
        attribute.add( DateUtils.getGeneralizedTime() );
        entry.put( attribute );

        nextInterceptor.add( addContext );
    }


    public void modify( NextInterceptor nextInterceptor, ServiceContext modifyContext )
        throws NamingException
    {
        nextInterceptor.modify( modifyContext );

        if ( modifyContext.getDn().getNormName().equals( subschemaSubentryDn.getNormName() ) ) 
        {
            return;
        }
        
        // add operational attributes after call in case the operation fails
        Attributes attributes = new AttributesImpl( true );
        Attribute attribute = new AttributeImpl( SchemaConstants.MODIFIERS_NAME_AT );
        attribute.add( getPrincipal().getName() );
        attributes.put( attribute );

        attribute = new AttributeImpl( SchemaConstants.MODIFY_TIMESTAMP_AT );
        attribute.add( DateUtils.getGeneralizedTime() );
        attributes.put( attribute );

        ModifyServiceContext newModify = new ModifyServiceContext( 
        		modifyContext.getDn(),
        		DirContext.REPLACE_ATTRIBUTE, 
        		attributes);
        nexus.modify( newModify );
    }


    public void modify( NextInterceptor nextInterceptor, LdapDN name, ModificationItemImpl[] items ) throws NamingException
    {
        nextInterceptor.modify( name, items );

        if ( name.getNormName().equals( subschemaSubentryDn.getNormName() ) ) 
        {
            return;
        }
        
        // add operational attributes after call in case the operation fails
        Attributes attributes = new AttributesImpl( true );
        Attribute attribute = new AttributeImpl( SchemaConstants.MODIFIERS_NAME_AT );
        attribute.add( getPrincipal().getName() );
        attributes.put( attribute );

        attribute = new AttributeImpl( SchemaConstants.MODIFY_TIMESTAMP_AT );
        attribute.add( DateUtils.getGeneralizedTime() );
        attributes.put( attribute );

        ModifyServiceContext newModify = new ModifyServiceContext( 
        		name,
        		DirContext.REPLACE_ATTRIBUTE, 
        		attributes);
        nexus.modify( newModify );
    }


    public void modifyRn( NextInterceptor nextInterceptor, LdapDN name, String newRn, boolean deleteOldRn )
        throws NamingException
    {
        nextInterceptor.modifyRn( name, newRn, deleteOldRn );

        // add operational attributes after call in case the operation fails
        Attributes attributes = new AttributesImpl( true );
        Attribute attribute = new AttributeImpl( SchemaConstants.MODIFIERS_NAME_AT );
        attribute.add( getPrincipal().getName() );
        attributes.put( attribute );

        attribute = new AttributeImpl( SchemaConstants.MODIFY_TIMESTAMP_AT );
        attribute.add( DateUtils.getGeneralizedTime() );
        attributes.put( attribute );

        LdapDN newDn = ( LdapDN ) name.clone();
        newDn.remove( name.size() - 1 );
        newDn.add( newRn );
        newDn.normalize( registry.getNormalizerMapping() );
        
        ModifyServiceContext newModify = new ModifyServiceContext( 
        		newDn,
        		DirContext.REPLACE_ATTRIBUTE, 
        		attributes);
        nexus.modify( newModify );
    }


    public void move( NextInterceptor nextInterceptor, LdapDN name, LdapDN newParentName ) throws NamingException
    {
        nextInterceptor.move( name, newParentName );

        // add operational attributes after call in case the operation fails
        Attributes attributes = new AttributesImpl( true );
        Attribute attribute = new AttributeImpl( SchemaConstants.MODIFIERS_NAME_AT );
        attribute.add( getPrincipal().getName() );
        attributes.put( attribute );

        attribute = new AttributeImpl( SchemaConstants.MODIFY_TIMESTAMP_AT );
        attribute.add( DateUtils.getGeneralizedTime() );
        attributes.put( attribute );

        ModifyServiceContext newModify = new ModifyServiceContext( 
        		newParentName,
        		DirContext.REPLACE_ATTRIBUTE, 
        		attributes);
        nexus.modify( newModify );
    }


    public void move( NextInterceptor nextInterceptor, LdapDN name, LdapDN newParentName, String newRn, boolean deleteOldRn )
        throws NamingException
    {
        nextInterceptor.move( name, newParentName, newRn, deleteOldRn );

        // add operational attributes after call in case the operation fails
        Attributes attributes = new AttributesImpl( true );
        Attribute attribute = new AttributeImpl( SchemaConstants.MODIFIERS_NAME_AT );
        attribute.add( getPrincipal().getName() );
        attributes.put( attribute );

        attribute = new AttributeImpl( SchemaConstants.MODIFY_TIMESTAMP_AT );
        attribute.add( DateUtils.getGeneralizedTime() );
        attributes.put( attribute );

        ModifyServiceContext newModify = new ModifyServiceContext( 
        		newParentName,
        		DirContext.REPLACE_ATTRIBUTE, 
        		attributes);
        nexus.modify( newModify );
    }


    public Attributes lookup( NextInterceptor nextInterceptor, ServiceContext lookupContext ) throws NamingException
    {
        Attributes result = nextInterceptor.lookup( lookupContext );
        
        if ( result == null )
        {
            return null;
        }

        if ( ((LookupServiceContext)lookupContext).getAttrsId() == null )
        {
            filter( result );
        }
        else
        {
            filter( lookupContext, result );
        }
        
        return result;
    }


    public NamingEnumeration list( NextInterceptor nextInterceptor, LdapDN base ) throws NamingException
    {
        NamingEnumeration e = nextInterceptor.list( base );
        Invocation invocation = InvocationStack.getInstance().peek();
        return new SearchResultFilteringEnumeration( e, new SearchControls(), invocation, SEARCH_FILTER );
    }


    public NamingEnumeration search( NextInterceptor nextInterceptor, LdapDN base, Map env, ExprNode filter,
                                     SearchControls searchCtls ) throws NamingException
    {
        Invocation invocation = InvocationStack.getInstance().peek();
        NamingEnumeration e = nextInterceptor.search( base, env, filter, searchCtls );
        if ( searchCtls.getReturningAttributes() != null )
        {
            if ( isDenormalizeOpAttrsEnabled )
            {
                return new SearchResultFilteringEnumeration( e, searchCtls, invocation, DENORMALIZING_SEARCH_FILTER );
            }
                
            return e;
        }

        return new SearchResultFilteringEnumeration( e, searchCtls, invocation, SEARCH_FILTER );
    }


    /**
     * Filters out the operational attributes within a search results attributes.  The attributes are directly
     * modified.
     *
     * @param attributes the resultant attributes to filter
     * @return true always
     */
    private boolean filter( Attributes attributes ) throws NamingException
    {
        NamingEnumeration list = attributes.getIDs();

        while ( list.hasMore() )
        {
            String attrId = ( String ) list.next();

            AttributeType type = null;

            if ( registry.hasAttributeType( attrId ) )
            {
                type = registry.lookup( attrId );
            }

            if ( type != null && type.getUsage() != UsageEnum.USER_APPLICATIONS )
            {
                attributes.remove( attrId );
            }
        }
        return true;
    }


    private void filter( ServiceContext lookupContext, Attributes entry ) throws NamingException
    {
        LdapDN dn = ((LookupServiceContext)lookupContext).getDn();
        List<String> ids = ((LookupServiceContext)lookupContext).getAttrsId();
        
        // still need to protect against returning op attrs when ids is null
        if ( ids == null )
        {
            filter( entry );
            return;
        }

        if ( dn.size() == 0 )
        {
            Set<String> idsSet = new HashSet<String>( ids.size() );

            for ( String id:ids  )
            {
                idsSet.add( id.toLowerCase() );
            }

            NamingEnumeration list = entry.getIDs();

            while ( list.hasMore() )
            {
                String attrId = ( ( String ) list.nextElement() ).toLowerCase();

                if ( !idsSet.contains( attrId ) )
                {
                    entry.remove( attrId );
                }
            }
        }

        denormalizeEntryOpAttrs( entry );
        
        // do nothing past here since this explicity specifies which
        // attributes to include - backends will automatically populate
        // with right set of attributes using ids array
    }

    
    public void denormalizeEntryOpAttrs( Attributes entry ) throws NamingException
    {
        if ( isDenormalizeOpAttrsEnabled )
        {
            AttributeType type = registry.lookup( SchemaConstants.CREATORS_NAME_AT );
            Attribute attr = AttributeUtils.getAttribute( entry, type );

            if ( attr != null )
            {
                LdapDN creatorsName = new LdapDN( ( String ) attr.get() );
                
                attr.clear();
                attr.add( denormalizeTypes( creatorsName ).getUpName() );
            }
            
            type = null;
            type = registry.lookup( SchemaConstants.MODIFIERS_NAME_AT );
            attr = null;
            attr = AttributeUtils.getAttribute( entry, type );
            
            if ( attr != null )
            {
                LdapDN modifiersName = new LdapDN( ( String ) attr.get() );

                attr.clear();
                attr.add( denormalizeTypes( modifiersName ).getUpName() );
            }

            type = null;
            type = registry.lookup( ApacheSchemaConstants.SCHEMA_MODIFIERS_NAME_AT );
            attr = null;
            attr = AttributeUtils.getAttribute( entry, type );
            
            if ( attr != null )
            {
                LdapDN modifiersName = new LdapDN( ( String ) attr.get() );

                attr.clear();
                attr.add( denormalizeTypes( modifiersName ).getUpName() );
            }
        }
    }

    
    /**
     * Does not create a new DN but alters existing DN by using the first
     * short name for an attributeType definition.
     */
    public LdapDN denormalizeTypes( LdapDN dn ) throws NamingException
    {
        LdapDN newDn = new LdapDN();
        
        for ( int ii = 0; ii < dn.size(); ii++ )
        {
            Rdn rdn = dn.getRdn( ii );
            if ( rdn.size() == 0 )
            {
                newDn.add( new Rdn() );
                continue;
            }
            else if ( rdn.size() == 1 )
            {
                newDn.add( new Rdn( registry.lookup( rdn.getNormType() ).getName(), (String)rdn.getAtav().getValue() ) );
                continue;
            }

            // below we only process multi-valued rdns
            StringBuffer buf = new StringBuffer();
            for ( Iterator jj = rdn.iterator(); jj.hasNext(); /**/ )
            {
                AttributeTypeAndValue atav = ( AttributeTypeAndValue ) jj.next();
                String type = registry.lookup( rdn.getNormType() ).getName();
                buf.append( type ).append( '=' ).append( atav.getValue() );
                if ( jj.hasNext() )
                {
                    buf.append( '+' );
                }
            }
            newDn.add( new Rdn(buf.toString()) );
        }
        
        return newDn;
    }


    private boolean filterDenormalized( Attributes entry ) throws NamingException
    {
        denormalizeEntryOpAttrs( entry );
        return true;
    }
}