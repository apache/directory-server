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
 * A Move context used for Interceptors. It contains all the informations
 * needed for the modify DN operation, and used by all the interceptors
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 * @version $Rev$, $Date$
 */
public class MoveServiceContext extends ModifyDNServiceContext
{
    /** The parent DN */
    private LdapDN parent;
    
    /**
     * 
     * Creates a new instance of MoveServiceContext.
     *
     */
    public MoveServiceContext()
    {
    	super();
    }

    /**
     * 
     * Creates a new instance of MoveServiceContext.
     *
     */
    public MoveServiceContext( LdapDN oldDn, LdapDN parent, String newDn, boolean delOldDn )
    {
        super( oldDn, newDn, delOldDn );
        this.parent = parent;
    }

    /**
     *  @return The parent DN
     */
    public LdapDN getParent()
    {
        return parent;
    }

    /**
     * Set the parent DN
     *
     * @param parent The parent
     */
    public void setParent( LdapDN parent )
    {
        this.parent = parent;
    }

    /**
     * @see Object#toString()
     */
    public String toString()
    {
        return "ReplaceContext for old DN '" + getDn().getUpName() + "'" +
        ", parent '" + parent + "'";
    }

}
