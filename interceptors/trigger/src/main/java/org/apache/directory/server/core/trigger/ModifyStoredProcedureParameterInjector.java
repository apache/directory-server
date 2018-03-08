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


import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.apache.directory.api.ldap.model.constants.SchemaConstants;
import org.apache.directory.api.ldap.model.entry.Entry;
import org.apache.directory.api.ldap.model.entry.Modification;
import org.apache.directory.api.ldap.model.exception.LdapException;
import org.apache.directory.api.ldap.model.exception.LdapInvalidDnException;
import org.apache.directory.api.ldap.model.name.Dn;
import org.apache.directory.api.ldap.trigger.StoredProcedureParameter;
import org.apache.directory.server.core.api.CoreSession;
import org.apache.directory.server.core.api.interceptor.context.LookupOperationContext;
import org.apache.directory.server.core.api.interceptor.context.ModifyOperationContext;
import org.apache.directory.server.core.api.interceptor.context.OperationContext;


public class ModifyStoredProcedureParameterInjector extends AbstractStoredProcedureParameterInjector
{
    private Dn modifiedEntryName;
    private List<Modification> modifications;
    private Entry oldEntry;


    public ModifyStoredProcedureParameterInjector( ModifyOperationContext opContext ) throws LdapException
    {
        super( opContext );
        modifiedEntryName = opContext.getDn();
        modifications = opContext.getModItems();
        this.oldEntry = getEntry( opContext );
        Map<Class<?>, MicroInjector> injectors = super.getInjectors();
        injectors.put( StoredProcedureParameter.Modify_OBJECT.class, objectInjector );
        injectors.put( StoredProcedureParameter.Modify_MODIFICATION.class, modificationInjector );
        injectors.put( StoredProcedureParameter.Modify_OLD_ENTRY.class, oldEntryInjector );
        injectors.put( StoredProcedureParameter.Modify_NEW_ENTRY.class, newEntryInjector );
    }

    MicroInjector objectInjector = new MicroInjector()
    {
        public Object inject( OperationContext opContext, StoredProcedureParameter param )
            throws LdapInvalidDnException
        {
            // Return a safe copy constructed with user provided name.
            return opContext.getSession().getDirectoryService().getDnFactory().create( modifiedEntryName.getName() );
        }
    };

    MicroInjector modificationInjector = new MicroInjector()
    {
        public Object inject( OperationContext opContext, StoredProcedureParameter param ) throws LdapException
        {
            List<Modification> newMods = new ArrayList<Modification>();

            for ( Modification mod : modifications )
            {
                newMods.add( mod.clone() );
            }

            return newMods;
        }
    };

    MicroInjector oldEntryInjector = new MicroInjector()
    {
        public Object inject( OperationContext opContext, StoredProcedureParameter param ) throws LdapException
        {
            return oldEntry;
        }
    };

    MicroInjector newEntryInjector = new MicroInjector()
    {
        public Object inject( OperationContext opContext, StoredProcedureParameter param ) throws LdapException
        {
            return getEntry( opContext );
        }
    };


    private Entry getEntry( OperationContext opContext ) throws LdapException
    {
        /**
         * Exclude operational attributes while doing lookup
         * especially subentry related ones like "triggerExecutionSubentries".
         */
        CoreSession session = opContext.getSession();
        LookupOperationContext lookupContext = new LookupOperationContext( session, modifiedEntryName,
            SchemaConstants.ALL_USER_ATTRIBUTES_ARRAY );
        lookupContext.setPartition( opContext.getPartition() );
        lookupContext.setTransaction( opContext.getTransaction() );
        
        return session.getDirectoryService().getPartitionNexus().lookup( lookupContext );
    }
}
