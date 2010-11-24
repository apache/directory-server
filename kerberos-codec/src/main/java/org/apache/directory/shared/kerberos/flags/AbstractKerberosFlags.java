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
package org.apache.directory.shared.kerberos.flags;


/**
 * An implementation of a BitString for any KerberosFlags. The different values
 * are stored in an int, as there can't be more than 32 flags (TicketFlag).
 * 
 * Some basic operations are implemented in this abstract class, like those
 * manipulating flags.
 * 
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 */
public abstract class AbstractKerberosFlags implements KerberosFlags
{
    /**
     * The maximum size of the BitString as specified for Kerberos flags.
     */
    public static final int MAX_SIZE = 32;

    /** The associated value */
    protected int value;
    
    
    /**
     * Standard constructor, which create a BitString containing 32 bits
     */
    public AbstractKerberosFlags()
    {
        value = 0;
    }

    
    /**
     * Standard constructor, which create a BitString containing 32 bits
     * 
     * 
     * @param value The flags to store
     */
    public AbstractKerberosFlags( int value )
    {
        this.value = value;
    }

    
    /**
     * Standard constructor, taking a byte array, 32 bits
     */
    public AbstractKerberosFlags( byte[] flags )
    {
        if ( ( flags == null ) || ( flags.length != 4 ) )
        {
            throw new IllegalArgumentException( "The given flags is not correct" );
        }
        
        value = ( ( flags[0] & 0x00FF ) << 24 ) | ( ( flags[1] & 0x00FF ) << 16 ) | ( ( flags[2] & 0x00FF ) << 8 ) | ( 0x00FF & flags[3] ); 
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
        return ( flags & ( 1 << ( MAX_SIZE - 1 - flag ) ) ) != 0;
    }
    

    /**
     * Check if a flag is set for the actual value
     * 
     * @param flag The flag to check
     * @return True if the flag is set in the list of flags
     */
    public boolean isFlagSet( KerberosFlag flag )
    {
        int value = flag.getValue();
        int mask = 1 << ( MAX_SIZE - 1 - value );
        
        return ( value & mask ) != 0;
    }
    
    
    /**
     * Check if a flag is set
     * @param flag The flags to test
     * @return True if the flag is set in the list of flags
     */
    public boolean isFlagSet( int flag )
    {
        return ( value & ( 1 << ( MAX_SIZE - 1 - flag ) ) ) != 0;
    }
    
    
    /**
     * Set a flag in a list of flags
     * 
     * @param flag The flag to set
     */
    public void setFlag( KerberosFlag flag )
    {
        int pos = MAX_SIZE - 1 - flag.getValue();
        value |= 1 << pos;
    }
    
    
    /**
     * Set a flag in a list of flags
     * 
     * @param flag The flag to set
     */
    public void setFlag( int flag )
    {
        value |= 1 << ( MAX_SIZE - 1 - flag );
    }
    

    /**
     * clear a flag in a list of flags
     * 
     * @param flag The flag to set
     */
    public void clearFlag( KerberosFlag flag )
    {
        value &= ~( 1 << ( MAX_SIZE - 1 - flag.getValue() ) );
    }
    
    
    /**
     * clear a flag in a list of flags
     * 
     * @param flag The flag to set
     */
    public void clearFlag( int flag )
    {
        value &= ~( 1 << ( MAX_SIZE - 1 - flag ) );
    }
}
