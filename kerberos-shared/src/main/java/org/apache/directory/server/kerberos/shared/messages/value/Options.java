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
package org.apache.directory.server.kerberos.shared.messages.value;


import java.util.BitSet;


public abstract class Options
{
    private BitSet options;
    private int maxSize;


    protected Options(int maxSize)
    {
        this.maxSize = maxSize;
        options = new BitSet( maxSize );
    }


    public boolean match( Options options, int option )
    {
        return options.get( option ) == this.get( option );
    }


    public boolean get( int index )
    {
        return options.get( index );
    }


    public void set( int index )
    {
        options.set( index );
    }


    public void clear( int index )
    {
        options.clear( index );
    }


    /*
     * Byte-reversing methods are an anomaly of the BouncyCastle
     * DERBitString endianness.  Thes methods can be removed if the
     * Apache Directory Snickers codecs operate differently.
     */
    public byte[] getBytes()
    {
        byte[] bytes = new byte[maxSize / 8];

        for ( int ii = 0; ii < maxSize; ii++ )
        {
            if ( options.get( reversePosition( ii ) ) )
            {
                bytes[bytes.length - ii / 8 - 1] |= 1 << ( ii % 8 );
            }
        }
        return bytes;
    }


    protected void setBytes( byte[] bytes )
    {
        for ( int ii = 0; ii < bytes.length * 8; ii++ )
        {
            if ( ( bytes[bytes.length - ii / 8 - 1] & ( 1 << ( ii % 8 ) ) ) > 0 )
            {
                options.set( reversePosition( ii ) );
            }
        }
    }


    private int reversePosition( int position )
    {
        return maxSize - 1 - position;
    }
}
