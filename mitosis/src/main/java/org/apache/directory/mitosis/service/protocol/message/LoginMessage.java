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
package org.apache.directory.mitosis.service.protocol.message;

import org.apache.directory.shared.ldap.util.EqualsBuilder;
import org.apache.directory.shared.ldap.util.HashCodeBuilder;
import org.apache.directory.mitosis.common.ReplicaId;
import org.apache.directory.mitosis.service.protocol.Constants;

public class LoginMessage extends BaseMessage
{
    private final ReplicaId replicaId;

    public LoginMessage( int sequence, ReplicaId replicaId )
    {
        super( sequence );
        
        this.replicaId = replicaId;
    }
    
    public int getType()
    {
        return Constants.LOGIN;
    }
    
    public ReplicaId getReplicaId()
    {
        return replicaId;
    }

    /**
     * @see java.lang.Object#equals(Object)
     */
    public boolean equals(Object object)
    {
        if (!(object instanceof LoginMessage)) 
        {
            return false;
        }
        
        LoginMessage rhs = (LoginMessage) object;
        
        return new EqualsBuilder().appendSuper(super.equals(object)).append(
                this.replicaId, rhs.replicaId).isEquals();
    }

    /**
     * @see java.lang.Object#hashCode()
     */
    public int hashCode() 
    {
        return new HashCodeBuilder(1520317245, 1630850531).appendSuper(
                super.hashCode()).append(this.replicaId).toHashCode();
    }

    public String toString()
    {
        return "[Login] " + super.toString() + ", " + replicaId;
    }
}
