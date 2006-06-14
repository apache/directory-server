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


import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;

import javax.naming.NamingException;
import javax.naming.directory.Attributes;

import org.apache.directory.server.core.partition.DirectoryPartitionNexusProxy;
import org.apache.directory.shared.ldap.aci.ACITuple;
import org.apache.directory.shared.ldap.aci.AuthenticationLevel;
import org.apache.directory.shared.ldap.aci.UserClass;
import org.apache.directory.shared.ldap.name.LdapDN;


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
 * @author The Apache Directory Project
 * @version $Rev$, $Date$
 */
public class MostSpecificUserClassFilter implements ACITupleFilter
{
    public Collection filter( Collection tuples, OperationScope scope, DirectoryPartitionNexusProxy proxy,
                              Collection userGroupNames, LdapDN userName, Attributes userEntry, AuthenticationLevel authenticationLevel,
                              LdapDN entryName, String attrId, Object attrValue, Attributes entry, Collection microOperations )
        throws NamingException
    {
        if ( tuples.size() <= 1 )
        {
            return tuples;
        }

        Collection filteredTuples = new ArrayList();

        // If there are any tuples matching the requestor with UserClasses
        // element name or thisEntry, discard all other tuples.
        for ( Iterator i = tuples.iterator(); i.hasNext(); )
        {
            ACITuple tuple = ( ACITuple ) i.next();
            for ( Iterator j = tuple.getUserClasses().iterator(); j.hasNext(); )
            {
                UserClass userClass = ( UserClass ) j.next();
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
        for ( Iterator i = tuples.iterator(); i.hasNext(); )
        {
            ACITuple tuple = ( ACITuple ) i.next();
            for ( Iterator j = tuple.getUserClasses().iterator(); j.hasNext(); )
            {
                UserClass userClass = ( UserClass ) j.next();
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
        for ( Iterator i = tuples.iterator(); i.hasNext(); )
        {
            ACITuple tuple = ( ACITuple ) i.next();
            for ( Iterator j = tuple.getUserClasses().iterator(); j.hasNext(); )
            {
                UserClass userClass = ( UserClass ) j.next();
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

        return tuples;
    }

}
