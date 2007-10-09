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
package org.apache.directory.shared.ldap.common;

/**
 * 
 * The ServerModification class is used to store attribute modifications as
 * they are sent by the modifyRequest.
 * 
 * There are three kind of modifications :
 * - Add
 * - Replace
 * - Remove
 * 
 * One should not instanciate this class directly.
 * 
 * Each of these modifications will be implemented as a subclass
 * of this one.
 * 
 * For operationnal attributes which are modified by the server, we use 
 * a flag which is set to <code>true</code>. 
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 * @version $Rev$, $Date$
 */
public class ServerModification
{
    /** A flag set for an attribute modified by the server */
    private boolean serverModified;
    
    /** The modified attribute */
    private ServerAttribute attribute;
    
    /**
     * Creates a new instance of ServerModification.
     */
    protected ServerModification()
    {
        serverModified = false;
    }


    /**
     * Creates a new instance of ServerModification.
     * 
     * @param attribute The modified attribute
     */
    protected ServerModification( ServerAttribute attribute )
    {
        this.attribute = attribute;
        serverModified = false;
    }

    
    /**
     * Creates a new instance of ServerModification.
     * 
     * @param attribute The modified attribute
     */
    protected ServerModification( String attributeId, Value<?> value )
    {
        this.attribute = new ServerAttributeImpl( attributeId, value );
        serverModified = false;
    }

    
    /**
     * Creates a new instance of ServerModification.
     * 
     * @param attribute The modified attribute
     * @param serverModified The attribute is modified by the server
     */
    protected ServerModification( ServerAttribute attribute, boolean serverModified )
    {
        this.attribute = attribute;
        this.serverModified = serverModified;
    }

    
    /**
     * Get the modified attribute
     *
     * @return The modified attribute
     */
    public ServerAttribute getAttribute()
    {
        return attribute;
    }

    
    /**
     * 
     * Set the modified attribute
     *
     * @param attribute The modified attribute
     */
    public void setAttribute( ServerAttribute attribute )
    {
        this.attribute = attribute;
    }

    
    /**
     * 
     * Tels if the attribute has been modified by the server
     *
     * @return
     */
    public boolean isServerModified()
    {
        return serverModified;
    }

    
    /**
     * Set the flag for a server modified attribute
     */
    public void setServerModified()
    {
        serverModified = true;
    }
    
    
    /**
     * @see Object#toString()
     */
    public String toString()
    {
        StringBuilder sb = new StringBuilder();
        
        if ( serverModified )
        {
            sb.append( "[S]" );
        }
        
        sb.append( " : " ).append( attribute );
        
        return sb.toString();
    }
}
