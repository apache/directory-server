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
import org.apache.directory.mitosis.common.CSNVector;
import org.apache.directory.mitosis.service.protocol.Constants;


public class BeginLogEntriesAckMessage extends ResponseMessage
{
    private final CSNVector purgeVector;
    private final CSNVector updateVector;


    public BeginLogEntriesAckMessage( int sequence, int responseCode, CSNVector purgeVector, CSNVector updateVector )
    {
        super( sequence, responseCode );

        if ( responseCode == Constants.OK )
        {
            assert purgeVector != null;
            assert updateVector != null;

            this.purgeVector = purgeVector;
            this.updateVector = updateVector;
        }
        else
        {
            this.purgeVector = null;
            this.updateVector = null;
        }
    }


    public int getType()
    {
        return Constants.GET_UPDATE_VECTOR_ACK;
    }


    public CSNVector getPurgeVector()
    {
        return purgeVector;
    }


    public CSNVector getUpdateVector()
    {
        return updateVector;
    }


    /**
     * @see java.lang.Object#equals(Object)
     */
    public boolean equals( Object object )
    {
        if ( object == this )
        {
            return true;
        }

        if ( !( object instanceof BeginLogEntriesAckMessage ) )
        {
            return false;
        }

        BeginLogEntriesAckMessage rhs = ( BeginLogEntriesAckMessage ) object;

        return new EqualsBuilder().appendSuper( super.equals( object ) ).append( this.purgeVector, rhs.purgeVector )
            .append( this.updateVector, rhs.updateVector ).isEquals();
    }


    /**
     * @see java.lang.Object#hashCode()
     */
    public int hashCode()
    {
        return new HashCodeBuilder( 537917217, 1652875233 ).appendSuper( super.hashCode() ).append( this.purgeVector )
            .append( this.updateVector ).toHashCode();
    }


    public String toString()
    {
        return "[BeginLogEntriesAck] " + super.toString() + ", PV: " + purgeVector + ", UV: " + updateVector;
    }
}
