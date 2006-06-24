/*
 *   Copyright 2006 The Apache Software Foundation
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

package org.apache.directory.server.core.trigger;

import java.util.Map;

import javax.naming.NamingEnumeration;
import javax.naming.NamingException;
import javax.naming.directory.Attribute;
import javax.naming.directory.Attributes;
import javax.naming.directory.DirContext;
import javax.naming.directory.ModificationItem;

import org.apache.directory.server.core.invocation.Invocation;
import org.apache.directory.server.core.partition.DirectoryPartitionNexusProxy;
import org.apache.directory.shared.ldap.name.LdapDN;
import org.apache.directory.shared.ldap.trigger.StoredProcedureParameter.ModifyStoredProcedureParameter;

public class ModifyStoredProcedureParameterInjector extends AbstractStoredProcedureParameterInjector
{
    private LdapDN modifiedEntryName;
    private ModificationItem[] modifications;
    
    private Attributes oldEntry;
    
    private Map injectors;
    
    public ModifyStoredProcedureParameterInjector( Invocation invocation, LdapDN modifiedEntryName, ModificationItem[] modifications ) throws NamingException
    {
        super( invocation );
        init( modifiedEntryName, modifications );
    }
    
    public ModifyStoredProcedureParameterInjector( Invocation invocation, LdapDN modifiedEntryName, int modOp, Attributes modifications ) throws NamingException
    {
        super( invocation );
        ModificationItem[] mods = new ModificationItem[ modifications.size() ];
        NamingEnumeration modEnum = modifications.getAll();
        int i = 0;
        while ( modEnum.hasMoreElements() )
        {
            Attribute attribute = ( Attribute ) modEnum.nextElement();
            mods[ i++ ] = new ModificationItem( modOp, attribute ); 
        }
        
        init( modifiedEntryName, mods );
    }
    
    private void init( LdapDN modifiedEntryName, ModificationItem[] modifications ) throws NamingException
    {
        this.modifiedEntryName = modifiedEntryName;
        this.modifications = modifications;
        injectors = super.getInjectors();
        injectors.put( ModifyStoredProcedureParameter.OBJECT, $objectInjector.inject() );
        injectors.put( ModifyStoredProcedureParameter.MODIFICATION, $modificationInjector.inject() );
        injectors.put( ModifyStoredProcedureParameter.OLD_ENTRY, $oldEntryInjector.inject() );
        injectors.put( ModifyStoredProcedureParameter.NEW_ENTRY, $newEntryInjector.inject() );
    }
    
    MicroInjector $objectInjector = new MicroInjector()
    {
        public Object inject() throws NamingException
        {
            // Return a safe copy constructed with user provided name.
            return new LdapDN( modifiedEntryName.toUpName() );
        };
    };
    
    MicroInjector $modificationInjector = new MicroInjector()
    {
        public Object inject() throws NamingException
        {
            return modifications.clone();
        };
    };
    
    MicroInjector $oldEntryInjector = new MicroInjector()
    {
        public Object inject() throws NamingException
        {
            DirectoryPartitionNexusProxy proxy = getInvocation().getProxy();
            /**
             * Using LOOKUP_EXCLUDING_OPR_ATTRS_BYPASS here to exclude operational attributes
             * especially subentry related ones like "triggerSubentries".
             */
            oldEntry = proxy.lookup( modifiedEntryName, DirectoryPartitionNexusProxy.LOOKUP_EXCLUDING_OPR_ATTRS_BYPASS );
            return oldEntry;
        };
    };
    
    MicroInjector $newEntryInjector = new MicroInjector()
    {
        public Object inject() throws NamingException
        {
            Attributes newEntry = ( Attributes ) oldEntry.clone();
            
            for ( int i = 0; i < modifications.length; i++ )
            {
                switch ( modifications[i].getModificationOp() )
                {
                    case ( DirContext.ADD_ATTRIBUTE  ):
                        newEntry.put( modifications[i].getAttribute() );
                        break;
                    case ( DirContext.REMOVE_ATTRIBUTE  ):
                        newEntry.remove( modifications[i].getAttribute().getID() );
                        break;
                    case ( DirContext.REPLACE_ATTRIBUTE  ):
                        newEntry.remove( modifications[i].getAttribute().getID() );
                        newEntry.put( modifications[i].getAttribute() );
                        break;
                }
            }
            
            return newEntry;
        };
    };

}
