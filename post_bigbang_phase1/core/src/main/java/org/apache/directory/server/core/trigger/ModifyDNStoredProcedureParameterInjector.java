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

import org.apache.directory.server.core.invocation.Invocation;
import org.apache.directory.shared.ldap.name.LdapDN;
import org.apache.directory.shared.ldap.name.Rdn;
import org.apache.directory.shared.ldap.trigger.StoredProcedureParameter;

import javax.naming.NamingException;
import java.util.Map;


public class ModifyDNStoredProcedureParameterInjector extends AbstractStoredProcedureParameterInjector
{
    private boolean deleteOldRn;
    private LdapDN oldRDN;
    private Rdn newRDN;
    private LdapDN oldSuperiorDN;
    private LdapDN newSuperiorDN;
    private LdapDN oldDN;
    private LdapDN newDN;

	public ModifyDNStoredProcedureParameterInjector( Invocation invocation, boolean deleteOldRn,
        LdapDN oldRDN, Rdn newRDN, LdapDN oldSuperiorDN, LdapDN newSuperiorDN, LdapDN oldDN, LdapDN newDN)
    {
        super( invocation );
        this.deleteOldRn = deleteOldRn;
		this.oldRDN = oldRDN;
		this.newRDN = newRDN;
		this.oldSuperiorDN = oldSuperiorDN;
		this.newSuperiorDN = newSuperiorDN;
		this.oldDN = oldDN;
		this.newDN = newDN;
		
		Map<Class, MicroInjector> injectors = super.getInjectors();
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
        public Object inject( StoredProcedureParameter param ) throws NamingException
        {
            // Return a safe copy constructed with user provided name.
            return new LdapDN( oldDN.getUpName() );
        }
    };

    /**
     * Injector for 'newrdn' parameter of ModifyDNRequest as in RFC4511.
     */
    MicroInjector $newrdnInjector = new MicroInjector()
    {
        public Object inject( StoredProcedureParameter param ) throws NamingException
        {
            // Return a safe copy constructed with user provided name.
            return new LdapDN( newRDN.getUpName() );
        }
    };

    /**
     * Injector for 'newrdn' parameter of ModifyDNRequest as in RFC4511.
     */
    MicroInjector $deleteoldrdnInjector = new MicroInjector()
    {
        public Object inject( StoredProcedureParameter param ) throws NamingException
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
        public Object inject( StoredProcedureParameter param ) throws NamingException
        {
            // Return a safe copy constructed with user provided name.
            return new LdapDN( newSuperiorDN.getUpName() );
        }
    };
    
    /**
     * Extra injector for 'oldRDN' which can be derived from parameters specified for ModifyDNRequest as in RFC4511.
     */
    MicroInjector $oldRDNInjector = new MicroInjector()
    {
        public Object inject( StoredProcedureParameter param ) throws NamingException
        {
            // Return a safe copy constructed with user provided name.
            return new LdapDN( oldRDN.getUpName() );
        }
    };
    
    /**
     * Extra injector for 'oldRDN' which can be derived from parameters specified for ModifyDNRequest as in RFC4511.
     */
    MicroInjector $oldSuperiorDNInjector = new MicroInjector()
    {
        public Object inject( StoredProcedureParameter param ) throws NamingException
        {
            // Return a safe copy constructed with user provided name.
            return new LdapDN( oldSuperiorDN.getUpName() );
        }
    };
    
    /**
     * Extra injector for 'newDN' which can be derived from parameters specified for ModifyDNRequest as in RFC4511.
     */
    MicroInjector $newDNInjector = new MicroInjector()
    {
        public Object inject( StoredProcedureParameter param ) throws NamingException
        {
            // Return a safe copy constructed with user provided name.
            return new LdapDN( newDN.getUpName() );
        }
    };
    
}
