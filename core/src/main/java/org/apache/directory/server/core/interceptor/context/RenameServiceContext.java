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
 * A RenameService context used for Interceptors. It contains all the informations
 * needed for the modify DN operation, and used by all the interceptors
 * 
 * This is used whne the modifyDN is about changing the RDN, not the base DN.
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 * @version $Rev$, $Date$
 */
public class RenameServiceContext extends AbstractServiceContext
{
    /** The new DN */
    private String newRdn;

    /** The flag to remove the old DN Attribute  */
    private boolean delOldDn;
    
    /**
     * 
     * Creates a new instance of RenameServiceContext.
     *
     */
    public RenameServiceContext()
    {
    	super();
    }

    /**
     * 
     * Creates a new instance of RenameServiceContext.
     *
     */
    public RenameServiceContext( LdapDN oldDn, String newRdn, boolean delOldDn )
    {
        super( oldDn );
        this.newRdn = newRdn;
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
	 * @return The new RDN
	 */
	public String getNewRdn() 
	{
		return newRdn;
	}

	/**
	 * Set the new RDN
	 * @param newDn The new RDN
	 */
	public void setNewRdn( String newRdn ) 
	{
		this.newRdn = newRdn;
	}

	/**
     * @see Object#toString()
     */
    public String toString()
    {
        return "RenameContext for old DN '" + getDn().getUpName() + "'" +
        ", new RDN '" + newRdn + "'" +
        ( delOldDn ? ", delete old Dn" : "" ) ; 
    }
}
