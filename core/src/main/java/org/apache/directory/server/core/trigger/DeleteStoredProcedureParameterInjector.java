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

import javax.naming.NamingException;
import javax.naming.directory.Attributes;

import org.apache.directory.server.core.invocation.Invocation;
import org.apache.directory.server.core.partition.DirectoryPartitionNexusProxy;
import org.apache.directory.shared.ldap.name.LdapDN;
import org.apache.directory.shared.ldap.trigger.StoredProcedureParameter.DeleteStoredProcedureParameter;


public class DeleteStoredProcedureParameterInjector extends AbstractStoredProcedureParameterInjector
{
    private LdapDN deletedEntryName;
    
    private Map injectors;
    
    public DeleteStoredProcedureParameterInjector( Invocation invocation, LdapDN deletedEntryName ) throws NamingException
    {
        super( invocation );
        this.deletedEntryName = deletedEntryName;
        injectors = super.getInjectors();
        injectors.put( DeleteStoredProcedureParameter.NAME, $nameInjector.inject() );
        injectors.put( DeleteStoredProcedureParameter.DELETED_ENTRY, $deletedEntryInjector.inject() );
    }
    
    MicroInjector $nameInjector = new MicroInjector()
    {
        public Object inject() throws NamingException
        {
            return deletedEntryName; // LdapDN is still a Name
        };
    };
    
    MicroInjector $deletedEntryInjector = new MicroInjector()
    {
        public Object inject() throws NamingException
        {
            DirectoryPartitionNexusProxy proxy = getInvocation().getProxy();
            Attributes deletedEntry = proxy.lookup( deletedEntryName, DirectoryPartitionNexusProxy.LOOKUP_BYPASS );
            return deletedEntry;
        };
    };

}
