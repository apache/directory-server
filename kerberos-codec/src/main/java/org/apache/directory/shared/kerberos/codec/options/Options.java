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


import org.apache.directory.shared.kerberos.flags.AbstractKerberosFlags;


/**
 * A base class to manage Kerberos BitSet elements.
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 */
public abstract class Options extends AbstractKerberosFlags
{
    /**
     * Creates a new BitSet with a specific number of bits.
     * @param maxSize The number of bits to allocate
     */
    protected Options( int maxSize )
    {
        super( maxSize );
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
        if ( ( index < 0 ) || ( index >= size() ) )
        {
            throw new ArrayIndexOutOfBoundsException();
        }

        return super.getBit( index );
    }


    /**
     * Sets the option at a given index.
     *
     * @param index The position of the Option w want to set
     */
    public void set( int index )
    {
        if ( ( index < 0 ) || ( index >= size() ) )
        {
            return;
        }

        setBit( index );
    }


    /**
     * Clears (sets false) the option at a given index.
     *
     * @param index The position of the Option we want to clear
     */
    public void clear( int index )
    {
        if ( ( index < 0 ) || ( index >= size() ) )
        {
            return;
        }

        clearBit( index );
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
        return super.getData();
    }


    /**
     * Set the array of bytes representing the bits
     * @param bytes The bytes to store
     */
    protected void setBytes( byte[] bytes )
    {
        super.setData( bytes );
    }


    /**
     * {@inheritDoc}
     */
    @Override
    public String toString()
    {
        return super.toString();
    }
}
