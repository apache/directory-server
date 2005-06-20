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
package org.apache.ldap.server.operational;


import org.apache.ldap.common.schema.AttributeType;
import org.apache.ldap.common.schema.UsageEnum;
import org.apache.ldap.common.util.DateUtils;
import org.apache.ldap.server.interceptor.BaseInterceptor;
import org.apache.ldap.server.interceptor.InterceptorContext;
import org.apache.ldap.server.interceptor.NextInterceptor;
import org.apache.ldap.server.db.ResultFilteringEnumeration;
import org.apache.ldap.server.db.SearchResultFilter;
import org.apache.ldap.server.invocation.*;
import org.apache.ldap.server.partition.RootNexus;
import org.apache.ldap.server.schema.AttributeTypeRegistry;

import javax.naming.Name;
import javax.naming.NamingEnumeration;
import javax.naming.NamingException;
import javax.naming.directory.*;
import javax.naming.ldap.LdapContext;
import java.util.HashSet;


/**
 * An {@link org.apache.ldap.server.interceptor.Interceptor} that adds or modifies the default attributes
 * of entries. There are four default attributes for now;<code>'creatorsName'
 * </code>, <code>'createTimestamp'</code>, <code>'modifiersName'</code>, and
 * <code>'modifyTimestamp'</code>.
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 * @version $Rev$, $Date$
 */
public class OperationalAttributeService extends BaseInterceptor
{
    /**
     * the database search result filter to register with filter service
     */
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

    /**
     * the root nexus of the system
     */
    private RootNexus nexus;

    private AttributeTypeRegistry registry;


    /**
     * Creates the operational attribute management service interceptor.
     */
    public OperationalAttributeService()
    {
    }


    public void init( InterceptorContext ctx ) throws NamingException
    {
        nexus = ctx.getRootNexus();
        registry = ctx.getGlobalRegistries().getAttributeTypeRegistry();
    }


    public void destroy()
    {
    }


    /**
     * Adds extra operational attributes to the entry before it is added.
     */
    protected void process( NextInterceptor nextInterceptor, Add call ) throws NamingException
    {
        String principal = getPrincipal( call ).getName();
        Attributes entry = call.getAttributes();

        BasicAttribute attribute = new BasicAttribute( "creatorsName" );
        attribute.add( principal );
        entry.put( attribute );

        attribute = new BasicAttribute( "createTimestamp" );
        attribute.add( DateUtils.getGeneralizedTime() );
        entry.put( attribute );

        nextInterceptor.process( call );
    }


    protected void process( NextInterceptor nextInterceptor, Modify call ) throws NamingException
    {
        nextInterceptor.process( call );
        
        // add operational attributes after call in case the operation fails
        Attributes attributes = new BasicAttributes();
        BasicAttribute attribute = new BasicAttribute( "modifiersName" );
        attribute.add( getPrincipal( call ).getName() );
        attributes.put( attribute );

        attribute = new BasicAttribute( "modifyTimestamp" );
        attribute.add( DateUtils.getGeneralizedTime() );
        attributes.put( attribute );

        nexus.modify( call.getName(), DirContext.REPLACE_ATTRIBUTE, attributes );
    }


    protected void process( NextInterceptor nextInterceptor, ModifyMany call ) throws NamingException
    {
        nextInterceptor.process( call );

        // add operational attributes after call in case the operation fails
        Attributes attributes = new BasicAttributes();
        BasicAttribute attribute = new BasicAttribute( "modifiersName" );
        attribute.add( getPrincipal( call ).getName() );
        attributes.put( attribute );

        attribute = new BasicAttribute( "modifyTimestamp" );
        attribute.add( DateUtils.getGeneralizedTime() );
        attributes.put( attribute );

        nexus.modify( call.getName(), DirContext.REPLACE_ATTRIBUTE, attributes );
    }


    protected void process( NextInterceptor nextInterceptor, ModifyRN call ) throws NamingException
    {
        nextInterceptor.process( call );
        
        // add operational attributes after call in case the operation fails
        Attributes attributes = new BasicAttributes();
        BasicAttribute attribute = new BasicAttribute( "modifiersName" );
        attribute.add( getPrincipal( call ).getName() );
        attributes.put( attribute );

        attribute = new BasicAttribute( "modifyTimestamp" );
        attribute.add( DateUtils.getGeneralizedTime() );
        attributes.put( attribute );

        Name newDn = call.getName().getSuffix( 1 ).add( call.getNewRelativeName() );
        nexus.modify( newDn, DirContext.REPLACE_ATTRIBUTE, attributes );
    }


    protected void process( NextInterceptor nextInterceptor, Move call ) throws NamingException
    {
        nextInterceptor.process( call );

        // add operational attributes after call in case the operation fails
        Attributes attributes = new BasicAttributes();
        BasicAttribute attribute = new BasicAttribute( "modifiersName" );
        attribute.add( getPrincipal( call ).getName() );
        attributes.put( attribute );

        attribute = new BasicAttribute( "modifyTimestamp" );
        attribute.add( DateUtils.getGeneralizedTime() );
        attributes.put( attribute );

        nexus.modify( call.getNewParentName(), DirContext.REPLACE_ATTRIBUTE, attributes );
    }


    protected void process( NextInterceptor nextInterceptor, MoveAndModifyRN call ) throws NamingException
    {
        nextInterceptor.process( call );

        // add operational attributes after call in case the operation fails
        Attributes attributes = new BasicAttributes();
        BasicAttribute attribute = new BasicAttribute( "modifiersName" );
        attribute.add( getPrincipal( call ).getName() );
        attributes.put( attribute );

        attribute = new BasicAttribute( "modifyTimestamp" );
        attribute.add( DateUtils.getGeneralizedTime() );
        attributes.put( attribute );

        nexus.modify( call.getNewParentName(), DirContext.REPLACE_ATTRIBUTE, attributes );
    }


    protected void process( NextInterceptor nextInterceptor, Lookup call ) throws NamingException
    {
        nextInterceptor.process( call );

        Attributes attributes = ( Attributes ) call.getReturnValue();
        Attributes retval = ( Attributes ) attributes.clone();
        filter( retval );
        call.setReturnValue( retval );
    }


    protected void process( NextInterceptor nextInterceptor, LookupWithAttrIds call ) throws NamingException
    {
        nextInterceptor.process( call );

        Attributes attributes = ( Attributes ) call.getReturnValue();
        if ( attributes == null )
        {
            return;
        }

        Attributes retval = ( Attributes ) attributes.clone();
        filter( call.getName(), retval, call.getAttributeIds() );
        call.setReturnValue( retval );
    }


    protected void process( NextInterceptor nextInterceptor, List call ) throws NamingException
    {
        nextInterceptor.process( call );

        NamingEnumeration e;
        ResultFilteringEnumeration retval;
        LdapContext ctx = ( LdapContext ) call.getContextStack().peek();
        e = ( NamingEnumeration ) call.getReturnValue();
        retval = new ResultFilteringEnumeration( e, new SearchControls(), ctx, SEARCH_FILTER );
        call.setReturnValue( retval );
    }


    protected void process( NextInterceptor nextInterceptor, Search call ) throws NamingException
    {
        nextInterceptor.process( call );

        SearchControls searchControls = call.getControls();
        if ( searchControls.getReturningAttributes() != null )
        {
            return;
        }

        NamingEnumeration e;
        ResultFilteringEnumeration retval;
        LdapContext ctx = ( LdapContext ) call.getContextStack().peek();
        e = ( NamingEnumeration ) call.getReturnValue();
        retval = new ResultFilteringEnumeration( e, searchControls, ctx, SEARCH_FILTER );
        call.setReturnValue( retval );
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
            OperationalAttributeService.this.filter( entry );
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

                if ( !idsSet.contains( attrId ) )
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