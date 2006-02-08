/*
 *   @(#) $Id$
 *   
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
package org.apache.directory.server.core.authz.support;

import java.util.*;

import javax.naming.Name;
import javax.naming.NamingEnumeration;
import javax.naming.NamingException;
import javax.naming.directory.Attributes;
import javax.naming.directory.SearchControls;

import org.apache.directory.server.core.partition.DirectoryPartitionNexusProxy;
import org.apache.directory.shared.ldap.aci.ACITuple;
import org.apache.directory.shared.ldap.aci.AuthenticationLevel;
import org.apache.directory.shared.ldap.aci.ProtectedItem;
import org.apache.directory.shared.ldap.filter.ExprNode;
import org.apache.directory.shared.ldap.filter.PresenceNode;


/**
 * An {@link ACITupleFilter} that discards all tuples that doesn't satisfy
 * {@link ProtectedItem.MaxImmSub} constraint if available. (18.8.3.3, X.501)
 *
 * @author The Apache Directory Project
 * @version $Rev$, $Date$
 */
public class MaxImmSubFilter implements ACITupleFilter
{
    private final ExprNode childrenFilter;
    private final SearchControls childrenSearchControls;

    public MaxImmSubFilter()
    {
        childrenFilter = new PresenceNode( "objectClass" );
        childrenSearchControls = new SearchControls();
        childrenSearchControls.setSearchScope( SearchControls.ONELEVEL_SCOPE );
    }

    public Collection filter( Collection tuples, OperationScope scope, DirectoryPartitionNexusProxy proxy, Collection userGroupNames, Name userName, Attributes userEntry, AuthenticationLevel authenticationLevel, Name entryName, String attrId, Object attrValue, Attributes entry, Collection microOperations ) throws NamingException
    {
        if( entryName.size() == 0 )
        {
            return tuples;
        }

        if( tuples.size() == 0 )
        {
            return tuples;
        }

        if( scope != OperationScope.ENTRY )
        {
            return tuples;
        }

        int immSubCount = -1;

        for( Iterator i = tuples.iterator(); i.hasNext(); )
        {
            ACITuple tuple = ( ACITuple ) i.next();
            if( !tuple.isGrant() )
            {
                continue;
            }

            for( Iterator j = tuple.getProtectedItems().iterator(); j.hasNext(); )
            {
                ProtectedItem item = ( ProtectedItem ) j.next();
                if( item instanceof ProtectedItem.MaxImmSub )
                {
                    if( immSubCount < 0 )
                    {
                        immSubCount = getImmSubCount( proxy, entryName );
                    }

                    ProtectedItem.MaxImmSub mis = ( ProtectedItem.MaxImmSub ) item;
                    if( immSubCount >= mis.getValue() )
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
        Collection c = new HashSet();
        c.add( "normalizationService" );
        c.add( "authenticationService" );
        c.add( "authorizationService" );
        c.add( "oldAuthorizationService" );
        c.add( "schemaService" );
        c.add( "subentryService" );
        c.add( "operationalAttributeService" );
        c.add( "eventService" );
        SEARCH_BYPASS = Collections.unmodifiableCollection( c );
    }


    private int getImmSubCount( DirectoryPartitionNexusProxy proxy, Name entryName ) throws NamingException
    {
        int cnt = 0;
        NamingEnumeration e = null;
        try
        {
            e = proxy.search(
                entryName.getPrefix( 1 ), new HashMap(),
                childrenFilter, childrenSearchControls, SEARCH_BYPASS );

            while( e.hasMore() )
            {
                e.next();
                cnt ++;
            }

        }
        finally
        {
            if( e != null )
            {
                e.close();
            }
        }

        return cnt;
    }

}
