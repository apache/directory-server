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

import javax.naming.Name;
import javax.naming.NamingException;
import javax.naming.directory.Attributes;

import org.apache.directory.server.core.partition.DirectoryPartitionNexusProxy;
import org.apache.directory.shared.ldap.aci.ACITuple;
import org.apache.directory.shared.ldap.aci.AuthenticationLevel;


/**
 * An {@link ACITupleFilter} that discards all tuples having a precedence less
 * than the highest remaining precedence. (18.8.4.1, X.501)
 *
 * @author The Apache Directory Project
 * @version $Rev$, $Date$
 */
public class HighestPrecedenceFilter implements ACITupleFilter
{
    public Collection filter( Collection tuples, OperationScope scope, DirectoryPartitionNexusProxy proxy,
        Collection userGroupNames, Name userName, Attributes userEntry, AuthenticationLevel authenticationLevel,
        Name entryName, String attrId, Object attrValue, Attributes entry, Collection microOperations )
        throws NamingException
    {
        if ( tuples.size() <= 1 )
        {
            return tuples;
        }

        int maxPrecedence = -1;

        // Find the maximum precedence for all tuples.
        for ( Iterator i = tuples.iterator(); i.hasNext(); )
        {
            ACITuple tuple = ( ACITuple ) i.next();
            if ( tuple.getPrecedence() > maxPrecedence )
            {
                maxPrecedence = tuple.getPrecedence();
            }
        }

        // Remove all tuples whose precedences are not the maximum one.
        for ( Iterator i = tuples.iterator(); i.hasNext(); )
        {
            ACITuple tuple = ( ACITuple ) i.next();
            if ( tuple.getPrecedence() != maxPrecedence )
            {
                i.remove();
            }
        }

        return tuples;
    }
}
