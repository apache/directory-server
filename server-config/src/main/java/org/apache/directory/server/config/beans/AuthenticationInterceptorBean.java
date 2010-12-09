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

import java.util.ArrayList;
import java.util.List;

/**
 * A bean used to store the Zuthentictor interceptor condifuration
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 */
public class AuthenticationInterceptorBean extends InterceptorBean
{
    /** The list of authenticators */
    private List<AuthenticatorBean> authenticators = new ArrayList<AuthenticatorBean>();

    /**
     * Creates a new AuthenticationInterceptorBean instance
     */
    public AuthenticationInterceptorBean() 
    {
        super();
    }
    
    
    /**
     * @param authenticators the authenticators to set
     */
    public void setAuthenticators( List<AuthenticatorBean> authenticators )
    {
        this.authenticators = authenticators;
    }

    
    /**
     * @param authenticators the authenticators to add
     */
    public void addAuthenticators( AuthenticatorBean... authenticators )
    {
        for ( AuthenticatorBean authenticator : authenticators )
        {   
            this.authenticators.add( authenticator );
        }
    }
    

    /**
     * @return the extendedOps
     */
    public List<AuthenticatorBean> getAuthenticators()
    {
        return authenticators;
    }
    

    /**
     * {@inheritDoc}
     */
    public String toString( String tabs )
    {
        StringBuilder sb = new StringBuilder();
        
        sb.append( tabs ).append( "AuthenticationInterceptor :\n" );
        sb.append( super.toString( tabs + "  " ) );
        
        if ( ( authenticators != null ) && ( authenticators.size() > 0 ) )
        {
            sb.append( tabs ).append( "  authenticator :\n" );

            for ( AuthenticatorBean authenticator : authenticators )
            {
                sb.append( authenticator.toString( tabs + "    " ) );
            }
        }
        
        return sb.toString();
    }
}
