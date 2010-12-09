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


import org.apache.directory.server.config.ConfigurationElement;


/**
 * A class used to store the SASL mechanism handler configuration.
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 */
public class SaslMechHandlerBean extends AdsBaseBean
{
    /** The SASL mechanism handler */
    @ConfigurationElement(attributeType = "ads-saslMechName", isRdn = true)
    private String saslMechName;

    /** The SASL mechanism handler FQCN */
    @ConfigurationElement(attributeType = "ads-saslMechClassName")
    private String saslMechClassName;

    /** The NTLM provider */
    @ConfigurationElement(attributeType = "ads-ntlmMechProvider")
    private String ntlmMechProvider;


    /**
     * Create a new LdapServerSaslMechanisHandlerBean instance
     */
    public SaslMechHandlerBean()
    {
        super();
    }


    /**
     * @return the ldapServerSaslMechName
     */
    public String getSaslMechName()
    {
        return saslMechName;
    }


    /**
     * @param saslMechName the SaslMechName to set
     */
    public void setSaslMechName( String saslMechName )
    {
        this.saslMechName = saslMechName;
    }


    /**
     * @return the SaslMechClassName
     */
    public String getSaslMechClassName()
    {
        return saslMechClassName;
    }


    /**
     * @param SaslMechClassName the SaslMechClassName to set
     */
    public void setSaslMechClassName( String saslMechClassName )
    {
        this.saslMechClassName = saslMechClassName;
    }


    /**
     * @return the NtlmMechProvider
     */
    public String getNtlmMechProvider()
    {
        return ntlmMechProvider;
    }


    /**
     * @param NtlmMechProvider the NtlmMechProvider to set
     */
    public void setNtlmMechProvider( String ntlmMechProvider )
    {
        this.ntlmMechProvider = ntlmMechProvider;
    }


    /**
     * {@inheritDoc}
     */
    public String toString( String tabs )
    {
        StringBuilder sb = new StringBuilder();

        sb.append( tabs ).append( "SASL mechanism handler :\n" );
        sb.append( tabs ).append( "  SASL mechanism name :" ).append( saslMechName ).append( '\n' );
        sb.append( tabs ).append( "  SASL mechanism class name :" ).append( saslMechClassName ).append( '\n' );
        sb.append( toString( tabs, "  NTLM mechanism provider", ntlmMechProvider ) );

        return sb.toString();
    }


    /**
     * {@inheritDoc}
     */
    public String toString()
    {
        return toString( "" );
    }
}
