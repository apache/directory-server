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

import java.util.List;

import org.apache.directory.shared.ldap.name.LdapDN;

/**
 * A Bind context used for Interceptors. It contains all the informations
 * needed for the bind operation, and used by all the interceptors
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 * @version $Rev$, $Date$
 */
public class BindServiceContext implements ServiceContext
{
    /** The principal DN */
    private LdapDN bindDn;

    /** The list of supported mechanisms */
    private List<String> mechanisms;
    
    /** The password */
    private byte[] credentials;
    
    /** The SASL identifier */
    private String saslAuthId;
    
    /**
     * @return The Principal's DN
     */
    public LdapDN getBindDn()
    {
        return bindDn;
    }
    
    /**
     * Set the principal's DN.
     *
     * @param bindDn The principal's DN
     */
    public void setBindDn( LdapDN bindDn )
    {
        this.bindDn = bindDn;
    }

    /**
     * @return The list of supported mechanisms
     */
    public List<String> getMechanisms()
    {
        return mechanisms;
    }

    public void setMechanisms( List<String> mechanisms )
    {
        this.mechanisms = mechanisms;
    }

    /**
     * @return The principal password
     */
    public byte[] getCredentials()
    {
        return credentials;
    }

    public void setCredentials( byte[] credentials )
    {
        this.credentials = credentials;
    }

    /**
     * @return The SASL authentication ID
     */
    public String getSaslAuthId()
    {
        return saslAuthId;
    }

    public void setSaslAuthId( String saslAuthId )
    {
        this.saslAuthId = saslAuthId;
    }
}
