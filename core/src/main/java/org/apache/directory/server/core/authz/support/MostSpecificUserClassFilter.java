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
import org.apache.directory.shared.ldap.aci.UserClass;
import org.apache.directory.shared.ldap.model.entry.Entry;
import org.apache.directory.shared.ldap.model.exception.LdapException;


/**
 * An {@link ACITupleFilter} that chooses the tuples with the most specific user
 * class. (18.8.4.2)
 * <p>
 * If more than one tuple remains, choose the tuples with the most specific user
 * class. If there are any tuples matching the requestor with UserClasses element
 * name or thisEntry, discard all other tuples. Otherwise if there are any tuples
 * matching UserGroup, discard all other tuples. Otherwise if there are any tuples
 * matching subtree, discard all other tuples.
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 */
public class MostSpecificUserClassFilter implements ACITupleFilter
{
    public Collection<ACITuple> filter( AciContext aciContext, OperationScope scope, Entry userEntry ) throws LdapException
    {
        if ( aciContext.getAciTuples().size() <= 1 )
        {
            return aciContext.getAciTuples();
        }

        Collection<ACITuple> filteredTuples = new ArrayList<ACITuple>();

        // If there are any tuples matching the requestor with UserClasses
        // element name or thisEntry, discard all other tuples.
        for ( ACITuple tuple:aciContext.getAciTuples() )
        {
            for ( UserClass userClass:tuple.getUserClasses() )
            {
                if ( userClass instanceof UserClass.Name || userClass instanceof UserClass.ThisEntry )
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

        // Otherwise if there are any tuples matching UserGroup,
        // discard all other tuples.
        for ( ACITuple tuple:aciContext.getAciTuples() )
        {
            for ( UserClass userClass:tuple.getUserClasses() )
            {
                if ( userClass instanceof UserClass.UserGroup )
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

        // Otherwise if there are any tuples matching subtree,
        // discard all other tuples.
        for ( ACITuple tuple:aciContext.getAciTuples() )
        {
            for ( UserClass userClass:tuple.getUserClasses() )
            {
                if ( userClass instanceof UserClass.Subtree )
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

        return aciContext.getAciTuples();
    }

}
