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
import java.util.Iterator;

import org.apache.directory.api.ldap.model.entry.Attribute;
import org.apache.directory.api.ldap.model.entry.Entry;
import org.apache.directory.api.ldap.model.entry.Value;
import org.apache.directory.api.ldap.model.exception.LdapException;
import org.apache.directory.api.ldap.model.schema.AttributeType;
import org.apache.directory.shared.ldap.aci.ACITuple;
import org.apache.directory.shared.ldap.aci.ProtectedItem;
import org.apache.directory.shared.ldap.aci.protectedItem.RestrictedByElem;
import org.apache.directory.shared.ldap.aci.protectedItem.RestrictedByItem;


/**
 * An {@link ACITupleFilter} that discards all tuples that doesn't satisfy
 * {@link org.apache.directory.shared.ldap.aci.protectedItem.RestrictedByItem} constraint if available. (18.8.3.3, X.501)
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 */
public class RestrictedByFilter implements ACITupleFilter
{
    public Collection<ACITuple> filter( AciContext aciContext, OperationScope scope, Entry userEntry )
        throws LdapException
    {
        if ( scope != OperationScope.ATTRIBUTE_TYPE_AND_VALUE )
        {
            return aciContext.getAciTuples();
        }

        if ( aciContext.getAciTuples().size() == 0 )
        {
            return aciContext.getAciTuples();
        }

        for ( Iterator<ACITuple> ii = aciContext.getAciTuples().iterator(); ii.hasNext(); )
        {
            ACITuple tuple = ii.next();

            if ( !tuple.isGrant() )
            {
                continue;
            }

            if ( isRemovable( tuple, aciContext.getAttributeType(), aciContext.getAttrValue(), aciContext.getEntry() ) )
            {
                ii.remove();
            }
        }

        return aciContext.getAciTuples();
    }


    public boolean isRemovable( ACITuple tuple, AttributeType attributeType, Value<?> attrValue, Entry entry )
        throws LdapException
    {
        for ( ProtectedItem item : tuple.getProtectedItems() )
        {
            if ( item instanceof RestrictedByItem )
            {
                RestrictedByItem rb = ( RestrictedByItem ) item;

                for ( Iterator<RestrictedByElem> k = rb.iterator(); k.hasNext(); )
                {
                    RestrictedByElem rbItem = k.next();

                    // TODO Fix DIRSEVER-832 
                    if ( attributeType.equals( rbItem.getAttributeType() ) )
                    {
                        Attribute attr = entry.get( rbItem.getValuesIn() );

                        // TODO Fix DIRSEVER-832
                        if ( ( attr == null ) || !attr.contains( attrValue ) )
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
