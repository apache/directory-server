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
package org.apache.directory.server.kerberos.shared.messages.value.types;


/**
 * An enum describing the differnet types of Principal.
 * 
 * Here is the list, taken from RFC 4120 :
 *  NT-UNKNOWN        0    Name type not known
 *  NT-PRINCIPAL      1    Just the name of the principal as in DCE,
 *                           or for users
 *  NT-SRV-INST       2    Service and other unique instance (krbtgt)
 *  NT-SRV-HST        3    Service with host name as instance
 *                           (telnet, rcommands)
 *  NT-SRV-XHST       4    Service with host as remaining components
 *  NT-UID            5    Unique ID
 *  NT-X500-PRINCIPAL 6    Encoded X.509 Distinguished name [RFC2253]
 *  NT-SMTP-NAME      7    Name in form of SMTP email name
 *                           (e.g., user@example.com)
 *  NT-ENTERPRISE    10    Enterprise name - may be mapped to principal
 *                           name
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 * @version $Rev: 540371 $, $Date: 2007-05-22 02:00:43 +0200 (Tue, 22 May 2007) $
 */
public enum PrincipalNameType
{
    /**
     * Constant for the "Name type not known" principal name type.
     */
    KRB_NT_UNKNOWN( 0 ),

    /**
     * Constant for the "Just the name of the principal as in DCE, or for users" principal name type.
     */
    KRB_NT_PRINCIPAL( 1 ),

    /**
     * Constant for the "Service and other unique instance (krbtgt)" principal name type.
     */
    KRB_NT_SRV_INST( 2 ),

    /**
     * Constant for the "Service with host name as instance (telnet, rcommands)" principal name type.
     */
    KRB_NT_SRV_HST( 3 ),

    /**
     * Constant for the "Service with host as remaining components" principal name type.
     */
    KRB_NT_SRV_XHST( 4 ),

    /**
     * Constant for the "Unique ID" principal name type.
     */
    KRB_NT_UID( 5 ),

    /**
     * Constant for the "Encoded X.509 Distinguished name [RFC2253]" principal name type.
     */
    KRB_NT_X500_PRINCIPAL( 6 ),

    /**
     * Constant for the "Name in form of SMTP email name (e.g., user@example.com)" principal name type.
     */
    KRB_NT_SMTP_NAME( 7 ),

    /**
     * Constant for the "Enterprise name; may be mapped to principal name" principal name type.
     */
    KRB_NT_ENTERPRISE( 10 );

    /**
     * The value/code for the principal name type.
     */
    private final int ordinal;


    /**
     * Private constructor prevents construction outside of this class.
     */
    private PrincipalNameType( int ordinal )
    {
        this.ordinal = ordinal;
    }


    /**
     * Returns the principal name type when specified by its ordinal.
     *
     * @param type
     * @return The principal name type.
     */
    public static PrincipalNameType getTypeByOrdinal( int type )
    {
        switch ( type )
        {
            case 0 : return KRB_NT_UNKNOWN;
            case 1 : return KRB_NT_PRINCIPAL;
            case 2 : return KRB_NT_SRV_INST;
            case 3 : return KRB_NT_SRV_HST;
            case 4 : return KRB_NT_SRV_XHST;
            case 5 : return KRB_NT_UID;
            case 6 : return KRB_NT_X500_PRINCIPAL;
            case 7 : return KRB_NT_SMTP_NAME;
            case 10 : return KRB_NT_ENTERPRISE;
            default : return KRB_NT_UNKNOWN;
        }
    }


    /**
     * Returns the number associated with this principal name type.
     *
     * @return The principal name type ordinal.
     */
    public int getOrdinal()
    {
        return ordinal;
    }

    /**
     * @see Object#toString()
     */
    public String toString()
    {
        switch ( this )
        {
            case KRB_NT_UNKNOWN         : 
                return "Name type not known" + "(" + ordinal + ")";
                
            case KRB_NT_PRINCIPAL       : 
                return "Just the name of the principal as in DCE, or for users" + "(" + ordinal + ")";
                
            case KRB_NT_SRV_INST        : 
                return "Service and other unique instance (krbtgt)" + "(" + ordinal + ")";
            
            case KRB_NT_SRV_HST         : 
                return "Service with host name as instance (telnet, rcommands)" + "(" + ordinal + ")";
            
            case KRB_NT_SRV_XHST        : 
                return "Service with host as remaining components" + "(" + ordinal + ")";
            
            case KRB_NT_UID             : 
                return "Unique ID" + "(" + ordinal + ")";
            
            case KRB_NT_X500_PRINCIPAL  : 
                return "Encoded X.509 Distinguished name [RFC2253]" + "(" + ordinal + ")";
            
            case KRB_NT_SMTP_NAME       : 
                return "Name in form of SMTP email name (e.g., user@example.com)" + "(" + ordinal + ")";
            
            case KRB_NT_ENTERPRISE      : 
                return "Enterprise name; may be mapped to principal name" + "(" + ordinal + ")";
            
            default                     : 
                return "unknown name type" + "(" + ordinal + ")";
        }
    }
}
