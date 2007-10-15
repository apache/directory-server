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


import org.apache.directory.server.core.authn.AuthenticationInterceptor;
import org.apache.directory.server.core.authz.AciAuthorizationInterceptor;
import org.apache.directory.server.core.authz.DefaultAuthorizationInterceptor;
import org.apache.directory.server.core.event.EventInterceptor;
import org.apache.directory.server.core.interceptor.context.SearchOperationContext;
import org.apache.directory.server.core.normalization.NormalizationInterceptor;
import org.apache.directory.server.core.operational.OperationalAttributeInterceptor;
import org.apache.directory.server.core.partition.PartitionNexusProxy;
import org.apache.directory.server.core.schema.SchemaInterceptor;
import org.apache.directory.server.core.subtree.SubentryInterceptor;
import org.apache.directory.shared.ldap.aci.ACITuple;
import org.apache.directory.shared.ldap.aci.AuthenticationLevel;
import org.apache.directory.shared.ldap.aci.MicroOperation;
import org.apache.directory.shared.ldap.aci.ProtectedItem;
import org.apache.directory.shared.ldap.constants.SchemaConstants;
import org.apache.directory.shared.ldap.filter.ExprNode;
import org.apache.directory.shared.ldap.filter.PresenceNode;
import org.apache.directory.shared.ldap.message.DerefAliasesEnum;
import org.apache.directory.shared.ldap.name.LdapDN;

import javax.naming.Name;
import javax.naming.NamingEnumeration;
import javax.naming.NamingException;
import javax.naming.directory.Attributes;
import javax.naming.directory.SearchControls;
import javax.naming.directory.SearchResult;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;


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


    public Collection<ACITuple> filter( 
            Collection<ACITuple> tuples, 
            OperationScope scope, 
            PartitionNexusProxy proxy,
            Collection<Name> userGroupNames, 
            LdapDN userName, 
            Attributes userEntry, 
            AuthenticationLevel authenticationLevel,
            LdapDN entryName, 
            String attrId, 
            Object attrValue, 
            Attributes entry, 
            Collection<MicroOperation> microOperations,
            Attributes entryView )
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

        for ( Iterator<ACITuple> i = tuples.iterator(); i.hasNext(); )
        {
            ACITuple tuple = i.next();
            if ( !tuple.isGrant() )
            {
                continue;
            }

            for ( ProtectedItem item : tuple.getProtectedItems() )
            {
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

    public static final Collection<String> SEARCH_BYPASS;
    static
    {
        Collection<String> c = new HashSet<String>();
        c.add( NormalizationInterceptor.class.getName() );
        c.add( AuthenticationInterceptor.class.getName() );
//        c.add( ReferralInterceptor.class.getName() );
        c.add( AciAuthorizationInterceptor.class.getName() );
        c.add( DefaultAuthorizationInterceptor.class.getName() );
//        c.add( ExceptionInterceptor.class.getName() );
        c.add( OperationalAttributeInterceptor.class.getName() );
        c.add( SchemaInterceptor.class.getName() );
        c.add( SubentryInterceptor.class.getName() );
//        c.add( CollectiveAttributeInterceptor.class.getName() );
        c.add( EventInterceptor.class.getName() );
//        c.add( TriggerInterceptor.class.getName() );
        SEARCH_BYPASS = Collections.unmodifiableCollection( c );
    }


    private int getImmSubCount( PartitionNexusProxy proxy, LdapDN entryName ) throws NamingException
    {
        int cnt = 0;
        NamingEnumeration<SearchResult> e = null;
        
        try
        {
            e = proxy.search( new SearchOperationContext( ( LdapDN ) entryName.getPrefix( 1 ),
                    DerefAliasesEnum.DEREF_ALWAYS, childrenFilter, childrenSearchControls ), SEARCH_BYPASS );

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
