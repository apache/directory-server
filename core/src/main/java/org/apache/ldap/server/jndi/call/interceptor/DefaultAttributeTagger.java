/*
 *   Copyright 2004 The Apache Software Foundation
 *
 *   Licensed under the Apache License, Version 2.0 (the "License");
 *   you may not use this file except in compliance with the License.
 *   You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *   Unless required by applicable law or agreed to in writing, software
 *   distributed under the License is distributed on an "AS IS" BASIS,
 *   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *   See the License for the specific language governing permissions and
 *   limitations under the License.
 *
 */
package org.apache.ldap.server.jndi.call.interceptor;


import java.util.HashSet;
import java.util.Properties;

import javax.naming.Name;
import javax.naming.NamingEnumeration;
import javax.naming.NamingException;
import javax.naming.directory.Attributes;
import javax.naming.directory.BasicAttribute;
import javax.naming.directory.BasicAttributes;
import javax.naming.directory.DirContext;
import javax.naming.directory.SearchControls;
import javax.naming.directory.SearchResult;
import javax.naming.ldap.LdapContext;

import org.apache.ldap.common.schema.AttributeType;
import org.apache.ldap.common.schema.UsageEnum;
import org.apache.ldap.common.util.DateUtils;
import org.apache.ldap.server.RootNexus;
import org.apache.ldap.server.db.ResultFilteringEnumeration;
import org.apache.ldap.server.db.SearchResultFilter;
import org.apache.ldap.server.jndi.call.Add;
import org.apache.ldap.server.jndi.call.Lookup;
import org.apache.ldap.server.jndi.call.LookupWithAttrIds;
import org.apache.ldap.server.jndi.call.Modify;
import org.apache.ldap.server.jndi.call.ModifyMany;
import org.apache.ldap.server.jndi.call.ModifyRN;
import org.apache.ldap.server.jndi.call.Move;
import org.apache.ldap.server.jndi.call.MoveAndModifyRN;
import org.apache.ldap.server.jndi.call.Search;
import org.apache.ldap.server.schema.AttributeTypeRegistry;
import org.apache.ldap.server.schema.GlobalRegistries;


/**
 * An interceptor based service which manages the creation and modification of
 * operational attributes as operations are performed.
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 * @version $Rev$
 */
public class DefaultAttributeTagger extends BaseInterceptor
{
    /** the database search result filter to register with filter service */
    private final SearchResultFilter SEARCH_FILTER = new SearchResultFilter()
    {
        public boolean accept( LdapContext ctx, SearchResult result, SearchControls controls )
            throws NamingException
        {
            if ( controls.getReturningAttributes() == null )
            {
                return filter( result.getAttributes() );
            }

            return true;
        }
    };

    /** the root nexus of the system */
    private final RootNexus nexus;
    private final AttributeTypeRegistry registry;


    /**
     * Creates the operational attribute management service interceptor.
     *
     * @param nexus the root nexus of the system
     * @param globalRegistries the global schema object registries
     */
    public DefaultAttributeTagger( RootNexus nexus,
                                   GlobalRegistries globalRegistries )
    {
        this.nexus = nexus;
        if ( this.nexus == null )
        {
            throw new NullPointerException( "the nexus cannot be null" );
        }

        if ( globalRegistries == null )
        {
            throw new NullPointerException( "the global registries cannot be null" );
        }
        this.registry = globalRegistries.getAttributeTypeRegistry();
    }

    public void destroy() {
    }

    public void init(Properties config) throws NamingException {
    }

    /**
     * Adds extra operational attributes to the entry before it is added.
     *
     * @see BaseInterceptor#add(String, Name, Attributes)
     */
    protected void process( NextInterceptor nextInterceptor, Add request ) throws NamingException
    {
        String principal = getPrincipal( request ).getName();
        Attributes entry = request.getAttributes();

        BasicAttribute attribute = new BasicAttribute( "creatorsName" );
        attribute.add( principal );
        entry.put( attribute );

        attribute = new BasicAttribute( "createTimestamp" );
        attribute.add( DateUtils.getGeneralizedTime() );
        entry.put( attribute );
        
        nextInterceptor.process( request );
    }


    protected void process( NextInterceptor nextInterceptor, Modify request ) throws NamingException
    {
        nextInterceptor.process( request );
        
        // add operational attributes after call in case the operation fails
        Attributes attributes = new BasicAttributes();
        BasicAttribute attribute = new BasicAttribute( "modifiersName" );
        attribute.add( getPrincipal( request ).getName() );
        attributes.put( attribute );

        attribute = new BasicAttribute( "modifyTimestamp" );
        attribute.add( DateUtils.getGeneralizedTime() );
        attributes.put( attribute );

        nexus.modify( request.getName(), DirContext.REPLACE_ATTRIBUTE, attributes );
    }


    protected void process( NextInterceptor nextInterceptor, ModifyMany request ) throws NamingException
    {
        nextInterceptor.process( request );

        // add operational attributes after call in case the operation fails
        Attributes attributes = new BasicAttributes();
        BasicAttribute attribute = new BasicAttribute( "modifiersName" );
        attribute.add( getPrincipal( request ).getName() );
        attributes.put( attribute );

        attribute = new BasicAttribute( "modifyTimestamp" );
        attribute.add( DateUtils.getGeneralizedTime() );
        attributes.put( attribute );

        nexus.modify( request.getName(), DirContext.REPLACE_ATTRIBUTE, attributes );
    }


    protected void process( NextInterceptor nextInterceptor, ModifyRN request ) throws NamingException
    {
        nextInterceptor.process( request );
        
        // add operational attributes after call in case the operation fails
        Attributes attributes = new BasicAttributes();
        BasicAttribute attribute = new BasicAttribute( "modifiersName" );
        attribute.add( getPrincipal( request ).getName() );
        attributes.put( attribute );

        attribute = new BasicAttribute( "modifyTimestamp" );
        attribute.add( DateUtils.getGeneralizedTime() );
        attributes.put( attribute );

        Name newDn = request.getName().getSuffix( 1 ).add( request.getNewRelativeName() );
        nexus.modify( newDn, DirContext.REPLACE_ATTRIBUTE, attributes );
    }


    protected void process( NextInterceptor nextInterceptor, Move request ) throws NamingException
    {
        nextInterceptor.process( request );

        // add operational attributes after call in case the operation fails
        Attributes attributes = new BasicAttributes();
        BasicAttribute attribute = new BasicAttribute( "modifiersName" );
        attribute.add( getPrincipal( request ).getName() );
        attributes.put( attribute );

        attribute = new BasicAttribute( "modifyTimestamp" );
        attribute.add( DateUtils.getGeneralizedTime() );
        attributes.put( attribute );

        nexus.modify( request.getNewParentName(), DirContext.REPLACE_ATTRIBUTE, attributes );
    }


    protected void process( NextInterceptor nextInterceptor, MoveAndModifyRN request ) throws NamingException
    {
        nextInterceptor.process( request );

        // add operational attributes after call in case the operation fails
        Attributes attributes = new BasicAttributes();
        BasicAttribute attribute = new BasicAttribute( "modifiersName" );
        attribute.add( getPrincipal( request ).getName() );
        attributes.put( attribute );

        attribute = new BasicAttribute( "modifyTimestamp" );
        attribute.add( DateUtils.getGeneralizedTime() );
        attributes.put( attribute );

        nexus.modify( request.getNewParentName(), DirContext.REPLACE_ATTRIBUTE, attributes );
    }


    protected void process(NextInterceptor nextInterceptor, Lookup request) throws NamingException {
        nextInterceptor.process( request );
        
        Attributes attributes = ( Attributes ) request.getResponse();
        Attributes retval = ( Attributes ) attributes.clone();
        filter( retval );
        request.setResponse( retval );
    }

    protected void process(NextInterceptor nextInterceptor, LookupWithAttrIds request) throws NamingException {
        nextInterceptor.process( request );
        
        Attributes attributes = ( Attributes ) request.getResponse();
        if ( attributes == null )
        {
            return;
        }

        Attributes retval = ( Attributes ) attributes.clone();
        filter( request.getName(), retval, request.getAttributeIds() );
        request.setResponse( retval );
    }

    protected void process(NextInterceptor nextInterceptor, Search request) throws NamingException {
        nextInterceptor.process( request );
        
        SearchControls searchControls = request.getSearchControls();
        if ( searchControls.getReturningAttributes() != null )
        {
            return;
        }

        NamingEnumeration e ;
        ResultFilteringEnumeration retval;
        LdapContext ctx = ( LdapContext ) request.getContextStack().peek();
        e = ( NamingEnumeration ) request.getResponse();
        retval = new ResultFilteringEnumeration( e, searchControls, ctx, SEARCH_FILTER );
        request.setResponse( retval );
    }

    /**
     * Filters out the operational attributes within a search results
     * attributes.  The attributes are directly modified.
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

            if ( type != null && type.getUsage() != UsageEnum.USERAPPLICATIONS )
            {
                attributes.remove( attrId );
            }
        }
        return true;
    }

    private void filter( Name dn, Attributes entry, String[] ids )
        throws NamingException
    {
        // still need to protect against returning op attrs when ids is null
        if ( ids == null )
        {
            DefaultAttributeTagger.this.filter( entry );
            return;
        }
        
        if ( dn.size() == 0 )
        {
            HashSet idsSet = new HashSet( ids.length );
            
            for ( int ii = 0; ii < ids.length; ii++ )
            {
                idsSet.add( ids[ii].toLowerCase() );
            }

            NamingEnumeration list = entry.getIDs();
            
            while ( list.hasMore() )
            {
                String attrId = ( ( String ) list.nextElement() ).toLowerCase();
                
                if ( ! idsSet.contains( attrId ) )
                {
                    entry.remove( attrId );
                }
            }
        }
        
        // do nothing past here since this explicity specifies which
        // attributes to include - backends will automatically populate
        // with right set of attributes using ids array
    }
}