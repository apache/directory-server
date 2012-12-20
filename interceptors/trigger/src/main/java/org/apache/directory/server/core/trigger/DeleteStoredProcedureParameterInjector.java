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

import org.apache.directory.api.ldap.model.constants.SchemaConstants;
import org.apache.directory.api.ldap.model.entry.Entry;
import org.apache.directory.api.ldap.model.exception.LdapException;
import org.apache.directory.api.ldap.model.name.Dn;
import org.apache.directory.api.ldap.trigger.StoredProcedureParameter;
import org.apache.directory.server.core.api.CoreSession;
import org.apache.directory.server.core.api.interceptor.context.LookupOperationContext;
import org.apache.directory.server.core.api.interceptor.context.OperationContext;


public class DeleteStoredProcedureParameterInjector extends AbstractStoredProcedureParameterInjector
{
    private Dn deletedEntryName;
    private Entry deletedEntry;


    public DeleteStoredProcedureParameterInjector( OperationContext opContext, Dn deletedEntryName )
        throws LdapException
    {
        super( opContext );
        this.deletedEntryName = deletedEntryName;
        this.deletedEntry = getDeletedEntry( opContext );
        Map<Class<?>, MicroInjector> injectors = super.getInjectors();
        injectors.put( StoredProcedureParameter.Delete_NAME.class, $nameInjector );
        injectors.put( StoredProcedureParameter.Delete_DELETED_ENTRY.class, $deletedEntryInjector );
    }

    MicroInjector $nameInjector = new MicroInjector()
    {
        public Object inject( OperationContext opContext, StoredProcedureParameter param ) throws LdapException
        {
            // Return a safe copy constructed with user provided name.
            return opContext.getSession().getDirectoryService().getDnFactory().create( deletedEntryName.getName() );
        }
    };

    MicroInjector $deletedEntryInjector = new MicroInjector()
    {
        public Object inject( OperationContext opContext, StoredProcedureParameter param ) throws LdapException
        {
            return deletedEntry;
        }
    };


    private Entry getDeletedEntry( OperationContext opContext ) throws LdapException
    {
        /**
         * Using LOOKUP_EXCLUDING_OPR_ATTRS_BYPASS here to exclude operational attributes
         * especially subentry related ones like "triggerExecutionSubentries".
         */
        CoreSession session = opContext.getSession();
        LookupOperationContext lookupContext = new LookupOperationContext( session, deletedEntryName,
            SchemaConstants.ALL_USER_ATTRIBUTES_ARRAY );

        return session.getDirectoryService().getPartitionNexus().lookup( lookupContext );
    }
}
