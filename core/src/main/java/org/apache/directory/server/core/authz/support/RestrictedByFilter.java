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


import java.util.Collection;
import java.util.Iterator;

import javax.naming.NamingException;
import javax.naming.directory.Attribute;
import javax.naming.directory.Attributes;

import org.apache.directory.server.core.partition.PartitionNexusProxy;
import org.apache.directory.shared.ldap.aci.ACITuple;
import org.apache.directory.shared.ldap.aci.AuthenticationLevel;
import org.apache.directory.shared.ldap.aci.ProtectedItem;
import org.apache.directory.shared.ldap.aci.ProtectedItem.RestrictedByItem;
import org.apache.directory.shared.ldap.name.LdapDN;


/**
 * An {@link ACITupleFilter} that discards all tuples that doesn't satisfy
 * {@link ProtectedItem.RestrictedBy} constraint if available. (18.8.3.3, X.501)
 *
 * @author The Apache Directory Proejct
 * @version $Rev$, $Date$
 */
public class RestrictedByFilter implements ACITupleFilter
{
    public Collection filter( Collection tuples, OperationScope scope, PartitionNexusProxy proxy,
                              Collection userGroupNames, LdapDN userName, Attributes userEntry, AuthenticationLevel authenticationLevel,
                              LdapDN entryName, String attrId, Object attrValue, Attributes entry, Collection microOperations )
        throws NamingException
    {
        if ( scope != OperationScope.ATTRIBUTE_TYPE_AND_VALUE )
        {
            return tuples;
        }

        if ( tuples.size() == 0 )
        {
            return tuples;
        }

        for ( Iterator i = tuples.iterator(); i.hasNext(); )
        {
            ACITuple tuple = ( ACITuple ) i.next();
            if ( !tuple.isGrant() )
            {
                continue;
            }

            if ( isRemovable( tuple, attrId, attrValue, entry ) )
            {
                i.remove();
            }
        }

        return tuples;
    }


    public boolean isRemovable( ACITuple tuple, String attrId, Object attrValue, Attributes entry )
    {
        for ( Iterator i = tuple.getProtectedItems().iterator(); i.hasNext(); )
        {
            ProtectedItem item = ( ProtectedItem ) i.next();
            if ( item instanceof ProtectedItem.RestrictedBy )
            {
                ProtectedItem.RestrictedBy rb = ( ProtectedItem.RestrictedBy ) item;
                for ( Iterator k = rb.iterator(); k.hasNext(); )
                {
                    RestrictedByItem rbItem = ( RestrictedByItem ) k.next();
                    if ( attrId.equalsIgnoreCase( rbItem.getAttributeType() ) )
                    {
                        Attribute attr = entry.get( rbItem.getValuesIn() );
                        if ( attr == null || !attr.contains( attrValue ) )
                        {
                            return true;
                        }
                    }
                }
            }
        }

        return false;
    }

}
