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
 * This abstract class stores common context elements, like the DN, which is used
 * in all the contexts.
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 * @version $Rev$, $Date$
 */
public class AbstractOperationContext implements OperationContext
{
    /** The DN associated with the context */
    private LdapDN dn;
    
    /**
     * 
     * Creates a new instance of AbstractOperationContext.
     *
     */
    public AbstractOperationContext()
    {
    }

    /**
     * 
     * Creates a new instance of AbstractOperationContext.
     *
     * @param dn The associated DN
     */
    public AbstractOperationContext( LdapDN dn )
    {
        this.dn = dn;
    }

    /**
     * @return The associated DN
     */
    public LdapDN getDn()
    {
        return dn;
    }

    /**
     * Set the context DN
     *
     * @param dn The DN to set
     */
    public void setDn( LdapDN dn )
    {
        this.dn = dn;
    }
}
