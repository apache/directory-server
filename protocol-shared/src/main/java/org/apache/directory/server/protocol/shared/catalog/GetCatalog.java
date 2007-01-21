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
import java.util.Map;

import javax.naming.Name;
import javax.naming.NamingEnumeration;
import javax.naming.directory.Attribute;
import javax.naming.directory.Attributes;
import javax.naming.directory.DirContext;
import javax.naming.directory.SearchControls;
import javax.naming.directory.SearchResult;

import org.apache.directory.server.protocol.shared.store.ContextOperation;


/**
 * A JNDI context operation for building a catalog.
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 * @version $Rev$, $Date$
 */
public class GetCatalog implements ContextOperation
{
    private static final long serialVersionUID = -6657995003127926278L;
    private static final String ENTRY = "apacheCatalogEntry";
    private static final String ENTRY_NAME = "apacheCatalogEntryName";
    private static final String ENTRY_BASEDN = "apacheCatalogEntryBaseDn";


    /**
     * Note that the base is relative to the existing context.
     */
    public Object execute( DirContext ctx, Name base ) throws Exception
    {
        SearchControls controls = new SearchControls();
        controls.setSearchScope( SearchControls.SUBTREE_SCOPE );

        String filter = "(objectClass=" + ENTRY + ")";

        NamingEnumeration list = ctx.search( "", filter, controls );

        Map catalog = new HashMap();

        while ( list.hasMore() )
        {
            SearchResult result = ( SearchResult ) list.next();

            Attributes attrs = result.getAttributes();
            Attribute attr;

            String name = ( attr = attrs.get( ENTRY_NAME ) ) != null ? ( String ) attr.get() : null;
            String basedn = ( attr = attrs.get( ENTRY_BASEDN ) ) != null ? ( String ) attr.get() : null;

            catalog.put( name, basedn );
        }

        return catalog;
    }
}
