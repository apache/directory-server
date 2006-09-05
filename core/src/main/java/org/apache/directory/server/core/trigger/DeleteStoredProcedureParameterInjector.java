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

package org.apache.directory.server.core.trigger;


import java.util.Map;

import javax.naming.NamingException;
import javax.naming.directory.Attributes;

import org.apache.directory.server.core.invocation.Invocation;
import org.apache.directory.server.core.partition.PartitionNexusProxy;
import org.apache.directory.shared.ldap.name.LdapDN;
import org.apache.directory.shared.ldap.trigger.StoredProcedureParameter;


public class DeleteStoredProcedureParameterInjector extends AbstractStoredProcedureParameterInjector
{
    private LdapDN deletedEntryName;
    private Attributes deletedEntry;
    
    private Map injectors;
    
    public DeleteStoredProcedureParameterInjector( Invocation invocation, LdapDN deletedEntryName ) throws NamingException
    {
        super( invocation );
        this.deletedEntryName = deletedEntryName;
        this.deletedEntry = getDeletedEntry();
        injectors = super.getInjectors();
        injectors.put( StoredProcedureParameter.Delete_NAME.class, $nameInjector );
        injectors.put( StoredProcedureParameter.Delete_DELETED_ENTRY.class, $deletedEntryInjector );
    }
    
    MicroInjector $nameInjector = new MicroInjector()
    {
        public Object inject( StoredProcedureParameter param ) throws NamingException
        {
            // Return a safe copy constructed with user provided name.
            return new LdapDN( deletedEntryName.getUpName() );
        };
    };
    
    MicroInjector $deletedEntryInjector = new MicroInjector()
    {
        public Object inject( StoredProcedureParameter param ) throws NamingException
        {
            return deletedEntry;
        };
    };
    
    private Attributes getDeletedEntry() throws NamingException
    {
        PartitionNexusProxy proxy = getInvocation().getProxy();
        /**
         * Using LOOKUP_EXCLUDING_OPR_ATTRS_BYPASS here to exclude operational attributes
         * especially subentry related ones like "triggerExecutionSubentries".
         */
        Attributes deletedEntry = proxy.lookup( deletedEntryName, PartitionNexusProxy.LOOKUP_EXCLUDING_OPR_ATTRS_BYPASS );
        return deletedEntry;
    }
}
