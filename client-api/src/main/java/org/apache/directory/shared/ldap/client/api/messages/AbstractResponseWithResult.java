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
package org.apache.directory.shared.ldap.client.api.messages;


/**
 * A request who's one or more responses contains an LdapResult.
 * 
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 * @version $Rev: 760984 $
 */
public abstract class AbstractResponseWithResult extends AbstractMessage implements ResponseWithResult
{
    /** The result */
    private LdapResult ldapResult;
    
    /**
     * Creates a new instance of AbstractResponseWithResult.
     */
    public AbstractResponseWithResult()
    {
        super();
    }

    
    /**
     * Returns the response's result
     * 
     * @return a result containing response with defaults and the messageId set
     * in response to this specific request
     */
    public LdapResult getLdapResult()
    {
        return ldapResult;
    }
    
    
    /**
     * Sets the result into the response.
     * 
     * @param ldapResult The LdapResult instance associated with this response
     */
    public void setLdapResult( LdapResult ldapResult )
    {
        this.ldapResult = ldapResult;
    }


    /**
     * Get a String representation of an Response
     * 
     * @return An Response String
     */
    public String toString()
    {
        StringBuilder sb = new StringBuilder();
        
        sb.append( super.toString() );
        
        if ( ldapResult != null )
        {
            sb.append( ldapResult );
        }
        
        return sb.toString();
    }
}
