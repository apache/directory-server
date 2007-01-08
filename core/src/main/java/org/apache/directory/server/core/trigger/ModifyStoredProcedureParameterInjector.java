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

import javax.naming.NamingEnumeration;
import javax.naming.NamingException;
import javax.naming.directory.Attribute;
import javax.naming.directory.Attributes;

import org.apache.directory.server.core.invocation.Invocation;
import org.apache.directory.server.core.partition.PartitionNexusProxy;
import org.apache.directory.shared.ldap.message.ModificationItemImpl;
import org.apache.directory.shared.ldap.name.LdapDN;
import org.apache.directory.shared.ldap.trigger.StoredProcedureParameter;

public class ModifyStoredProcedureParameterInjector extends AbstractStoredProcedureParameterInjector
{
    private LdapDN modifiedEntryName;
    private ModificationItemImpl[] modifications;
    
    private Attributes oldEntry;
    
    private Map injectors;
    
    public ModifyStoredProcedureParameterInjector( Invocation invocation, LdapDN modifiedEntryName, ModificationItemImpl[] modifications ) throws NamingException
    {
        super( invocation );
        init( modifiedEntryName, modifications );
    }
    
    public ModifyStoredProcedureParameterInjector( Invocation invocation, LdapDN modifiedEntryName, int modOp, Attributes modifications ) throws NamingException
    {
        super( invocation );
        ModificationItemImpl[] mods = new ModificationItemImpl[ modifications.size() ];
        NamingEnumeration modEnum = modifications.getAll();
        int i = 0;
        while ( modEnum.hasMoreElements() )
        {
            Attribute attribute = ( Attribute ) modEnum.nextElement();
            mods[ i++ ] = new ModificationItemImpl( modOp, attribute ); 
        }
        
        init( modifiedEntryName, mods );
    }
    
    private void init( LdapDN modifiedEntryName, ModificationItemImpl[] modifications ) throws NamingException
    {
        this.modifiedEntryName = modifiedEntryName;
        this.modifications = modifications;
        this.oldEntry = getEntry();
        injectors = super.getInjectors();
        injectors.put( StoredProcedureParameter.Modify_OBJECT.class, $objectInjector );
        injectors.put( StoredProcedureParameter.Modify_MODIFICATION.class, $modificationInjector );
        injectors.put( StoredProcedureParameter.Modify_OLD_ENTRY.class, $oldEntryInjector );
        injectors.put( StoredProcedureParameter.Modify_NEW_ENTRY.class, $newEntryInjector );
    }
    
    MicroInjector $objectInjector = new MicroInjector()
    {
        public Object inject( StoredProcedureParameter param ) throws NamingException
        {
            // Return a safe copy constructed with user provided name.
            return new LdapDN( modifiedEntryName.getUpName() );
        };
    };
    
    MicroInjector $modificationInjector = new MicroInjector()
    {
        public Object inject( StoredProcedureParameter param ) throws NamingException
        {
            return modifications.clone();
        };
    };
    
    MicroInjector $oldEntryInjector = new MicroInjector()
    {
        public Object inject( StoredProcedureParameter param ) throws NamingException
        {
            return oldEntry;
        };
    };
    
    MicroInjector $newEntryInjector = new MicroInjector()
    {
        public Object inject( StoredProcedureParameter param ) throws NamingException
        {
            return getEntry();
        };
    };
    
    private Attributes getEntry() throws NamingException
    {
        PartitionNexusProxy proxy = getInvocation().getProxy();
        /**
         * Using LOOKUP_EXCLUDING_OPR_ATTRS_BYPASS here to exclude operational attributes
         * especially subentry related ones like "triggerExecutionSubentries".
         */
        return proxy.lookup( modifiedEntryName, PartitionNexusProxy.LOOKUP_EXCLUDING_OPR_ATTRS_BYPASS );
    }

}
