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
package org.apache.directory.server.core.authz.support;


import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;

import javax.naming.NamingEnumeration;
import javax.naming.NamingException;
import javax.naming.directory.Attributes;
import javax.naming.directory.SearchControls;
import javax.naming.directory.SearchResult;

import org.apache.directory.server.core.configuration.StartupConfiguration;
import org.apache.directory.server.core.interceptor.context.SearchOperationContext;
import org.apache.directory.server.core.partition.PartitionNexusProxy;
import org.apache.directory.shared.ldap.aci.ACITuple;
import org.apache.directory.shared.ldap.aci.AuthenticationLevel;
import org.apache.directory.shared.ldap.aci.ProtectedItem;
import org.apache.directory.shared.ldap.constants.SchemaConstants;
import org.apache.directory.shared.ldap.filter.ExprNode;
import org.apache.directory.shared.ldap.filter.PresenceNode;
import org.apache.directory.shared.ldap.name.LdapDN;


/**
 * An {@link ACITupleFilter} that discards all tuples that doesn't satisfy
 * {@link ProtectedItem.MaxImmSub} constraint if available. (18.8.3.3, X.501)
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 * @version $Rev$, $Date$
 */
public class MaxImmSubFilter implements ACITupleFilter
{
    private final ExprNode childrenFilter;
    private final SearchControls childrenSearchControls;


    public MaxImmSubFilter()
    {
        childrenFilter = new PresenceNode( SchemaConstants.OBJECT_CLASS_AT );
        childrenSearchControls = new SearchControls();
        childrenSearchControls.setSearchScope( SearchControls.ONELEVEL_SCOPE );
    }


    public Collection filter( Collection tuples, OperationScope scope, PartitionNexusProxy proxy,
                              Collection userGroupNames, LdapDN userName, Attributes userEntry, AuthenticationLevel authenticationLevel,
                              LdapDN entryName, String attrId, Object attrValue, Attributes entry, Collection microOperations )
        throws NamingException
    {
        if ( entryName.size() == 0 )
        {
            return tuples;
        }

        if ( tuples.size() == 0 )
        {
            return tuples;
        }

        if ( scope != OperationScope.ENTRY )
        {
            return tuples;
        }

        int immSubCount = -1;

        for ( Iterator i = tuples.iterator(); i.hasNext(); )
        {
            ACITuple tuple = ( ACITuple ) i.next();
            if ( !tuple.isGrant() )
            {
                continue;
            }

            for ( Iterator j = tuple.getProtectedItems().iterator(); j.hasNext(); )
            {
                ProtectedItem item = ( ProtectedItem ) j.next();
                if ( item instanceof ProtectedItem.MaxImmSub )
                {
                    if ( immSubCount < 0 )
                    {
                        immSubCount = getImmSubCount( proxy, entryName );
                    }

                    ProtectedItem.MaxImmSub mis = ( ProtectedItem.MaxImmSub ) item;
                    if ( immSubCount >= mis.getValue() )
                    {
                        i.remove();
                        break;
                    }
                }
            }
        }

        return tuples;
    }

    public static final Collection SEARCH_BYPASS;
    static
    {
        Collection<String> c = new HashSet<String>();
        c.add( StartupConfiguration.NORMALIZATION_SERVICE_NAME );
        c.add( StartupConfiguration.AUTHENTICATION_SERVICE_NAME );
        c.add( StartupConfiguration.AUTHORIZATION_SERVICE_NAME );
        c.add( StartupConfiguration.DEFAULT_AUTHORIZATION_SERVICE_NAME );
        c.add( StartupConfiguration.SCHEMA_SERVICE_NAME );
        c.add( StartupConfiguration.SUBENTRY_SERVICE_NAME );
        c.add( StartupConfiguration.OPERATIONAL_ATTRIBUTE_SERVICE_NAME );
        c.add( StartupConfiguration.EVENT_SERVICE_NAME );
        SEARCH_BYPASS = Collections.unmodifiableCollection( c );
    }


    private int getImmSubCount( PartitionNexusProxy proxy, LdapDN entryName ) throws NamingException
    {
        int cnt = 0;
        NamingEnumeration<SearchResult> e = null;
        
        try
        {
            e = proxy.search( 
                new SearchOperationContext( ( LdapDN ) entryName.getPrefix( 1 ), new HashMap(), childrenFilter, childrenSearchControls ),
                SEARCH_BYPASS );

            while ( e.hasMore() )
            {
                e.next();
                cnt++;
            }

        }
        finally
        {
            if ( e != null )
            {
                e.close();
            }
        }

        return cnt;
    }

}
