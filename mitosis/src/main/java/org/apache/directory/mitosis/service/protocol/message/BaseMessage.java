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

public abstract class BaseMessage {

    private final int sequence;

    protected BaseMessage( int sequence )
    {
        this.sequence = sequence;
    }
    
    public abstract int getType();

    public int getSequence()
    {
        return sequence;
    }

    public boolean equals(Object object) 
    {
        if (!(object instanceof BaseMessage)) 
        {
            return false;
        }
        
        BaseMessage rhs = (BaseMessage) object;
        
        return new EqualsBuilder().append(
                this.sequence, rhs.sequence).isEquals();
    }

    public int hashCode() 
    {
        return new HashCodeBuilder(-1364566505, -1158072471).append(
                this.sequence).toHashCode();
    }
    
    public String toString()
    {
        return String.valueOf( sequence );
    }
}
