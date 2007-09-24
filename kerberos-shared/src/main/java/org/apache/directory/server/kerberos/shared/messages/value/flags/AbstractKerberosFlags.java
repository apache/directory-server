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
 * @version $Rev: 540371 $, $Date: 2007-05-22 02:00:43 +0200 (Tue, 22 May 2007) $
 */
public abstract class AbstractKerberosFlags extends BitString implements KerberosFlags
{
    /** The associated value */
    protected int value;
    
    /**
     * Standard constructor, which create a BitString containing 32 bits
     */
    public AbstractKerberosFlags()
    {
        super( 32 );
    }

    /**
     * Standard constructor, taking a byte array
     */
    public AbstractKerberosFlags( byte[] flags )
    {
        super( flags );
    }
    
    /**
     * A static method to get the bayte array representation of an int
     * @return The byte array for a list of flags.
     */
    public static byte[] getBytes( int flags )
    {
        return new byte[]{
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
        /*new byte[]{
                (byte)( value >>> 24), 
                (byte)( ( value >> 16 ) & 0x00ff ), 
                (byte)( ( value >> 8 ) & 0x00ff ), 
                (byte)( value & 0x00ff ) };*/
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
     * Check if a flag is set
     * @param flags The flags to test
     * @param flag The flag to check
     * @return True if the flag is set in the list of flags
     */
    /*public static boolean isFlagSet( int flags, KerberosFlag flag )
    {
        return ( flags & ( 1 << flag.getOrdinal() ) ) != 0;
    }*/
    
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
     * @param flags The flags to test
     * @return True if the flag is set in the list of flags
     */
    public boolean isFlagSet( int flag )
    {
        return ( flag & ( 1 << value ) ) != 0;
    }
    
    /**
     * Set a flag in a list of flags
     * 
     * @param flags The list of flags
     * @param flag The flag to set
     */
    /*public static int setFlag( int flags, KerberosFlag flag )
    {
        flags |= 1 << flag.getOrdinal();
        return flags;
    }*/
    
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
     * Modify a byte array to an integer value
     * @param value The 4 bytes byte array to transform.
     * @return The int which contains the bytes value.
     */
    public void setFlags( byte[] bytes )
    {
        if ( (bytes== null ) || ( bytes.length != 4 ) )
        {
            value = -1;
        }
        
        value = ( bytes[0] << 24 ) + ( bytes[1] << 16 ) + ( bytes[2] << 8 ) + bytes[3];
        setData( bytes );
    }
    
    /**
     * clear a flag in a list of flags
     * 
     * @param flags The list of flags
     * @param flag The flag to set
     */
    /*public static void clearFlag( int flags, KerberosFlag flag )
    {
        flags &= ~( 1 << flag.getOrdinal() );
    }*/

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
    
    /**
     * @return The hex value for this flag, in its position.
     * For instance, getting the flag 5 will return 0x0000 0010 
     */
    public int getHexValue()
    {
        return 1 << value;
    }
}
