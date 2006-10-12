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

import org.apache.directory.mitosis.service.protocol.Constants;
import org.apache.directory.shared.ldap.util.EqualsBuilder;
import org.apache.directory.shared.ldap.util.HashCodeBuilder;

public class EndLogEntriesAckMessage extends ResponseMessage
{

    public EndLogEntriesAckMessage( int sequence, int responseCode )
    {
        super( sequence, responseCode );
    }

    public int getType()
    {
        return Constants.END_LOG_ENTRIES_ACK;
    }

    public boolean equals( Object object )
    {
        if( !( object instanceof EndLogEntriesAckMessage ) )
        {
            return false;
        }
        
        return new EqualsBuilder().appendSuper( super.equals( object ) )
                .isEquals();
    }

    public int hashCode()
    {
        return new HashCodeBuilder( 247639103, -470023671 ).appendSuper(
                super.hashCode() ).toHashCode();
    }

    public String toString()
    {
        return "[EndLogEntriesAck] " + super.toString();
    }
}
