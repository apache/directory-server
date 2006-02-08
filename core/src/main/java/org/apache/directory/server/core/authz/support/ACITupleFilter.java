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

import javax.naming.Name;
import javax.naming.NamingException;
import javax.naming.directory.Attributes;

import org.apache.directory.server.core.partition.DirectoryPartitionNexusProxy;
import org.apache.directory.shared.ldap.aci.AuthenticationLevel;
import org.apache.directory.shared.ldap.aci.MicroOperation;


/**
 * An interface that filters the specified collection of tuples using the
 * specified extra information.
 *
 * @author The Apache Directory Project
 * @version $Rev$, $Date$
 *
 */
public interface ACITupleFilter
{
    /**
     * Returns the collection of the filtered tuples using the specified
     * extra information.
     * 
     * @param tuples the collection of tuples to filter
     * @param scope the scope of the operation to be performed
     * @param proxy the proxy interceptor for this filter to access the DIT
     * @param userGroupNames the collection of group ({@link Name})s which the current user belongs to
     * @param userName the {@link Name} of the current user
     * @param userEntry the {@link Attributes} of the current user entry in the DIT
     * @param authenticationLevel the level of authentication of the current user
     * @param entryName the {@link Name} of the entry the current user accesses
     * @param attrId the attribute ID the current user accesses
     * @param attrValue the value of the attribute the current user accesses
     * @param entry the {@link Attributes} of the entry the current user accesses
     * @param microOperations the set of {@link MicroOperation}s the current user will perform
     * 
     * @return the collection of filtered tuples
     * @throws NamingException if failed to filter the specifiec tuples
     */
    Collection filter(
            Collection tuples, OperationScope scope, DirectoryPartitionNexusProxy proxy,
            Collection userGroupNames, Name userName, Attributes userEntry,
            AuthenticationLevel authenticationLevel,
            Name entryName, String attrId, Object attrValue, Attributes entry,
            Collection microOperations ) throws NamingException;
}
