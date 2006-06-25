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

import org.apache.directory.server.core.invocation.Invocation;
import org.apache.directory.server.core.partition.DirectoryPartitionNexusProxy;
import org.apache.directory.shared.ldap.name.LdapDN;
import org.apache.directory.shared.ldap.trigger.StoredProcedureParameter.ModDNStoredProcedureParameter;


public class ModDNStoredProcedureParameterInjector extends AbstractStoredProcedureParameterInjector
{
    private LdapDN oldName;
    private String newRn;
    private boolean deleteOldRn;
    
    private Map injectors;
    
    public ModDNStoredProcedureParameterInjector( Invocation invocation, LdapDN oldName, String newRn, boolean deleteOldRn ) throws NamingException
    {
        super( invocation );
        init( oldName, newRn, deleteOldRn );
    }
    
    private void init( LdapDN oldName, String newRn, boolean deleteOldRn ) throws NamingException
    {
        this.oldName = oldName;
        this.newRn = newRn;
        this.deleteOldRn = deleteOldRn;
        injectors = super.getInjectors();
        /*
        injectors.put( ModDNStoredProcedureParameter.ENTRY, $entryInjector.inject() );
        injectors.put( ModDNStoredProcedureParameter.NEW_RDN, $newRdnInjector.inject() );
        injectors.put( ModDNStoredProcedureParameter.NEW_SUPERIOR, $newSuperior.inject() );
        injectors.put( ModDNStoredProcedureParameter.DELETE_OLD_RDN, $deleteOldRdnInjector.inject() );
        */
    }

}
