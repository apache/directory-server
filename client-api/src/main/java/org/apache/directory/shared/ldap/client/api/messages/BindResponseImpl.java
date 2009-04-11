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

import org.apache.directory.shared.ldap.util.StringTools;


/**
 * A client implementation of the client BindResponse LDAP message.
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 * @version $Rev$, $Date$
 */
public class BindResponseImpl extends AbstractResponseWithResult implements BindResponse
{
    /** optional property holding SASL authentication response parameters */
    private byte[] credentials;

    
    /**
     * Creates a new instance of BindResponseImpl.
     */
    public BindResponseImpl()
    {
        super();
    }
    

    /**
     * {@inheritDoc}
     */
    public byte[] getServerSaslCreds()
    {
        return credentials;
    }

    
    /**
     * {@inheritDoc}
     */
    public void setServerSaslCreds( byte[] credentials )
    {
        this.credentials = credentials;
    }


    /**
     * Get a String representation of a BindResponse
     * 
     * @return A BindResponse String
     */
    public String toString()
    {
        StringBuffer sb = new StringBuffer();

        sb.append( super.toString() );
        sb.append( "    BindResponse\n" );

        if ( credentials != null )
        {
            sb.append( "        Server sasl credentials : '" ).
                append( StringTools.utf8ToString( credentials ) ).
                append( "'\n" );
        }

        return sb.toString();
    }
}
