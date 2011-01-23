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

import org.apache.directory.shared.ldap.aci.ACITuple;
import org.apache.directory.shared.ldap.constants.Loggers;
import org.apache.directory.shared.ldap.model.entry.Entry;
import org.apache.directory.shared.ldap.model.exception.LdapException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * An interface that filters the specified collection of tuples using the
 * specified extra information.
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 *
 */
public interface ACITupleFilter
{
    /** the dedicated logger for ACI */
    static final Logger ACI_LOG = LoggerFactory.getLogger( Loggers.ACI_LOG.getName() );

    /**
     * Returns the collection of the filtered tuples using the specified
     * extra information.
     * 
     * @param aciContext the container for ACI items
     * @param scope the scope of the operation to be performed
     * @param userEntry the {@link org.apache.directory.shared.ldap.model.entry.Entry} of the current user entry in the DIT
     * @return the collection of filtered tuples
     * @throws org.apache.directory.shared.ldap.model.exception.LdapException if failed to filter the specific tuples
     */
    Collection<ACITuple> filter( AciContext aciContext, OperationScope scope, Entry userEntry ) throws LdapException;
}
