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
package org.apache.ldap.server.jndi.ibs;


import java.util.*;
import javax.naming.Name;
import javax.naming.NamingException;
import javax.naming.NamingEnumeration;
import javax.naming.ldap.LdapContext;
import javax.naming.directory.Attributes;
import javax.naming.directory.SearchControls;
import javax.naming.directory.Attribute;
import javax.naming.directory.SearchResult;

import org.apache.ldap.server.jndi.BaseInterceptor;
import org.apache.ldap.server.jndi.Invocation;
import org.apache.ldap.server.jndi.InvocationStateEnum;
import org.apache.eve.RootNexus;
import org.apache.ldap.server.db.SearchResultFilter;
import org.apache.ldap.server.schema.GlobalRegistries;
import org.apache.ldap.server.schema.AttributeTypeRegistry;
import org.apache.ldap.server.db.SearchResultFilter;

import org.apache.ldap.common.schema.*;
import org.apache.ldap.common.message.LockableAttributeImpl;
import org.apache.ldap.common.message.LockableAttributesImpl;
import org.apache.ldap.common.filter.ExprNode;
import org.apache.ldap.common.filter.SimpleNode;
import org.apache.ldap.common.filter.PresenceNode;
import org.apache.ldap.common.util.SingletonEnumeration;
import org.apache.ldap.common.name.LdapName;


/**
 * A schema management and enforcement interceptor service.
 *
 * @author <a href="mailto:directory-dev@incubator.apache.org">Apache Directory Project</a>
 * @version $Rev$
 */
public class SchemaService extends BaseInterceptor
{
    /** the root nexus to all database partitions */
    private final RootNexus nexus;
    /** a binary attribute tranforming filter: String -> byte[] */
    private final BinaryAttributeFilter binaryAttributeFilter;
    /** the filter service used by the schema service */
    private final FilterService filterService;
    /** the global schema object registries */
    private final GlobalRegistries globalRegistries;
    /** subschemaSubentry attribute's value from Root DSE */
    private final String subentryDn;


    /**
     * Creates a schema service interceptor.
     *
     * @param nexus the root nexus to access all database partitions
     * @param globalRegistries the global schema object registries
     * @param filterService
     */
    public SchemaService( RootNexus nexus, GlobalRegistries globalRegistries,
                          FilterService filterService ) throws NamingException
    {
        this.nexus = nexus;
        if ( this.nexus == null )
        {
            throw new NullPointerException( "the nexus cannot be null" );
        }

        this.globalRegistries = globalRegistries;
        if ( this.globalRegistries == null )
        {
            throw new NullPointerException( "the global registries cannot be null" );
        }

        this.filterService = filterService;
        if ( this.filterService == null )
        {
            throw new NullPointerException( "the filter service cannot be null" );
        }

        binaryAttributeFilter = new BinaryAttributeFilter(
                globalRegistries.getAttributeTypeRegistry() );
        filterService.addLookupFilter( binaryAttributeFilter );
        filterService.addSearchResultFilter( binaryAttributeFilter );

        // stuff for dealing with subentries (garbage for now)
        String subschemaSubentry = ( String ) nexus.getRootDSE().get( "subschemaSubentry" ).get();
        subentryDn = new LdapName( subschemaSubentry ).toString().toLowerCase();
    }


    /**
     * A special filter over entry attributes which replaces Attribute String
     * values with their respective byte[] representations using schema
     * information and the value held in the JNDI environment property:
     * <code>java.naming.ldap.attributes.binary</code>.
     *
     * @see <a href=
     * "http://java.sun.com/j2se/1.4.2/docs/guide/jndi/jndi-ldap-gl.html#binary">
     * java.naming.ldap.attributes.binary</a>
     */
    private class BinaryAttributeFilter implements LookupFilter, SearchResultFilter
    {
        private final static String BINARY_KEY =
                "java.naming.ldap.attributes.binary";
        private final AttributeTypeRegistry registry;


        public BinaryAttributeFilter( AttributeTypeRegistry registry )
        {
            this.registry = registry;
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
                    AttributeType type = registry.lookup( binaryArray[ii] );
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
                AttributeType type = registry.lookup( id );
                boolean isBinary = ! type.getSyntax().isHumanReadible();

                if ( isBinary || binaries.contains( type ) )
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


        public void filter( LdapContext ctx, Name dn, Attributes entry ) throws NamingException
        {
            doFilter( ctx, entry );
        }


        public void filter( LdapContext ctx, Name dn, Attributes entry, String[] ids )
                throws NamingException
        {
            doFilter( ctx, entry );
        }


        public boolean accept( LdapContext ctx, SearchResult result, SearchControls controls ) throws NamingException
        {
            doFilter( ctx, result.getAttributes() );
            return true;
        }
    }


    protected void search( Name base, Map env, ExprNode filter,
                           SearchControls searchControls ) throws NamingException
    {
        Invocation invocation = getInvocation();

        // check to make sure the DN searched for is a subentry
        if ( ! subentryDn.equals( base.toString() ) )
        {
            return;
        }

        if ( invocation.getState() == InvocationStateEnum.PREINVOCATION )
        {
            if ( searchControls.getSearchScope() == SearchControls.OBJECT_SCOPE &&
                 filter instanceof SimpleNode )
            {
                SimpleNode node = ( SimpleNode ) filter;

                if ( node.getAttribute().equalsIgnoreCase( "objectClass" ) &&
                     node.getValue().equalsIgnoreCase( "subschema" ) &&
                     node.getAssertionType() == SimpleNode.EQUALITY
                   )
                {
                    invocation.setBypass( true );
                    Attributes attrs = getSubschemaEntry( searchControls.getReturningAttributes() );
                    SearchResult result = new SearchResult( base.toString(), null, attrs );
                    SingletonEnumeration enum = new SingletonEnumeration( result );
                    invocation.setReturnValue( enum );
                }
            }
            else if ( searchControls.getSearchScope() == SearchControls.OBJECT_SCOPE &&
                     filter instanceof PresenceNode )
            {
                PresenceNode node = ( PresenceNode ) filter;

                if ( node.getAttribute().equalsIgnoreCase( "objectClass" ) )
                {
                    invocation.setBypass( true );
                    Attributes attrs = getSubschemaEntry( searchControls.getReturningAttributes() );
                    SearchResult result = new SearchResult( base.toString(), null, attrs );
                    SingletonEnumeration enum = new SingletonEnumeration( result );
                    invocation.setReturnValue( enum );
                }
            }
        }
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
}
