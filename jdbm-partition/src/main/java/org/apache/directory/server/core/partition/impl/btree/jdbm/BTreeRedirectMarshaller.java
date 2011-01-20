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

import org.apache.directory.server.core.avltree.Marshaller;
import org.apache.directory.server.i18n.I18n;
import org.apache.directory.shared.asn1.Hex;


/**
 * Serializes and deserializes a BTreeRedirect object to and from a byte[]
 * representation.  The serialized form is a fixed size byte array of length
 * 9.  The first byte contains the magic number of value 1 for this kind of
 * object and the last 8 bytes encode the record identifier as a long for
 * the BTree.
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 */
public class BTreeRedirectMarshaller implements Marshaller<BTreeRedirect>
{
    /** fixed byte array size of 9 for serialized form */
    static final int SIZE = 9;
    /** a reusable instance of this Marshaller */
    public static final BTreeRedirectMarshaller INSTANCE = new BTreeRedirectMarshaller();

    /**
     * @see Marshaller#serialize(Object)
     */
    public final byte[] serialize( BTreeRedirect redirect ) throws IOException
    {
        byte[] bites = new byte[SIZE];

        bites[0] = 1;

        bites[1] = ( byte ) ( redirect.recId >> 56 );
        bites[2] = ( byte ) ( redirect.recId >> 48 );
        bites[3] = ( byte ) ( redirect.recId >> 40 );
        bites[4] = ( byte ) ( redirect.recId >> 32 );
        bites[5] = ( byte ) ( redirect.recId >> 24 );
        bites[6] = ( byte ) ( redirect.recId >> 16 );
        bites[7] = ( byte ) ( redirect.recId >> 8 );
        bites[8] = ( byte ) redirect.recId;

        return bites;
    }


    /**
     * @see Marshaller#deserialize(byte[]) 
     */
    public final BTreeRedirect deserialize( byte[] bites ) throws IOException
    {
        if ( bites == null || bites.length != SIZE || bites[0] != 1 )
        {
            if ( bites != null )
            {
                throw new IOException( I18n.err( I18n.ERR_568, new String( Hex.encodeHex(bites) ) ) );
            }
            else
            {
                throw new IOException( I18n.err( I18n.ERR_569 ) );
            }
        }

        long recId;
        recId = bites[1] + ( ( bites[1] < 0 ) ? 256 : 0 );
        recId <<= 8;
        recId += bites[2] + ( ( bites[2] < 0 ) ? 256 : 0 );
        recId <<= 8;
        recId += bites[3] + ( ( bites[3] < 0 ) ? 256 : 0 );
        recId <<= 8;
        recId += bites[4] + ( ( bites[4] < 0 ) ? 256 : 0 );
        recId <<= 8;
        recId += bites[5] + ( ( bites[5] < 0 ) ? 256 : 0 );
        recId <<= 8;
        recId += bites[6] + ( ( bites[6] < 0 ) ? 256 : 0 );
        recId <<= 8;
        recId += bites[7] + ( ( bites[7] < 0 ) ? 256 : 0 );
        recId <<= 8;
        recId += bites[8] + ( ( bites[8] < 0 ) ? 256 : 0 );

        return new BTreeRedirect( recId );
    }


    /**
     * Checks to see if a byte[] contains a redirect.
     *
     * @param bites the bites to check for a redirect
     * @return true if bites contain BTreeRedirect, false otherwise
     */
    public static boolean isRedirect( byte[] bites )
    {
        return bites != null && bites.length == SIZE && bites[0] == 1;
    }
}
