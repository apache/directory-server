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
 * An interface for Intermediate responses.
 * 
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 */
public class IntermediateResponseImpl extends AbstractMessage implements IntermediateResponse
{
    /** The Response OID */
    private String responseName;

    /** The response value */
    private byte[] responseValue;
    
    /**
     * Creates a new instance of IntermediateResponseImpl.
     */
    public IntermediateResponseImpl()
    {
        super();
    }

    
    /**
     * {@inheritDoc}
     */
    public String getResponseName()
    {
        return responseName;
    }
    
    
    /**
     * {@inheritDoc}
     */
    public void setResponseName( String responseName )
    {
        this.responseName = responseName;
    }
    
    
    /**
     * {@inheritDoc}
     */
    public byte[] getResponseValue()
    {
        return responseValue;
    }

    
    /**
     * {@inheritDoc}
     */
    public void setResponseValue( byte[] responseValue )
    {
        this.responseValue = responseValue;
    }
}
