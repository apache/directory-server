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
package org.apache.directory.server.kerberos.shared.messages.value.flags;

import org.apache.directory.shared.asn1.primitives.BitString;

/**
 * An implementation of a BitString for any KerberosFlags. The different values
 * are stored in an int, as there can't be more than 32 flags (TicketFlag).
 * 
 * Some basic operations are implemented in this abstract class, like those
 * manipulating flags.
 * 
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 */
public abstract class AbstractKerberosFlags extends BitString implements KerberosFlags
{
    /**
     * The maximum size of the BitString as specified for Kerberos flags.
     * 1 byte contains the number of unused bits
     * 4 bytes containing the data
     * => 5 x 8 bits = 40 
     */
    public static final int MAX_SIZE = 40;

    /** The associated value */
    protected int value;
    
    
    /**
     * Standard constructor, which create a BitString containing 8 + 32 bits
     */
    public AbstractKerberosFlags()
    {
        super( MAX_SIZE );
    }

    
    /**
     * Standard constructor, taking a byte array, 8 + x (x <= 32) bits
     */
    public AbstractKerberosFlags( byte[] flags )
    {
        super( flags );
        // Remember getBytes() "A first byte containing the number of unused bits is added"
        value = ( ( getBytes()[1] & 0x00F ) << 24 ) | ( ( getBytes()[2] & 0x00FF ) << 16 ) | ( ( getBytes()[3] & 0x00FF ) << 8 ) | ( 0x00FF & getBytes()[4] ); 
    }
    
    
    /**
     * A static method to get the byte array representation of an int
     * @return The byte array for a list of flags.
     */
    public static byte[] getBytes( int flags )
    {
        return new byte[]{
            (byte)( 0 ), // unused bits
            (byte)( flags >>> 24), 
            (byte)( ( flags >> 16 ) & 0x00ff ), 
            (byte)( ( flags >> 8 ) & 0x00ff ), 
            (byte)( flags & 0x00ff ) };
    }
    
    
    /**
     * @return The byte array for a KerberosFlags
     */
    public byte[] getBytes()
    {
        return getData();
    }

    
    /**
     * Returns the int value associated with the flags
     */
    public int getIntValue()
    {
        return value;
    }
    
    
    /**
     * Check if a flag is set
     * @param flags The flags to test
     * @param flag The flag to check
     * @return True if the flag is set in the list of flags
     */
    public static boolean isFlagSet( int flags, int flag )
    {
        return ( flags & ( 1 << flag) ) != 0;
    }
    

    /**
     * Check if a flag is set for the actual value
     * 
     * @param flag The flag to check
     * @return True if the flag is set in the list of flags
     */
    public boolean isFlagSet( KerberosFlag flag )
    {
        return ( value & ( 1 << flag.getOrdinal() ) ) != 0;
    }
    
    
    /**
     * Check if a flag is set
     * @param flag The flags to test
     * @return True if the flag is set in the list of flags
     */
    public boolean isFlagSet( int flag )
    {
        return ( value & ( 1 << flag ) ) != 0;
    }
    
    
    /**
     * Set a flag in a list of flags
     * 
     * @param flag The flag to set
     */
    public void setFlag( KerberosFlag flag )
    {
        value |= 1 << flag.getOrdinal();
        setBit( flag.getOrdinal() );
    }
    
    
    /**
     * Set a flag in a list of flags
     * 
     * @param flag The flag to set
     */
    public void setFlag( int flag )
    {
        value |= 1 << flag;
        setBit( flag );
    }
    

    /**
     * clear a flag in a list of flags
     * 
     * @param flag The flag to set
     */
    public void clearFlag( KerberosFlag flag )
    {
        value &= ~( 1 << flag.getOrdinal() );
        clearBit( flag.getOrdinal() );
    }
    
    
    /**
     * clear a flag in a list of flags
     * 
     * @param flag The flag to set
     */
    public void clearFlag( int flag )
    {
        value &= ~( 1 << flag );
        clearBit( flag );
    }
}
