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
package org.apache.eve.jndi.ibs;


import java.util.Set;
import java.util.HashSet;
import java.util.Collections;
import javax.naming.Name;
import javax.naming.NamingException;
import javax.naming.NamingEnumeration;
import javax.naming.ldap.LdapContext;
import javax.naming.directory.Attributes;
import javax.naming.directory.SearchControls;
import javax.naming.directory.Attribute;

import org.apache.eve.jndi.BaseInterceptor;
import org.apache.eve.RootNexus;
import org.apache.eve.db.SearchResultFilter;
import org.apache.eve.db.DbSearchResult;
import org.apache.eve.schema.GlobalRegistries;
import org.apache.eve.schema.AttributeTypeRegistry;

import org.apache.ldap.common.schema.AttributeType;
import org.apache.ldap.common.message.LockableAttributeImpl;


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


    /**
     * Creates a schema service interceptor.
     *
     * @param nexus the root nexus to access all database partitions
     * @param globalRegistries the global schema object registries
     * @param filterService
     */
    public SchemaService( RootNexus nexus, GlobalRegistries globalRegistries,
                          FilterService filterService )
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


        public boolean accept( LdapContext ctx, DbSearchResult result, SearchControls controls ) throws NamingException
        {
            doFilter( ctx, result.getAttributes() );
            return true;
        }
    }
}
