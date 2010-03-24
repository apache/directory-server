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

import javax.naming.NamingException;

import org.apache.directory.server.core.interceptor.context.OperationContext;
import org.apache.directory.shared.ldap.aci.ACITuple;
import org.apache.directory.shared.ldap.aci.MicroOperation;
import org.apache.directory.shared.ldap.aci.ProtectedItem;
import org.apache.directory.shared.ldap.aci.ProtectedItem.RestrictedByItem;
import org.apache.directory.shared.ldap.constants.AuthenticationLevel;
import org.apache.directory.shared.ldap.entry.EntryAttribute;
import org.apache.directory.shared.ldap.entry.ServerEntry;
import org.apache.directory.shared.ldap.entry.Value;
import org.apache.directory.shared.ldap.name.DN;
import org.apache.directory.shared.ldap.schema.SchemaManager;


/**
 * An {@link ACITupleFilter} that discards all tuples that doesn't satisfy
 * {@link org.apache.directory.shared.ldap.aci.ProtectedItem.RestrictedBy} constraint if available. (18.8.3.3, X.501)
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 * @version $Rev$, $Date$
 */
public class RestrictedByFilter implements ACITupleFilter
{
    public Collection<ACITuple> filter( 
            SchemaManager schemaManager, 
            Collection<ACITuple> tuples, 
            OperationScope scope, 
            OperationContext opContext,
            Collection<DN> userGroupNames, 
            DN userName, 
            ServerEntry userEntry, 
            AuthenticationLevel authenticationLevel,
            DN entryName, 
            String attrId, 
            Value<?> attrValue, 
            ServerEntry entry, 
            Collection<MicroOperation> microOperations,
            ServerEntry entryView )
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

        for ( Iterator<ACITuple> ii = tuples.iterator() ; ii.hasNext(); )
        {
            ACITuple tuple = ii.next();
            
            if ( !tuple.isGrant() )
            {
                continue;
            }

            if ( isRemovable( tuple, attrId, attrValue, entry ) )
            {
                ii.remove();
            }
        }

        return tuples;
    }


    public boolean isRemovable( ACITuple tuple, String attrId, Value<?> attrValue, ServerEntry entry ) throws NamingException
    {
        for ( ProtectedItem item : tuple.getProtectedItems() )
        {
            if ( item instanceof ProtectedItem.RestrictedBy )
            {
                ProtectedItem.RestrictedBy rb = ( ProtectedItem.RestrictedBy ) item;
            
                for ( Iterator<RestrictedByItem> k = rb.iterator(); k.hasNext(); )
                {
                    RestrictedByItem rbItem = k.next();
                
                    // TODO Fix DIRSEVER-832 
                    if ( attrId.equalsIgnoreCase( rbItem.getAttributeType() ) )
                    {
                        EntryAttribute attr = entry.get( rbItem.getValuesIn() );
                        
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
