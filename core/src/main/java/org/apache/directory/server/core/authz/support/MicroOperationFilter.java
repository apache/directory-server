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
import org.apache.directory.shared.ldap.aci.MicroOperation;


/**
 * An {@link ACITupleFilter} that discard tuples which doesn't contain any
 * related {@link MicroOperation}s. (18.8.3.4, X.501) 
 *
 * @author The Apache Directory Project
 * @version $Rev$, $Date$
 *
 */
public class MicroOperationFilter implements ACITupleFilter
{
    public Collection filter( Collection tuples, OperationScope scope, DirectoryPartitionNexusProxy proxy,
        Collection userGroupNames, Name userName, Attributes userEntry, AuthenticationLevel authenticationLevel,
        Name entryName, String attrId, Object attrValue, Attributes entry, Collection microOperations )
        throws NamingException
    {
        if ( tuples.size() == 0 )
        {
            return tuples;
        }

        for ( Iterator i = tuples.iterator(); i.hasNext(); )
        {
            ACITuple tuple = ( ACITuple ) i.next();

            /*
             * The ACITuple must contain all the MicroOperations specified within the
             * microOperations argument.  Just matching a single microOperation is not
             * enough.  All must be matched to retain the ACITuple.
             */

            boolean retain = true;
            for ( Iterator j = microOperations.iterator(); j.hasNext(); )
            {
                MicroOperation microOp = ( MicroOperation ) j.next();
                if ( !tuple.getMicroOperations().contains( microOp ) )
                {
                    retain = false;
                    break;
                }
            }

            if ( !retain )
            {
                i.remove();
            }
        }

        return tuples;
    }

}
