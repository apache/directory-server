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
public class SaslMechHandlerBean extends AdsBaseBean
{
    /** The SASL mechanism handler */
    private String saslmechname;
    
    /** The SASL mechanism handler FQCN */
    private String saslmechclassname;
    
    /** The NTLM provider */
    private String ntlmmechprovider;

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
        return saslmechname;
    }
    

    /**
     * @param saslMechName the SaslMechName to set
     */
    public void setSaslMechName( String saslMechName )
    {
        this.saslmechname = saslMechName;
    }

    
    /**
     * @return the SaslMechClassName
     */
    public String getSaslMechClassName()
    {
        return saslmechclassname;
    }

    
    /**
     * @param SaslMechClassName the SaslMechClassName to set
     */
    public void setSaslMechClassName( String saslMechClassName )
    {
        this.saslmechclassname = saslMechClassName;
    }

    
    /**
     * @return the NtlmMechProvider
     */
    public String getNtlmMechProvider()
    {
        return ntlmmechprovider;
    }

    
    /**
     * @param NtlmMechProvider the NtlmMechProvider to set
     */
    public void setNtlmMechProvider( String ntlmMechProvider )
    {
        this.ntlmmechprovider = ntlmMechProvider;
    }
    
    
    /**
     * {@inheritDoc}
     */
    public String toString( String tabs )
    {
        StringBuilder sb = new StringBuilder();
        
        sb.append( tabs ).append( "SASL mechanism handler :\n" );
        sb.append( tabs ).append( "  SASL mechanism name :" ).append( saslmechname ).append( '\n' );
        sb.append( tabs ).append( "  SASL mechanism class name :" ).append( saslmechclassname ).append( '\n' );
        sb.append( toString( tabs, "  NTLM mechanism provider", ntlmmechprovider ) );
        
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
