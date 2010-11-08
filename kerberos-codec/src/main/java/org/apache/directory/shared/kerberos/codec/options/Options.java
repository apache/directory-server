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
package org.apache.directory.shared.kerberos.codec.options;


import java.util.BitSet;


/**
 * A base class to manage Kerberos BitSet elements.
 * 
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 */
public abstract class Options
{
    /** The Bits storage */
    private BitSet options;
    
    /** The size for the BitSet */
    private int maxSize;


    /**
     * Creates a new BitSet with a specific number of bits.
     * @param maxSize The number of bits to allocate
     */
    protected Options( int maxSize )
    {
        this.maxSize = maxSize;
        options = new BitSet( maxSize );
    }


    /**
     * Returns whether the option at a given index matches the option in this {@link Options}.
     *
     * @param options The set of possible options
     * @param option The Option we are looking for
     * @return true if two options are the same.
     */
    public boolean match( Options options, int option )
    {
        return options.get( option ) == this.get( option );
    }


    /**
     * Returns the value of the option at the given index.
     *
     * @param index The position in the BitSet for the option we are looking for
     * @return true if the option at the given index is set.
     */
    public boolean get( int index )
    {
        if ( index >= maxSize )
        {
            throw new ArrayIndexOutOfBoundsException();
        }
        
        return options.get( index );
    }


    /**
     * Sets the option at a given index.
     *
     * @param index The position of the Option w want to set
     */
    public void set( int index )
    {
        if ( ( index < 0 ) || ( index > options.size() ) )
        {
            return;
        }
        
        options.set( index );
    }


    /**
     * Clears (sets false) the option at a given index.
     *
     * @param index The position of the Option we want to clear
     */
    public void clear( int index )
    {
        if ( ( index < 0 ) || ( index > options.size() ) )
        {
            return;
        }
        
        options.clear( index );
    }


    /**
     * Byte-reversing methods are an anomaly of the BouncyCastle
     * DERBitString endianness. These methods can be removed if the
     * Apache Directory Snickers codecs operate differently.
     * 
     * @return The raw {@link Options} bytes.
     */
    public byte[] getBytes()
    {
        byte[] bytes = new byte[maxSize / 8];

        for ( int i = 0; i < maxSize; i++ )
        {
            if ( options.get( i ) )
            {
                bytes[bytes.length - i / 8 - 1] |= 1 << ( i % 8 );
            }
        }
        return bytes;
    }


    protected void setBytes( byte[] bytes )
    {
        for ( int i = 0; i < bytes.length * 8; i++ )
        {
            if ( ( bytes[bytes.length - i / 8 - 1] & ( 1 << ( i % 8 ) ) ) > 0 )
            {
                options.set( i);
            }
        }
    }
    
    
    /**
     * {@inheritDoc}
     */
    public String toString()
    {
        StringBuilder sb = new StringBuilder();
        
        for ( int i = maxSize - 1; i >= 0; i-- )
        {
            sb.append( options.get( i ) ? "1" : "0" );
        }
        
        return sb.toString();
    }
}
