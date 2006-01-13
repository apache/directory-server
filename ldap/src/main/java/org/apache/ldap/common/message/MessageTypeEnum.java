/*
 *   Copyright 2004 The Apache Software Foundation
 *
 *   Licensed under the Apache License, Version 2.0 (the "License");
 *   you may not use this file except in compliance with the License.
 *   You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *   Unless required by applicable law or agreed to in writing, software
 *   distributed under the License is distributed on an "AS IS" BASIS,
 *   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *   See the License for the specific language governing permissions and
 *   limitations under the License.
 *
 */
package org.apache.ldap.common.message ;


import org.apache.ldap.common.util.ValuedEnum ;


/**
 * Type safe enumeration over the various LDAPv3 message types.
 *
 * @author <a href="mailto:aok123@bellsouth.net">Alex Karasulu</a>
 * @author $Author: akarasulu $
 * @version $Revision$
 */
public class MessageTypeEnum
    extends ValuedEnum
{
    static final long serialVersionUID = 4297707466257974528L;
    /** Bind request protocol message type value */
    public static final int BINDREQUEST_VAL = 0x40000000 ;
    /** Bind response protocol message type value */
    public static final int BINDRESPONSE_VAL = 0x40000001 ;
    /** Unbind request protocol message type value */
    public static final int UNBINDREQUEST_VAL = 0x40000002 ;
    /** Search request protocol message type value */
    public static final int SEARCHREQUEST_VAL = 0x40000003 ;
    /** Search entry response protocol message type value */
    public static final int SEARCHRESENTRY_VAL = 0x40000004 ;
    /** Search done response protocol message type value */
    public static final int SEARCHRESDONE_VAL = 0x40000005 ;
    /** Search reference response protocol message type value */
    public static final int SEARCHRESREF_VAL = 0x40000013 ;
    /** Modify request protocol message type value */
    public static final int MODIFYREQUEST_VAL = 0x40000006 ;
    /** Modify response protocol message type value */
    public static final int MODIFYRESPONSE_VAL = 0x40000007 ;
    /** Add request protocol message type value */
    public static final int ADDREQUEST_VAL = 0x40000008 ;
    /** Add response protocol message type value */
    public static final int ADDRESPONSE_VAL = 0x40000009 ;
    /** Delete request protocol message type value */
    public static final int DELREQUEST_VAL = 0x4000000a ;
    /** Delete response protocol message type value */
    public static final int DELRESPONSE_VAL = 0x4000000b ;
    /** Modify DN request protocol message type value */
    public static final int MODDNREQUEST_VAL = 0x4000000c ;
    /** Modify DN response protocol message type value */
    public static final int MODDNRESPONSE_VAL = 0x4000000d ;
    /** Compare request protocol message type value */
    public static final int COMPAREREQUEST_VAL = 0x4000000e ;
    /** Compare response protocol message type value */
    public static final int COMPARERESPONSE_VAL = 0x4000000f ;
    /** Abandon request protocol message type value */
    public static final int ABANDONREQUEST_VAL = 0x40000010 ;
    /** Extended request protocol message type value */
    public static final int EXTENDEDREQ_VAL = 0x40000017 ;
    /** Extended response protocol message type value */
    public static final int EXTENDEDRESP_VAL = 0x40000018 ;

    /** Bind request protocol message type */
    public static final MessageTypeEnum BINDREQUEST =
        new MessageTypeEnum( "BINDREQUEST", BINDREQUEST_VAL ) ;
    /** Bind response protocol message type */
    public static final MessageTypeEnum BINDRESPONSE =
        new MessageTypeEnum( "BINDRESPONSE", BINDRESPONSE_VAL ) ;
    /** Unbind request protocol message type */
    public static final MessageTypeEnum UNBINDREQUEST =
        new MessageTypeEnum( "UNBINDREQUEST", UNBINDREQUEST_VAL ) ;
    /** Search request protocol message type */
    public static final MessageTypeEnum SEARCHREQUEST =
        new MessageTypeEnum( "SEARCHREQUEST", SEARCHREQUEST_VAL ) ;
    /** Search entry response protocol message type */
    public static final MessageTypeEnum SEARCHRESENTRY =
        new MessageTypeEnum( "SEARCHRESENTRY", SEARCHRESENTRY_VAL ) ;
    /** Search done response protocol message type */
    public static final MessageTypeEnum SEARCHRESDONE =
        new MessageTypeEnum( "SEARCHRESDONE", SEARCHRESDONE_VAL ) ;
    /** Search reference response protocol message type */
    public static final MessageTypeEnum SEARCHRESREF =
        new MessageTypeEnum( "SEARCHRESREF", SEARCHRESREF_VAL ) ;
    /** Modify request protocol message type */
    public static final MessageTypeEnum MODIFYREQUEST =
        new MessageTypeEnum( "MODIFYREQUEST", MODIFYREQUEST_VAL ) ;
    /** Modify response protocol message type */
    public static final MessageTypeEnum MODIFYRESPONSE =
        new MessageTypeEnum( "MODIFYRESPONSE", MODIFYRESPONSE_VAL ) ;
    /** Add request protocol message type */
    public static final MessageTypeEnum ADDREQUEST =
        new MessageTypeEnum( "ADDREQUEST", ADDREQUEST_VAL ) ;
    /** Add response protocol message type */
    public static final MessageTypeEnum ADDRESPONSE =
        new MessageTypeEnum( "ADDRESPONSE", ADDRESPONSE_VAL ) ;
    /** Delete request protocol message type */
    public static final MessageTypeEnum DELREQUEST =
        new MessageTypeEnum( "DELREQUEST", DELREQUEST_VAL ) ;
    /** Delete response protocol message type */
    public static final MessageTypeEnum DELRESPONSE =
        new MessageTypeEnum( "DELRESPONSE", DELRESPONSE_VAL ) ;
    /** Modify DN request protocol message type */
    public static final MessageTypeEnum MODDNREQUEST =
        new MessageTypeEnum( "MODDNREQUEST", MODDNREQUEST_VAL ) ;
    /** Modify DN response protocol message type */
    public static final MessageTypeEnum MODDNRESPONSE =
        new MessageTypeEnum( "MODDNRESPONSE", MODDNRESPONSE_VAL ) ;
    /** Compare request protocol message type */
    public static final MessageTypeEnum COMPAREREQUEST =
        new MessageTypeEnum( "COMPAREREQUEST", COMPAREREQUEST_VAL ) ;
    /** Compare response protocol message type */
    public static final MessageTypeEnum COMPARERESPONSE =
        new MessageTypeEnum( "COMPARERESPONSE", COMPARERESPONSE_VAL ) ;
    /** Abandon request protocol message type */
    public static final MessageTypeEnum ABANDONREQUEST =
        new MessageTypeEnum( "ABANDONREQUEST", ABANDONREQUEST_VAL ) ;
    /** Extended request protocol message type */
    public static final MessageTypeEnum EXTENDEDREQ =
        new MessageTypeEnum( "EXTENDEDREQ", EXTENDEDREQ_VAL ) ;
    /** Extended response protocol message type */
    public static final MessageTypeEnum EXTENDEDRESP =
        new MessageTypeEnum( "EXTENDEDRESP", EXTENDEDRESP_VAL ) ;

    /**
     * Private construct so no other instances can be created other than the
     * public static constants in this class.
     *
     * @param a_name a string name for the enumeration value.
     * @param a_value the integer value of the enumeration.
     */
    private MessageTypeEnum( final String a_name, final int a_value )
    {
        super( a_name, a_value ) ;
    }
}
