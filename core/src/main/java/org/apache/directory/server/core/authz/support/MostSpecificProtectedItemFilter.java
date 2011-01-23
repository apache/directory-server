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


import java.util.ArrayList;
import java.util.Collection;

import org.apache.directory.shared.ldap.aci.ACITuple;
import org.apache.directory.shared.ldap.aci.ProtectedItem;
import org.apache.directory.shared.ldap.aci.protectedItem.AllAttributeValuesItem;
import org.apache.directory.shared.ldap.aci.protectedItem.AttributeTypeItem;
import org.apache.directory.shared.ldap.aci.protectedItem.AttributeValueItem;
import org.apache.directory.shared.ldap.aci.protectedItem.RangeOfValuesItem;
import org.apache.directory.shared.ldap.aci.protectedItem.SelfValueItem;
import org.apache.directory.shared.ldap.entry.Entry;
import org.apache.directory.shared.ldap.model.exception.LdapException;


/**
 * An {@link ACITupleFilter} that chooses the tuples with the most specific
 * protected item. (18.8.4.3, X.501)
 * <p>
 * If more than one tuple remains, choose the tuples with the most specific
 * protected item. If the protected item is an attribute and there are tuples 
 * that specify the attribute type explicitly, discard all other tuples. If
 * the protected item is an attribute value, and there are tuples that specify
 * the attribute value explicitly, discard all other tuples. A protected item
 * which is a rangeOfValues is to be treated as specifying an attribute value
 * explicitly.
 * 
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 */
public class MostSpecificProtectedItemFilter implements ACITupleFilter
{
    public Collection<ACITuple> filter( AciContext aciContext, OperationScope scope, Entry userEntry ) throws LdapException
    {
        if ( aciContext.getAciTuples().size() <= 1 )
        {
            return aciContext.getAciTuples();
        }

        Collection<ACITuple> filteredTuples = new ArrayList<ACITuple>();

        // If the protected item is an attribute and there are tuples that
        // specify the attribute type explicitly, discard all other tuples.
        for ( ACITuple tuple:aciContext.getAciTuples() )
        {
            for ( ProtectedItem item:tuple.getProtectedItems() )
            {
                if ( item instanceof AttributeTypeItem || item instanceof AllAttributeValuesItem
                    || item instanceof SelfValueItem || item instanceof AttributeValueItem )
                {
                    filteredTuples.add( tuple );
                    break;
                }
            }
        }

        if ( filteredTuples.size() > 0 )
        {
            return filteredTuples;
        }

        // If the protected item is an attribute value, and there are tuples
        // that specify the attribute value explicitly, discard all other tuples.
        // A protected item which is a rangeOfValues is to be treated as
        // specifying an attribute value explicitly. 
        for ( ACITuple tuple:aciContext.getAciTuples() )
        {
            for ( ProtectedItem item:tuple.getProtectedItems() )
            {
                if ( item instanceof RangeOfValuesItem)
                {
                    filteredTuples.add( tuple );
                }
            }
        }

        if ( filteredTuples.size() > 0 )
        {
            return filteredTuples;
        }

        return aciContext.getAciTuples();
    }
}
