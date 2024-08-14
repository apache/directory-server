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
package org.apache.directory.server.core.partition.impl.btree.jdbm;


import java.io.IOException;

import jdbm.helper.Serializer;

import org.apache.directory.api.util.Strings;
import org.apache.directory.server.i18n.I18n;


/**
 * A custom String serializer to [de]serialize Strings.
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 */
public final class StringSerializer implements Serializer
{
    private static final long serialVersionUID = -173163945773783649L;

    /** A static instance of a StringSerializer */
    public static final StringSerializer INSTANCE = new StringSerializer();


    /**
     * Default private constructor
     */
    private StringSerializer()
    {
    }


    /* (non-Javadoc)
     * @see jdbm.helper.Serializer#deserialize(byte[])
     */
    public Object deserialize( byte[] bytes ) throws IOException
    {
        if ( bytes.length == 0 )
        {
            return "";
        }
        
        // Check that we don't have a odd number of bytes (we should have an even number of bytes)
        if ( ( bytes.length & 1 ) == 1 )
        {
            throw new IOException( I18n.err( I18n.ERR_31001_ODD_NUMBER_OF_BYTES_IN_SERIALIZED_STRING ) );
        }

        char[] strchars = new char[bytes.length >> 1];
        int pos = 0;

        for ( int i = 0; i < bytes.length; i += 2 )
        {
            strchars[pos++] = ( char ) ( ( ( bytes[i] << 8 ) & 0x0000FF00 ) | ( bytes[i + 1] & 0x000000FF ) );
        }

        return new String( strchars );
    }


    /* (non-Javadoc)
     * @see jdbm.helper.Serializer#serialize(java.lang.Object)
     */
    public byte[] serialize( Object str ) throws IOException
    {
        if ( ( ( String ) str ).length() == 0 )
        {
            return Strings.EMPTY_BYTES;
        }

        char[] strchars = ( ( String ) str ).toCharArray();
        byte[] bites = new byte[strchars.length << 1];
        int pos = 0;

        for ( char c : strchars )
        {
            bites[pos++] = ( byte ) ( c >> 8 & 0x00FF );
            bites[pos++] = ( byte ) ( c & 0x00FF );
        }

        return bites;
    }
}
