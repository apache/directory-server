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
import org.apache.directory.shared.ldap.model.exception.LdapInvalidDnException;
import org.apache.directory.shared.ldap.model.name.Dn;
import org.apache.directory.shared.ldap.model.name.Rdn;
import org.apache.directory.shared.ldap.trigger.StoredProcedureParameter;


public class ModifyDNStoredProcedureParameterInjector extends AbstractStoredProcedureParameterInjector
{
    private boolean deleteOldRn;
    private Rdn oldRdn;
    private Rdn newRdn;
    private Dn oldSuperiorDn;
    private Dn newSuperiorDn;
    private Dn oldDn;
    private Dn newDn;


    public ModifyDNStoredProcedureParameterInjector( OperationContext opContext, boolean deleteOldRn,
        Rdn oldRDN, Rdn newRdn, Dn oldSuperiorDn, Dn newSuperiorDn, Dn oldDn, Dn newDn)
    {
        super( opContext );
        this.deleteOldRn = deleteOldRn;
        this.oldRdn = (Rdn)oldRDN.clone();
        this.newRdn = (Rdn) newRdn.clone();
        this.oldSuperiorDn = oldSuperiorDn;
        this.newSuperiorDn = newSuperiorDn;
        this.oldDn = oldDn;
        this.newDn = newDn;
        
        Map<Class<?>, MicroInjector> injectors = super.getInjectors();
        injectors.put( StoredProcedureParameter.ModifyDN_ENTRY.class, $entryInjector );
        injectors.put( StoredProcedureParameter.ModifyDN_NEW_RDN.class, $newrdnInjector );
        injectors.put( StoredProcedureParameter.ModifyDN_DELETE_OLD_RDN.class, $deleteoldrdnInjector );
        injectors.put( StoredProcedureParameter.ModifyDN_NEW_SUPERIOR.class, $newSuperiorInjector );
        injectors.put( StoredProcedureParameter.ModifyDN_OLD_RDN.class, $oldRDNInjector );
        injectors.put( StoredProcedureParameter.ModifyDN_OLD_SUPERIOR_DN.class, $oldSuperiorDNInjector );
        injectors.put( StoredProcedureParameter.ModifyDN_NEW_DN.class, $newDNInjector );
        
    }
    /**
     * Injector for 'entry' parameter of ModifyDNRequest as in RFC4511.
     */
    MicroInjector $entryInjector = new MicroInjector()
    {
        public Object inject( OperationContext opContext, StoredProcedureParameter param ) throws LdapInvalidDnException
        {
            // Return a safe copy constructed with user provided name.
            return opContext.getSession().getDirectoryService().getDnFactory().create( oldDn.getName() );
        }
    };

    /**
     * Injector for 'newrdn' parameter of ModifyDNRequest as in RFC4511.
     */
    MicroInjector $newrdnInjector = new MicroInjector()
    {
        public Object inject( OperationContext opContext, StoredProcedureParameter param ) throws LdapInvalidDnException
        {
            // Return a safe copy constructed with user provided name.
            return opContext.getSession().getDirectoryService().getDnFactory().create( newRdn.getName() );
        }
    };

    /**
     * Injector for 'newrdn' parameter of ModifyDNRequest as in RFC4511.
     */
    MicroInjector $deleteoldrdnInjector = new MicroInjector()
    {
        public Object inject( OperationContext opContext, StoredProcedureParameter param ) throws LdapInvalidDnException
        {
            // Return a safe copy constructed with user provided name.
            return deleteOldRn;
        }
    };

    /**
     * Injector for 'newSuperior' parameter of ModifyDNRequest as in RFC4511.
     */
    MicroInjector $newSuperiorInjector = new MicroInjector()
    {
        public Object inject( OperationContext opContext, StoredProcedureParameter param ) throws LdapInvalidDnException
        {
            // Return a safe copy constructed with user provided name.
            return opContext.getSession().getDirectoryService().getDnFactory().create( newSuperiorDn.getName() );
        }
    };
    
    /**
     * Extra injector for 'oldRdn' which can be derived from parameters specified for ModifyDNRequest as in RFC4511.
     */
    MicroInjector $oldRDNInjector = new MicroInjector()
    {
        public Object inject( OperationContext opContext, StoredProcedureParameter param ) throws LdapInvalidDnException
        {
            // Return a safe copy constructed with user provided name.
            return opContext.getSession().getDirectoryService().getDnFactory().create( oldRdn.getName() );
        }
    };
    
    /**
     * Extra injector for 'oldRdn' which can be derived from parameters specified for ModifyDNRequest as in RFC4511.
     */
    MicroInjector $oldSuperiorDNInjector = new MicroInjector()
    {
        public Object inject( OperationContext opContext, StoredProcedureParameter param ) throws LdapInvalidDnException
        {
            // Return a safe copy constructed with user provided name.
            return opContext.getSession().getDirectoryService().getDnFactory().create( oldSuperiorDn.getName() );
        }
    };
    
    /**
     * Extra injector for 'newDn' which can be derived from parameters specified for ModifyDNRequest as in RFC4511.
     */
    MicroInjector $newDNInjector = new MicroInjector()
    {
        public Object inject( OperationContext opContext, StoredProcedureParameter param ) throws LdapInvalidDnException
        {
            // Return a safe copy constructed with user provided name.
            return opContext.getSession().getDirectoryService().getDnFactory().create( newDn.getName() );
        }
    };
    
}
