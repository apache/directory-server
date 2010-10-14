/*
 *   Licensed to the Apache Software Foundation (ASF) under one
 *   or more contributor license agreements.  See the NOTICE file
 *   distributed with this work for additional information
 *   regarding copyright ownership.  The ASF licenses this file
 *   to you under the Apache License, Version 2.0 (the
 *   "License"); you may not use this file except in compliance
 *   with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 *   Unless required by applicable law or agreed to in writing,
 *   software distributed under the License is distributed on an
 *   "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 *   KIND, either express or implied.  See the License for the
 *   specific language governing permissions and limitations
 *   under the License.
 *
 */
package org.apache.directory.server.config.beans;


/**
 * A class used to store the SASL mechanism handler configuration.
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 */
public class LdapServerSaslMechanisHandlerBean extends BaseAdsBean
{
    /** The SASL mechanism handler */
    private String ldapServerSaslMechName;
    
    /** The SASL mechanism handler FQCN */
    private String ldapServerSaslMechClassName;
    
    /** The NTLM provider */
    private String ldapServerNtlmMechProvider;

    /**
     * Create a new LdapServerSaslMechanisHandlerBean instance
     */
    public LdapServerSaslMechanisHandlerBean()
    {
        super();
    }

    /**
     * @return the ldapServerSaslMechName
     */
    public String getLdapServerSaslMechName()
    {
        return ldapServerSaslMechName;
    }
    

    /**
     * @param ldapServerSaslMechName the ldapServerSaslMechName to set
     */
    public void setLdapServerSaslMechName( String ldapServerSaslMechName )
    {
        this.ldapServerSaslMechName = ldapServerSaslMechName;
    }

    
    /**
     * @return the ldapServerSaslMechClassName
     */
    public String getLdapServerSaslMechClassName()
    {
        return ldapServerSaslMechClassName;
    }

    
    /**
     * @param ldapServerSaslMechClassName the ldapServerSaslMechClassName to set
     */
    public void setLdapServerSaslMechClassName( String ldapServerSaslMechClassName )
    {
        this.ldapServerSaslMechClassName = ldapServerSaslMechClassName;
    }

    
    /**
     * @return the ldapServerNtlmMechProvider
     */
    public String getLdapServerNtlmMechProvider()
    {
        return ldapServerNtlmMechProvider;
    }

    
    /**
     * @param ldapServerNtlmMechProvider the ldapServerNtlmMechProvider to set
     */
    public void setLdapServerNtlmMechProvider( String ldapServerNtlmMechProvider )
    {
        this.ldapServerNtlmMechProvider = ldapServerNtlmMechProvider;
    }
}
