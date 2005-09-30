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
package org.apache.ldap.server.authz.support;

import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;

import javax.naming.Name;
import javax.naming.NamingEnumeration;
import javax.naming.NamingException;
import javax.naming.directory.Attributes;
import javax.naming.directory.SearchControls;

import org.apache.ldap.common.aci.ACITuple;
import org.apache.ldap.common.aci.AuthenticationLevel;
import org.apache.ldap.common.aci.ProtectedItem;
import org.apache.ldap.common.filter.ExprNode;
import org.apache.ldap.common.filter.PresenceNode;
import org.apache.ldap.server.interceptor.NextInterceptor;

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
    
    public Collection filter( Collection tuples, OperationScope scope, NextInterceptor next, Name userGroupName, Name userName, Attributes userEntry, AuthenticationLevel authenticationLevel, Name entryName, String attrId, Object attrValue, Attributes entry, Collection microOperations ) throws NamingException
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
                        immSubCount = getImmSubCount( next, entryName );
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
    
    private int getImmSubCount( NextInterceptor next, Name entryName ) throws NamingException
    {
        int cnt = 0;
        NamingEnumeration e = null;
        try
        {
            e = next.search(
                entryName.getPrefix( 1 ), new HashMap(),
                childrenFilter, childrenSearchControls );
            
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
