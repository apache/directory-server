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
 * A class used to store the Delegating Authenticator configuration.
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 */
public class DelegatingAuthenticatorBean extends AuthenticatorBean
{
    /** The delegate host */
    @ConfigurationElement(attributeType = "ads-delegateHost")
    private String delegateHost;

    /** The delegate port */
    @ConfigurationElement(attributeType = "ads-delegatePort")
    private int delegatePort;

    /** Tells if we use SSL to connect */
    @ConfigurationElement(attributeType = "ads-delegateSsl", isOptional = true)
    private boolean delegateSsl;


    /**
     * @return the delegateHost
     */
    public String getDelegateHost()
    {
        return delegateHost;
    }


    /**
     * @param delegateHost the delegateHost to set
     */
    public void setDelegateHost( String delegateHost )
    {
        this.delegateHost = delegateHost;
    }


    /**
     * @return the delegatePort
     */
    public int getDelegatePort()
    {
        return delegatePort;
    }


    /**
     * @param delegatePort the delegatePort to set
     */
    public void setDelegatePort( int delegatePort )
    {
        this.delegatePort = delegatePort;
    }


    /**
     * {@inheritDoc}
     */
    public String toString( String tabs )
    {
        StringBuilder sb = new StringBuilder();

        sb.append( tabs ).append( "Delegating Authenticator :\n" );
        sb.append( super.toString( tabs + "  " ) );

        sb.append( tabs ).append( "  delegate host : " ).append( delegateHost ).append( '\n' );
        sb.append( tabs ).append( "  delegate port : " ).append( delegatePort ).append( '\n' );
        sb.append( tabs ).append( "  delegate base DN : " ).append( baseDn ).append( '\n' );
        sb.append( tabs ).append( "  delegate SSL : " ).append( delegateSsl ).append( '\n' );

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
