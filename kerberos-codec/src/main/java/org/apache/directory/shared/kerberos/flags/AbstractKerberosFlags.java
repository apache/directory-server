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


import org.apache.directory.shared.asn1.util.BitString;


/**
 * An implementation of a BitString for any KerberosFlags. The different values
 * are stored in an int, as there can't be more than 32 flags (TicketFlag).
 *
 * Some basic operations are implemented in this abstract class, like those
 * manipulating flags.
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 */
@SuppressWarnings("serial")
public abstract class AbstractKerberosFlags extends BitString
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
        super( MAX_SIZE );
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
        super( MAX_SIZE );

        setData( value );
    }


    /**
     * Store the flags contained in the given integer value
     * @param value The list of flags to set, as a int
     */
    public void setData( int value )
    {
        byte[] bytes = new byte[5];

        // The first byte contains the number of unused bytes, 0 here as we store 32 bits
        bytes[0] = 0;

        bytes[1] = ( byte ) ( value >> 24 );
        bytes[3] = ( byte ) ( ( value >> 16 ) & 0x00FF );
        bytes[3] = ( byte ) ( ( value >> 8 ) & 0x00FF );
        bytes[4] = ( byte ) ( value & 0x00FF );

        super.setData( bytes );
        this.value = value;
    }


    /**
     * Standard constructor, taking a byte array, 32 bits
     */
    public AbstractKerberosFlags( byte[] flags )
    {
        super( flags );

        if ( ( flags == null ) || ( flags.length != 5 ) )
        {
            throw new IllegalArgumentException( "The given flags is not correct" );
        }

        value = ( ( flags[1] & 0x00FF ) << 24 ) | ( ( flags[2] & 0x00FF ) << 16 ) | ( ( flags[3] & 0x00FF ) << 8 )
            | ( 0x00FF & flags[4] );
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
        int mask = 1 << ( MAX_SIZE - 1 - flag.getValue() );

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
        setBit( flag.getValue() );
        value |= 1 << pos;
    }


    /**
     * Set a flag in a list of flags
     *
     * @param flag The flag to set
     */
    public void setFlag( int flag )
    {
        int pos = MAX_SIZE - 1 - flag;
        setBit( flag );
        value |= 1 << pos;
    }


    /**
     * clear a flag in a list of flags
     *
     * @param flag The flag to set
     */
    public void clearFlag( KerberosFlag flag )
    {
        int pos = MAX_SIZE - 1 - flag.getValue();
        clearBit( flag.getValue() );
        value &= ~( 1 << pos );
    }


    /**
     * clear a flag in a list of flags
     *
     * @param flag The flag to set
     */
    public void clearFlag( int flag )
    {
        int pos = MAX_SIZE - 1 - flag;
        clearBit( flag );
        value &= ~( 1 << pos );
    }


    @Override
    public int hashCode()
    {
        final int prime = 31;
        int result = 1;
        result = prime * result + value;
        return result;
    }


    @Override
    public boolean equals( Object obj )
    {
        if ( this == obj )
        {
            return true;
        }

        if ( obj == null )
        {
            return false;
        }

        AbstractKerberosFlags other = ( AbstractKerberosFlags ) obj;

        return value == other.value;
    }
}
