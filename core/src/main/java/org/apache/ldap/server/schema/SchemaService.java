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
package org.apache.ldap.server.schema;


import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import javax.naming.NamingEnumeration;
import javax.naming.NamingException;
import javax.naming.directory.Attribute;
import javax.naming.directory.Attributes;
import javax.naming.directory.SearchControls;
import javax.naming.directory.SearchResult;
import javax.naming.ldap.LdapContext;

import org.apache.ldap.common.filter.ExprNode;
import org.apache.ldap.common.filter.PresenceNode;
import org.apache.ldap.common.filter.SimpleNode;
import org.apache.ldap.common.message.LockableAttributeImpl;
import org.apache.ldap.common.message.LockableAttributesImpl;
import org.apache.ldap.common.name.LdapName;
import org.apache.ldap.common.schema.AttributeType;
import org.apache.ldap.common.schema.DITContentRule;
import org.apache.ldap.common.schema.DITStructureRule;
import org.apache.ldap.common.schema.MatchingRule;
import org.apache.ldap.common.schema.MatchingRuleUse;
import org.apache.ldap.common.schema.NameForm;
import org.apache.ldap.common.schema.ObjectClass;
import org.apache.ldap.common.schema.SchemaUtils;
import org.apache.ldap.common.schema.Syntax;
import org.apache.ldap.common.util.SingletonEnumeration;
import org.apache.ldap.server.interceptor.BaseInterceptor;
import org.apache.ldap.server.interceptor.InterceptorContext;
import org.apache.ldap.server.interceptor.NextInterceptor;
import org.apache.ldap.server.invocation.List;
import org.apache.ldap.server.invocation.Lookup;
import org.apache.ldap.server.invocation.LookupWithAttrIds;
import org.apache.ldap.server.invocation.Search;
import org.apache.ldap.server.jndi.ServerLdapContext;
import org.apache.ldap.server.partition.ContextPartitionNexus;
import org.apache.ldap.server.partition.impl.btree.ResultFilteringEnumeration;
import org.apache.ldap.server.partition.impl.btree.SearchResultFilter;


/**
 * An {@link org.apache.ldap.server.interceptor.Interceptor} that manages and enforces schemas.
 *
 * @todo Better interceptor description required.
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 * @version $Rev$, $Date$
 */
public class SchemaService extends BaseInterceptor
{
    private static final String BINARY_KEY = "java.naming.ldap.attributes.binary";

    /**
     * the root nexus to all database partitions
     */
    private ContextPartitionNexus nexus;

    /**
     * a binary attribute tranforming filter: String -> byte[]
     */
    private BinaryAttributeFilter binaryAttributeFilter;

    /**
     * the global schema object registries
     */
    private GlobalRegistries globalRegistries;

    private AttributeTypeRegistry attributeRegistry;

    /**
     * subschemaSubentry attribute's value from Root DSE
     */
    private String subentryDn;


    /**
     * Creates a schema service interceptor.
     */
    public SchemaService()
    {
    }


    public void init( InterceptorContext ctx ) throws NamingException
    {
        this.nexus = ctx.getRootNexus();
        this.globalRegistries = ctx.getGlobalRegistries();
        attributeRegistry = globalRegistries.getAttributeTypeRegistry();
        binaryAttributeFilter = new BinaryAttributeFilter();

        // stuff for dealing with subentries (garbage for now)
        String subschemaSubentry = ( String ) nexus.getRootDSE().get( "subschemaSubentry" ).get();
        subentryDn = new LdapName( subschemaSubentry ).toString().toLowerCase();
    }


    public void destroy()
    {
    }


    protected void process( NextInterceptor nextInterceptor, List call ) throws NamingException
    {
        nextInterceptor.process( call );

        NamingEnumeration e;
        ResultFilteringEnumeration retval;
        LdapContext ctx = ( LdapContext ) call.getContextStack().peek();
        e = ( NamingEnumeration ) call.getReturnValue();
        retval = new ResultFilteringEnumeration( e, new SearchControls(), ctx, binaryAttributeFilter );
        call.setReturnValue( retval );
    }


    protected void process( NextInterceptor nextInterceptor, Search call ) throws NamingException
    {
        // check to make sure the DN searched for is a subentry
        if ( !subentryDn.equals( call.getBaseName().toString() ) )
        {
            nextInterceptor.process( call );
            return;
        }

        boolean bypass = false;
        SearchControls searchControls = call.getControls();
        ExprNode filter = call.getFilter();
        if ( searchControls.getSearchScope() == SearchControls.OBJECT_SCOPE &&
                filter instanceof SimpleNode )
        {
            SimpleNode node = ( SimpleNode ) filter;

            if ( node.getAttribute().equalsIgnoreCase( "objectClass" ) &&
                    node.getValue().equalsIgnoreCase( "subschema" ) &&
                    node.getAssertionType() == SimpleNode.EQUALITY
            )
            {
                // call.setBypass( true );
                Attributes attrs = getSubschemaEntry( searchControls.getReturningAttributes() );
                SearchResult result = new SearchResult( call.getBaseName().toString(), null, attrs );
                SingletonEnumeration e = new SingletonEnumeration( result );
                call.setReturnValue( e );
                bypass = true;
            }
        }
        else if ( searchControls.getSearchScope() == SearchControls.OBJECT_SCOPE &&
                filter instanceof PresenceNode )
        {
            PresenceNode node = ( PresenceNode ) filter;

            if ( node.getAttribute().equalsIgnoreCase( "objectClass" ) )
            {
                // call.setBypass( true );
                Attributes attrs = getSubschemaEntry( searchControls.getReturningAttributes() );
                SearchResult result = new SearchResult( call.getBaseName().toString(), null, attrs );
                SingletonEnumeration e = new SingletonEnumeration( result );
                call.setReturnValue( e );
                bypass = true;
            }
        }

        if ( !bypass )
        {
            nextInterceptor.process( call );
        }

        if ( searchControls.getReturningAttributes() != null )
        {
            return;
        }

        NamingEnumeration e;
        ResultFilteringEnumeration retval;
        LdapContext ctx = ( LdapContext ) call.getContextStack().peek();
        e = ( NamingEnumeration ) call.getReturnValue();
        retval = new ResultFilteringEnumeration( e, searchControls, ctx, binaryAttributeFilter );
        call.setReturnValue( retval );
    }


    private Attributes getSubschemaEntry( String[] ids ) throws NamingException
    {
        if ( ids == null )
        {
            return new LockableAttributesImpl();
        }

        HashSet set = new HashSet( ids.length );
        LockableAttributesImpl attrs = new LockableAttributesImpl();
        LockableAttributeImpl attr = null;

        for ( int ii = 0; ii < ids.length; ii++ )
        {
            set.add( ids[ii].toLowerCase() );
        }


        if ( set.contains( "objectclasses" ) )
        {
            attr = new LockableAttributeImpl( attrs, "objectClasses" );
            Iterator list = globalRegistries.getObjectClassRegistry().list();
            while ( list.hasNext() )
            {
                ObjectClass oc = ( ObjectClass ) list.next();
                attr.add( SchemaUtils.render( oc ).toString() );
            }
            attrs.put( attr );
        }

        if ( set.contains( "attributetypes" ) )
        {
            attr = new LockableAttributeImpl( attrs, "attributeTypes" );
            Iterator list = globalRegistries.getAttributeTypeRegistry().list();
            while ( list.hasNext() )
            {
                AttributeType at = ( AttributeType ) list.next();
                attr.add( SchemaUtils.render( at ).toString() );
            }
            attrs.put( attr );
        }

        if ( set.contains( "matchingrules" ) )
        {
            attr = new LockableAttributeImpl( attrs, "matchingRules" );
            Iterator list = globalRegistries.getMatchingRuleRegistry().list();
            while ( list.hasNext() )
            {
                MatchingRule mr = ( MatchingRule ) list.next();
                attr.add( SchemaUtils.render( mr ).toString() );
            }
            attrs.put( attr );
        }

        if ( set.contains( "matchingruleuse" ) )
        {
            attr = new LockableAttributeImpl( attrs, "matchingRuleUse" );
            Iterator list = globalRegistries.getMatchingRuleUseRegistry().list();
            while ( list.hasNext() )
            {
                MatchingRuleUse mru = ( MatchingRuleUse ) list.next();
                attr.add( SchemaUtils.render( mru ).toString() );
            }
            attrs.put( attr );
        }

        if ( set.contains( "ldapsyntaxes" ) )
        {
            attr = new LockableAttributeImpl( attrs, "ldapSyntaxes" );
            Iterator list = globalRegistries.getSyntaxRegistry().list();
            while ( list.hasNext() )
            {
                Syntax syntax = ( Syntax ) list.next();
                attr.add( SchemaUtils.render( syntax ).toString() );
            }
            attrs.put( attr );
        }

        if ( set.contains( "ditcontentrules" ) )
        {
            attr = new LockableAttributeImpl( attrs, "dITContentRules" );
            Iterator list = globalRegistries.getDitContentRuleRegistry().list();
            while ( list.hasNext() )
            {
                DITContentRule dcr = ( DITContentRule ) list.next();
                attr.add( SchemaUtils.render( dcr ).toString() );
            }
            attrs.put( attr );
        }

        if ( set.contains( "ditstructurerules" ) )
        {
            attr = new LockableAttributeImpl( attrs, "dITStructureRules" );
            Iterator list = globalRegistries.getDitStructureRuleRegistry().list();
            while ( list.hasNext() )
            {
                DITStructureRule dsr = ( DITStructureRule ) list.next();
                attr.add( SchemaUtils.render( dsr ).toString() );
            }
            attrs.put( attr );
        }

        if ( set.contains( "nameforms" ) )
        {
            attr = new LockableAttributeImpl( attrs, "nameForms" );
            Iterator list = globalRegistries.getNameFormRegistry().list();
            while ( list.hasNext() )
            {
                NameForm nf = ( NameForm ) list.next();
                attr.add( SchemaUtils.render( nf ).toString() );
            }
            attrs.put( attr );
        }

        // add the objectClass attribute
        attr = new LockableAttributeImpl( attrs, "objectClass" );
        attr.add( "top" );
        attr.add( "subschema" );
        attrs.put( attr );

        // add the cn attribute as required for the RDN
        attrs.put( "cn", "schema" );

        return attrs;
    }


    protected void process( NextInterceptor nextInterceptor, Lookup call ) throws NamingException
    {
        nextInterceptor.process( call );

        ServerLdapContext ctx = ( ServerLdapContext ) call.getContextStack().peek();
        Attributes attributes = ( Attributes ) call.getReturnValue();
        Attributes retval = ( Attributes ) attributes.clone();
        doFilter( ctx, retval );
        call.setReturnValue( retval );
    }


    protected void process( NextInterceptor nextInterceptor, LookupWithAttrIds call ) throws NamingException
    {
        nextInterceptor.process( call );

        ServerLdapContext ctx = ( ServerLdapContext ) call.getContextStack().peek();
        Attributes attributes = ( Attributes ) call.getReturnValue();
        if ( attributes == null )
        {
            return;
        }

        Attributes retval = ( Attributes ) attributes.clone();
        doFilter( ctx, retval );
        call.setReturnValue( retval );
    }


    private void doFilter( LdapContext ctx, Attributes entry )
            throws NamingException
    {
        // set of AttributeType objects that are to behave as binaries
        Set binaries;
        
        // construct the set for fast lookups while filtering
        String binaryIds = ( String ) ctx.getEnvironment().get( BINARY_KEY );

        if ( binaryIds == null )
        {
            binaries = Collections.EMPTY_SET;
        }
        else
        {
            String[] binaryArray = binaryIds.split( " " );

            binaries = new HashSet( binaryArray.length );

            for ( int ii = 0; ii < binaryArray.length; ii++ )
            {
                AttributeType type = attributeRegistry.lookup( binaryArray[ii] );

                binaries.add( type );
            }
        }
        
        /*
         * start converting values of attributes to byte[]s which are not
         * human readable and those that are in the binaries set
         */
        NamingEnumeration list = entry.getIDs();

        while ( list.hasMore() )
        {
            String id = ( String ) list.next();

            AttributeType type = null;

            boolean asBinary = false;

            if ( attributeRegistry.hasAttributeType( id ) )
            {
                type = attributeRegistry.lookup( id );
            }

            if ( type != null )
            {
                asBinary = !type.getSyntax().isHumanReadible();

                asBinary = asBinary || binaries.contains( type );
            }

            if ( asBinary )
            {
                Attribute attribute = entry.get( id );

                Attribute binary = new LockableAttributeImpl( id );

                for ( int ii = 0; ii < attribute.size(); ii++ )
                {
                    Object value = attribute.get( ii );

                    if ( value instanceof String )
                    {
                        binary.add( ii, ( ( String ) value ).getBytes() );
                    }
                    else
                    {
                        binary.add( ii, value );
                    }
                }

                entry.remove( id );

                entry.put( binary );
            }
        }
    }


    /**
     * A special filter over entry attributes which replaces Attribute String values with their respective byte[]
     * representations using schema information and the value held in the JNDI environment property:
     * <code>java.naming.ldap.attributes.binary</code>.
     *
     * @see <a href= "http://java.sun.com/j2se/1.4.2/docs/guide/jndi/jndi-ldap-gl.html#binary">
     *      java.naming.ldap.attributes.binary</a>
     */
    private class BinaryAttributeFilter implements SearchResultFilter
    {
        public BinaryAttributeFilter()
        {
        }


        public boolean accept( LdapContext ctx, SearchResult result, SearchControls controls ) throws NamingException
        {
            doFilter( ctx, result.getAttributes() );
            return true;
        }
    }
}
