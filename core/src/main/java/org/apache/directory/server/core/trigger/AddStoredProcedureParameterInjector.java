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

import org.apache.directory.server.core.interceptor.context.OperationContext;
import org.apache.directory.shared.ldap.model.entry.Entry;
import org.apache.directory.shared.ldap.model.exception.LdapException;
import org.apache.directory.shared.ldap.model.exception.LdapInvalidDnException;
import org.apache.directory.shared.ldap.model.name.Dn;
import org.apache.directory.shared.ldap.trigger.StoredProcedureParameter;


public class AddStoredProcedureParameterInjector extends AbstractStoredProcedureParameterInjector
{
    private Dn addedEntryName;
    private Entry addedEntry;
    
    
    public AddStoredProcedureParameterInjector( OperationContext opContext, Dn addedEntryName,
        Entry addedEntry )
    {
        super( opContext );
        this.addedEntryName = addedEntryName;
        this.addedEntry = addedEntry;
        Map<Class<?>, MicroInjector> injectors = super.getInjectors();
        injectors.put( StoredProcedureParameter.Add_ENTRY.class, $entryInjector );
        injectors.put( StoredProcedureParameter.Add_ATTRIBUTES.class, $attributesInjector );
    }

    
    MicroInjector $entryInjector = new MicroInjector()
    {
        public Object inject( OperationContext opContext, StoredProcedureParameter param ) throws LdapInvalidDnException
        {
            // Return a safe copy constructed with user provided name.
            return opContext.getSession().getDirectoryService().getDnFactory().create( addedEntryName.getName() );
        }
    };
    
    
    MicroInjector $attributesInjector = new MicroInjector()
    {
        public Object inject( OperationContext opContext, StoredProcedureParameter param ) throws LdapException
        {
            return addedEntry.clone();
        }
    };

}
