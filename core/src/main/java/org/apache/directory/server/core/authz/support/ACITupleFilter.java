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

import org.apache.directory.server.core.interceptor.context.OperationContext;
import org.apache.directory.shared.ldap.aci.ACITuple;
import org.apache.directory.shared.ldap.aci.MicroOperation;
import org.apache.directory.shared.ldap.constants.AuthenticationLevel;
import org.apache.directory.shared.ldap.entry.Entry;
import org.apache.directory.shared.ldap.entry.Value;
import org.apache.directory.shared.ldap.exception.LdapException;
import org.apache.directory.shared.ldap.name.DN;
import org.apache.directory.shared.ldap.schema.SchemaManager;


/**
 * An interface that filters the specified collection of tuples using the
 * specified extra information.
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
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
     * @param userGroupNames the collection of group ({@link DN})s which the current user belongs to
     * @param userName the {@link DN} of the current user
     * @param userEntry the {@link Entry} of the current user entry in the DIT
     * @param authenticationLevel the level of authentication of the current user
     * @param entryName the {@link DN} of the entry the current user accesses
     * @param attrId the attribute ID the current user accesses
     * @param attrValue the value of the attribute the current user accesses
     * @param entry the {@link Entry} of the entry the current user accesses
     * @param microOperations the set of {@link MicroOperation}s the current user will perform
     * @param entryView in case of a Modify operation, view of the entry being modified as if the modification permitted and completed
     * @return the collection of filtered tuples
     * @throws LdapException if failed to filter the specific tuples
     */
    Collection<ACITuple> filter( 
            SchemaManager schemaManager, 
            Collection<ACITuple> tuples, 
            OperationScope scope, 
            OperationContext opContext,
            Collection<DN> userGroupNames, 
            DN userName, 
            Entry userEntry,
            AuthenticationLevel authenticationLevel, 
            DN entryName, 
            String attrId,
            Value<?> attrValue, 
            Entry entry, 
            Collection<MicroOperation> microOperations,
            Entry entryView )
        throws LdapException;
}
