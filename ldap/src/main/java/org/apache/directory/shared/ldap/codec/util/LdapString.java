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
package org.apache.directory.shared.ldap.codec.util;


import java.io.UnsupportedEncodingException;


/**
 * Decodes a LdapString, and checks that the character set used comply the ISO
 * 10646 encoded following the UTF-8 algorithm (RFC 2044, RFC 2279)
 * 
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 */
public class LdapString
{
    /** A null LdapString */
    public transient static final LdapString EMPTY_STRING = new LdapString();

    /** A null LdapString */
    public transient static final byte[] EMPTY_BYTES = new byte[]
        {};

    /** The inner String containing the LdapString */
    protected String string;

    /** The internal bytes representation of the LdapString */
    protected byte[] bytes;


    /**
     * Construct an empty LdapString
     */
    public LdapString()
    {
        bytes = EMPTY_BYTES;
        string = "";
    }


    // ~ Methods
    // ------------------------------------------------------------------------------------

    /**
     * Transform a byte array to a String. The byte array contains an UTF-8
     * representation of a String.
     * 
     * @param bytes
     *            The byte buffer that contains the LDAPSTRING
     * @throws LdapStringEncodingException
     *             If the byte array is not a UTF-8 encoded ISO-10646 (Unicode)
     *             compatible String.
     */
    public LdapString(byte[] bytes) throws LdapStringEncodingException
    {
        try
        {
            string = new String( bytes, "UTF-8" );
            this.bytes = new byte[bytes.length];

            System.arraycopy( bytes, 0, this.bytes, 0, bytes.length );
        }
        catch ( UnsupportedEncodingException uee )
        {
            throw new LdapStringEncodingException( "The byte array is not an UTF-8 encoded Unicode String : "
                + uee.getMessage() );
        }
    }


    /**
     * Get the LdapString as a String
     * 
     * @return The string.
     */
    public String getString()
    {
        return string;
    }


    /**
     * Get the content of the LdapString, as a byte array;
     * 
     * @return A byte array of the LdapString string
     */
    public byte[] getBytes()
    {
        return bytes;
    }


    /**
     * Get the size of the UTF-8 encoded string
     * 
     * @return A number of bytes
     */
    public int getNbBytes()
    {
        return ( bytes != null ? bytes.length : 0 );
    }


    /**
     * Return the string representation of a LdapString
     */
    public String toString()
    {
        return string;
    }
}
