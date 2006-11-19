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
package org.apache.directory.shared.ldap.message;

/**
 * Type safe enumeration over the various LDAPv3 message types.
 * 
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 * @version $Revision$
 */
public enum MessageTypeEnum
{
    /** Bind request protocol message type value */
    BIND_REQUEST( 0x40000000 ),

    /** Bind response protocol message type value */
    BIND_RESPONSE( 0x40000001 ),

    /** Unbind request protocol message type value */
    UNBIND_REQUEST( 0x40000002 ),

    /** Search request protocol message type value */
    SEARCH_REQUEST( 0x40000003 ),

    /** Search entry response protocol message type value */
    SEARCH_RES_ENTRY( 0x40000004 ),

    /** Search done response protocol message type value */
    SEARCH_RES_DONE( 0x40000005 ),

    /** Search reference response protocol message type value */
    SEARCH_RES_REF( 0x40000013 ),

    /** Modify request protocol message type value */
    MODIFY_REQUEST( 0x40000006 ),

    /** Modify response protocol message type value */
    MODIFY_RESPONSE( 0x40000007 ),

    /** Add request protocol message type value */
    ADD_REQUEST( 0x40000008 ),

    /** Add response protocol message type value */
    ADD_RESPONSE( 0x40000009 ),

    /** Delete request protocol message type value */
    DEL_REQUEST( 0x4000000 ),

    /** Delete response protocol message type value */
    DEL_RESPONSE( 0x4000000b ),

    /** Modify DN request protocol message type value */
    MOD_DN_REQUEST( 0x4000000c ),

    /** Modify DN response protocol message type value */
    MOD_DN_RESPONSE( 0x4000000d ),

    /** Compare request protocol message type value */
    COMPARE_REQUEST( 0x4000000e ),

    /** Compare response protocol message type value */
    COMPARE_RESPONSE( 0x4000000f ),

    /** Abandon request protocol message type value */
    ABANDON_REQUEST( 0x40000010 ),

    /** Extended request protocol message type value */
    EXTENDED_REQ( 0x40000017 ),

    /** Extended response protocol message type value */
    EXTENDED_RESP( 0x40000018 );
    
    private int messageType;

    /**
     * Private construct so no other instances can be created other than the
     * public static constants in this class.
     * 
     * @param messageType
     *            the integer value of the enumeration.
     */
    private MessageTypeEnum( int messageType )
    {
        this.messageType = messageType;
    }
    
    /**
     * @return The integer associated with the result code
     */
    public int getMessageType()
    {
        return messageType;
    }
}
