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

import org.apache.directory.api.ldap.aci.ACITuple;
import org.apache.directory.api.ldap.aci.ProtectedItem;
import org.apache.directory.api.ldap.aci.protectedItem.MaxValueCountElem;
import org.apache.directory.api.ldap.aci.protectedItem.MaxValueCountItem;
import org.apache.directory.api.ldap.model.entry.Attribute;
import org.apache.directory.api.ldap.model.entry.Entry;
import org.apache.directory.api.ldap.model.exception.LdapException;
import org.apache.directory.api.ldap.model.schema.AttributeType;


/**
 * An {@link ACITupleFilter} that discards all tuples that doesn't satisfy
 * {@link org.apache.directory.api.ldap.aci.protectedItem.MaxValueCountItem} constraint if available. (18.8.3.3, X.501)
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 */
public class MaxValueCountFilter implements ACITupleFilter
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

        for ( Iterator<ACITuple> i = aciContext.getAciTuples().iterator(); i.hasNext(); )
        {
            ACITuple tuple = i.next();

            if ( !tuple.isGrant() )
            {
                continue;
            }

            for ( Iterator<ProtectedItem> j = tuple.getProtectedItems().iterator(); j.hasNext(); )
            {
                ProtectedItem item = j.next();

                if ( item instanceof MaxValueCountItem )
                {
                    MaxValueCountItem mvc = ( MaxValueCountItem ) item;

                    if ( isRemovable( mvc, aciContext.getAttributeType(), aciContext.getEntryView() ) )
                    {
                        i.remove();
                        break;
                    }
                }
            }
        }

        return aciContext.getAciTuples();
    }


    private boolean isRemovable( MaxValueCountItem mvc, AttributeType attributeType, Entry entryView )
        throws LdapException
    {
        for ( Iterator<MaxValueCountElem> k = mvc.iterator(); k.hasNext(); )
        {
            MaxValueCountElem mvcItem = k.next();

            if ( attributeType.equals( mvcItem.getAttributeType() ) )
            {
                Attribute attr = entryView.get( attributeType );
                int attrCount = attr == null ? 0 : attr.size();

                if ( attrCount > mvcItem.getMaxCount() )
                {
                    return true;
                }
            }
        }

        return false;
    }

}
