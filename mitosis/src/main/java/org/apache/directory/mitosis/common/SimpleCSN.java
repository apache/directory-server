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
package org.apache.directory.mitosis.common;

import java.io.Serializable;

import org.apache.directory.mitosis.util.OctetString;

/**
 * Basic implementation of {@link CSN}.
 * 
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 */
public class SimpleCSN implements CSN, Serializable, Comparable
{
    /**
     * Declares the Serial Version Uid.
     *
     * @see <a
     *      href="http://c2.com/cgi/wiki?AlwaysDeclareSerialVersionUid">Always
     *      Declare Serial Version Uid</a>
     */
    private static final long serialVersionUID = 1L;
    
    /** The timeStamp of this operation */
    private final long timestamp;

    /** The server identification */
    private final ReplicaId replicaId;
    
    /** The operation number in the same timestamp */
    private final int operationSequence;
    
    /** Stores the String representation of the CSN */
    private transient String octetString;

    /** Stores the byte array representation of the CSN */
    private transient byte[] bytes;

    /**
     * Creates a new instance.
     * 
     * @param timestamp GMT timestamp of modification
     * @param replicaId Replica ID where modification occurred (<tt>[-_A-Za-z0-9]{1,16}</tt>)
     * @param operationSequence Operation sequence
     */
    public SimpleCSN( long timestamp, ReplicaId replicaId, int operationSequence )
    {
        this.timestamp = timestamp;
        this.replicaId = replicaId;
        this.operationSequence = operationSequence;
    }
    
    /**
     * Creates a new instance of SimpleCSN from a String.
     * 
     * The string format must be :
     * &lt;timestamp> : &lt;replica ID> : &lt;operation sequence>
     *
     * @param value The String containing the CSN
     */
    public SimpleCSN( String value ) throws InvalidCSNException
    {
        assert value != null;
        
        int sepTS = value.indexOf( ':' );
        
        assert sepTS > 0;
        
        int sepID = value.lastIndexOf( ':' );
        
        if ( ( sepID == -1 ) || ( sepID == sepTS ) | ( sepID - sepTS < 2 ) )
        {
            throw new InvalidCSNException();
        }
        
        try
        {
            timestamp = Long.parseLong( value.substring( 0, sepTS ), 16 );
        }
        catch ( NumberFormatException ife )
        {
            throw new InvalidCSNException();
        }
        
        try
        {
            replicaId = new ReplicaId( value.substring(  sepTS + 1, sepID ) );
        }
        catch ( IllegalArgumentException iae )
        {
            throw new InvalidCSNException();
        }

        try
        {
            operationSequence = Integer.parseInt( value.substring( sepID + 1 ), 16 );
        }
        catch ( NumberFormatException ife )
        {
            throw new InvalidCSNException();
        }
    }

    /**
     * Creates a new instance of SimpleCSN from the serialized data
     *
     * @param value The byte array which contains the serialized CSN
     */
    public SimpleCSN( byte[] value )
    {
        timestamp =  ((long)(value[0] & 0x00FF) << 56) |
                ((long)(value[1] & 0x00FF) << 48) |
                ((long)(value[2] & 0x00FF) << 40) |
                ((long)(value[3] & 0x00FF) << 32) |
                ((value[4] << 24) & 0x00000000FF000000L) |
                ((value[5] << 16) & 0x0000000000FF0000L) |
                ((value[6] << 8) & 0x000000000000FF00L) |
                (value[7] & 0x00000000000000FFL);
        
        operationSequence = ((value[8] & 0x00FF) << 24) +
            ((value[9] & 0x00FF) << 16) +
            ((value[10] & 0x00FF) << 8) +
            (value[11] & 0x00FF);
        
        char[] chars = new char[value.length - 12];
                                
        for ( int i = 12; i < value.length; i++ )
        {
            chars[i - 12] = (char)(value[i] & 0x00FF);
        }
        
        replicaId = new ReplicaId( new String( chars ) );
        bytes = value;
    }

    /**
     * Return the CSN as a formated string. The used format is :
     * &lt;timestamp> ':' &lt;replicaId> ':' &lt;operation sequence>
     * 
     * @return The CSN as a String
     */
    public String toOctetString()
    {
        if( octetString == null )
        {
            StringBuffer buf = new StringBuffer( 40 );
            OctetString.append( buf, timestamp );
            buf.append( ':' );
            buf.append( replicaId );
            buf.append( ':' );
            OctetString.append( buf, operationSequence );
            octetString = buf.toString();
        }

        return octetString;
    }
    
    /**
     * Get the CSN as a byte array. The data are stored as :
     * bytes 1 to 8  : timestamp, big-endian
     * bytes 9 to 12 : operation sequence, big-endian
     * bytes 13 to ... : ReplicaId 
     * 
     * @return A byte array representing theCSN
     */
    public byte[] toBytes()
    {
        if ( bytes == null )
        {
            String id = replicaId.getId();
            byte[] bb = new byte[8 + id.length() + 4];
            
            bb[0] = (byte)(timestamp >> 56 );
            bb[1] = (byte)(timestamp >> 48 );
            bb[2] = (byte)(timestamp >> 40 );
            bb[3] = (byte)(timestamp >> 32 );
            bb[4] = (byte)(timestamp >> 24 );
            bb[5] = (byte)(timestamp >> 16 );
            bb[6] = (byte)(timestamp >> 8 );
            bb[7] = (byte)timestamp;
            bb[8] = (byte)((operationSequence >> 24 ) );
            bb[9] = (byte)((operationSequence >> 16 ) );
            bb[10] = (byte)((operationSequence >> 8 ) );
            bb[11] = (byte)(operationSequence );
            
            for ( int i = 0; i < id.length(); i++ )
            {
                bb[12+i] =(byte)id.charAt( i );
            }
            
            bytes = bb;
        }
        
        return bytes;
    }
    
    /**
     * @return The timestamp
     */
    public long getTimestamp()
    {
        return timestamp;
    }
    
    /**
     * @return The replicaId
     */
    public ReplicaId getReplicaId()
    {
        return replicaId;
    }

    /**
     * @return The operation sequence
     */
    public int getOperationSequence()
    {
        return operationSequence;
    }

    /**
     * @return The CSN as a String
     */
    public String toString()
    {
        return toOctetString();
    }
    
    /**
     * Returns a hash code value for the object.
     * 
     * @return a hash code value for this object.
     */
    public int hashCode()
    {
        return replicaId.hashCode() ^ ( int ) timestamp ^ operationSequence;
    }

    /**
     * Indicates whether some other object is "equal to" this one
     * 
     * @param o the reference object with which to compare.
     * @return <code>true</code> if this object is the same as the obj argument; 
     * <code>false</code> otherwise.
     */
    public boolean equals( Object o )
    {
        if( o == null )
        {
            return false;
        }
        
        if( this == o )
        {
            return true;
        }
        
        if( !( o instanceof CSN ) )
        {
            return false;
        }
        
        CSN that = ( CSN ) o;
        
        return timestamp == that.getTimestamp() &&
               replicaId.equals( that.getReplicaId() ) &&
               operationSequence == that.getOperationSequence();
    }
    
    /**
     * Compares this object with the specified object for order.  Returns a
     * negative integer, zero, or a positive integer as this object is less
     * than, equal to, or greater than the specified object.<p>
     * 
     * @param   o the Object to be compared.
     * @return  a negative integer, zero, or a positive integer as this object
     *      is less than, equal to, or greater than the specified object.
     */
    public int compareTo( Object o )
    {
        CSN that = ( CSN ) o;
        long thatTimestamp = that.getTimestamp();

        if( this.timestamp < thatTimestamp )
        {
            return -1;
        }
        else if( this.timestamp > thatTimestamp )
        {
            return 1;
        }
        
        int replicaIdCompareResult = this.replicaId.compareTo( that.getReplicaId() );
        
        if( replicaIdCompareResult != 0 )
        {
            return replicaIdCompareResult;
        }
        
        int thatSequence = that.getOperationSequence();

        if( this.operationSequence < thatSequence )
        {
            return -1;
        }
        else if( this.operationSequence > thatSequence )
        {
            return 1;
        }
        else
        {
            return 0;
        }
    }
}
