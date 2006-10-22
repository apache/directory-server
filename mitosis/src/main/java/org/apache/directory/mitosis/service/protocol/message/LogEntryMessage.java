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

import org.apache.directory.mitosis.operation.Operation;
import org.apache.directory.mitosis.service.protocol.Constants;
import org.apache.directory.shared.ldap.util.EqualsBuilder;
import org.apache.directory.shared.ldap.util.HashCodeBuilder;

public class LogEntryMessage extends BaseMessage
{
    private final Operation operation;

    public LogEntryMessage( int sequence, Operation operation )
    {
        super(sequence);
        this.operation = operation;
    }
    
    public int getType()
    {
        return Constants.LOG_ENTRY;
    }
    
    public Operation getOperation()
    {
        return operation;
    }

    /**
     * @see java.lang.Object#equals(Object)
     */
    public boolean equals(Object object) 
    {
        if (object == this) {
            return true;
        }
    
        if (!(object instanceof LogEntryMessage)) 
        {
            return false;
        }
        
        LogEntryMessage rhs = (LogEntryMessage) object;
        
        return new EqualsBuilder().appendSuper(super.equals(object)).append(
                this.operation, rhs.operation).isEquals();
    }

    /**
     * @see java.lang.Object#hashCode()
     */
    public int hashCode() 
    {
        return new HashCodeBuilder(633013569, -1063609843).appendSuper(
                super.hashCode()).append(this.operation)
                .toHashCode();
    }
    
    public String toString()
    {
        return "[LogEntry] " + super.toString() + ", " + operation;
    }
}
