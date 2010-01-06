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


/**
 * A custom String serializer to [de]serialize Strings.
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 * @version $Rev$, $Date$
 */
public class StringSerializer implements Serializer
{
    private static final long serialVersionUID = -173163945773783649L;


    /* (non-Javadoc)
     * @see jdbm.helper.Serializer#deserialize(byte[])
     */
    public Object deserialize( byte[] bites ) throws IOException
    {
        if ( bites.length == 0 )
        {
            return "";
        }

        char[] strchars = new char[bites.length >> 1];
        for ( int ii = 0, jj = 0; ii < strchars.length; ii++, jj = ii << 1 )
        {
            int ch = bites[jj] << 8 & 0x0000FF00;
            ch |= bites[jj+1] & 0x000000FF;
            strchars[ii] = ( char ) ch;
        }
        return new String( strchars );
    }


    private static final byte[] EMPTY_BYTE_ARRAY = new byte[0];
    
    /* (non-Javadoc)
     * @see jdbm.helper.Serializer#serialize(java.lang.Object)
     */
    public byte[] serialize( Object str ) throws IOException
    {
        if ( ( ( String ) str ).length() == 0 )
        {
            return EMPTY_BYTE_ARRAY;
        }
        
        char[] strchars = ( ( String ) str ).toCharArray();
        byte[] bites = new byte[strchars.length<<1];
        for ( int ii = 0, jj = 0; ii < strchars.length; ii++, jj = ii << 1 )
        {
            bites[jj] = ( byte ) ( strchars[ii] >> 8 & 0x00FF );
            bites[jj+1] = ( byte ) ( strchars[ii] & 0x00FF );
        }
        
        return bites;
    }
}
