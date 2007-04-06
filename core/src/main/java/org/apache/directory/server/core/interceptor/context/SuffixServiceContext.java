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
 * A Suffix context used for Interceptors. It contains all the informations
 * needed for the isSuffix operation, and used by all the interceptors
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 * @version $Rev$, $Date$
 */
public class SuffixServiceContext implements ServiceContext
{
    /** The principal DN */
    private LdapDN suffixDn;

    /**
     * Creates a new instance of SuffixServiceContext.
     */
    public SuffixServiceContext()
    {
    }
    
    /**
     * Creates a new instance of SuffixServiceContext.
     *
     * @param suffixDn The principal DN from which the suffix must be hecked
     */
    public SuffixServiceContext( LdapDN suffixDn )
    {
        this.suffixDn = suffixDn;
    }
    
    /**
     * @return The Suffix DN
     */
    public LdapDN getSuffixDn()
    {
        return suffixDn;
    }
    
    /**
     * Set the Suffix DN.
     *
     * @param unbindDn The Suffix DN
     */
    public void setSuffixDn( LdapDN suffixDn )
    {
        this.suffixDn = suffixDn;
    }
    
    /**
     * @see Object#toString()
     */
    public String toString()
    {
        return "SuffixContext for DN '" + suffixDn.getUpName() + "'";
    }
}
