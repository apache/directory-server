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
package org.apache.directory.server.core.interceptor.context;

import org.apache.directory.shared.ldap.name.LdapDN;

/**
 * A ModifyDN context used for Interceptors. It contains all the informations
 * needed for the modify DN operation, and used by all the interceptors
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 * @version $Rev$, $Date$
 */
public class ModifyDNServiceContext extends AbstractServiceContext
{
    /** The new DN */
    private String newDn;

    /** The flag to remove the old DN Attribute  */
    private boolean delOldDn;
    
    /**
     * 
     * Creates a new instance of ModifyDNServiceContext.
     *
     */
    public ModifyDNServiceContext()
    {
    	super();
    }

    /**
     * 
     * Creates a new instance of ModifyDNServiceContext.
     *
     */
    public ModifyDNServiceContext( LdapDN oldDn, String newDn, boolean delOldDn )
    {
        super( oldDn );
        this.newDn = newDn;
        this.delOldDn = delOldDn;
    }

    /**
     * @return The delete old DN flag
     */
	public boolean getDelOldDn() 
	{
		return delOldDn;
	}

	/**
	 * Set the flag to delete the old DN
	 * @param delOldDn the flag to set
	 */
	public void setDelOldDn( boolean delOldDn ) 
	{
		this.delOldDn = delOldDn;
	}

	/**
	 * @return The new DN
	 */
	public String getNewDn() 
	{
		return newDn;
	}

	/**
	 * Set the new DN
	 * @param newDn The new Dn
	 */
	public void setNewDn( String newDn ) 
	{
		this.newDn = newDn;
	}

	/**
     * @see Object#toString()
     */
    public String toString()
    {
        return "ModifyDNContext for old DN '" + getDn().getUpName() + "'" +
        ", newDn '" + newDn + "'" +
        ( delOldDn ? ", delete old Dn" : "" ) ; 
    }
}
