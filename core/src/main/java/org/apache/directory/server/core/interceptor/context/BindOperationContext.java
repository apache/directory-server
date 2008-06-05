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
 

import org.apache.directory.server.core.CoreSession;
import org.apache.directory.shared.ldap.message.MessageTypeEnum;
import org.apache.directory.shared.ldap.util.StringTools;


/**
 * A Bind context used for Interceptors. It contains all the informations
 * needed for the bind operation, and used by all the interceptors
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 * @version $Rev$, $Date$
 */
public class BindOperationContext extends AbstractOperationContext
{
    /** The password */
    private byte[] credentials;

    /** The SASL mechanism */
    private String saslMechanism;
    
    /** The SASL identifier */
    private String saslAuthId;
    
    
    
    /**
     * Creates a new instance of BindOperationContext.
     */
    public BindOperationContext( CoreSession session )
    {
        super( session );
    }

    
    /**
     * @return the SASL mechanisms
     */
    public String getSaslMechanism()
    {
        return saslMechanism;
    }

    
    public void setSaslMechanism( String saslMechanism )
    {
        this.saslMechanism = saslMechanism;
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
    
    
    public boolean isSaslBind()
    {
        return saslMechanism != null;
    }
    
    
    /**
     * @return the operation name
     */
    public String getName()
    {
        return MessageTypeEnum.BIND_REQUEST.name();
    }


    /**
     * @see Object#toString()
     */
    public String toString()
    {
        return "BindContext for DN '" + getDn().getUpName() + "', credentials <" +
            ( credentials != null ? StringTools.dumpBytes( credentials ) : "" ) + ">" +
            ( saslMechanism != null ? ", saslMechanism : <" + saslMechanism + ">" : "" ) +
            ( saslAuthId != null ? ", saslAuthId <" + saslAuthId + ">" : "" );
    }
    
    
    public void setSession( CoreSession session )
    {
        super.setSession( session );
    }
}
