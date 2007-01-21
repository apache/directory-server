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

package org.apache.directory.server.protocol.shared.catalog;


import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import javax.naming.Context;
import javax.naming.NamingEnumeration;
import javax.naming.directory.Attribute;
import javax.naming.directory.Attributes;
import javax.naming.directory.DirContext;
import javax.naming.directory.SearchControls;
import javax.naming.directory.SearchResult;

import org.apache.directory.server.protocol.shared.AbstractBackingStoreTest;


public class CatalogTest extends AbstractBackingStoreTest
{
    /**
     * Setup the backing store with test partitions.
     */
    public void setUp() throws Exception
    {
        super.setUp();

        loadPartition( "ou=system", "configuration-dns.ldif" );
    }


    public void testListCatalogEntries() throws Exception
    {
        String baseDn = "cn=org.apache.dns.1,cn=dns,ou=services,ou=configuration,ou=system";

        env.put( Context.PROVIDER_URL, baseDn );
        DirContext ctx = ( DirContext ) factory.getInitialContext( env );

        SearchControls controls = new SearchControls();
        controls.setSearchScope( SearchControls.SUBTREE_SCOPE );

        String[] returningAttributes = new String[]
            { "apacheCatalogEntryBaseDn", "apacheCatalogEntryName" };
        controls.setReturningAttributes( returningAttributes );

        Set set = new HashSet();
        NamingEnumeration list = ctx.search( "", "(objectClass=apacheCatalogEntry)", controls );

        Map map = new HashMap();

        while ( list.hasMore() )
        {
            SearchResult result = ( SearchResult ) list.next();
            set.add( result.getName() );

            Attributes attrs = result.getAttributes();
            Attribute attr;

            String catalogEntryBaseDn = ( attr = attrs.get( "apacheCatalogEntryBaseDn" ) ) != null ? ( String ) attr
                .get() : null;
            String catalogEntryName = ( attr = attrs.get( "apacheCatalogEntryName" ) ) != null ? ( String ) attr.get()
                : null;
            map.put( catalogEntryName, catalogEntryBaseDn );

            assertTrue( catalogEntryBaseDn.equals( "ou=zones,dc=example,dc=com" ) );
            assertTrue( catalogEntryName.equals( "example.com" ) );
        }

        assertTrue( set
            .contains( "cn=example.com,ou=catalog,cn=org.apache.dns.1,cn=dns,ou=services,ou=configuration,ou=system" ) );
    }
}
