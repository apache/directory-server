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
package org.apache.directory.server.kerberos.shared;

/**
 * An enum listing all the Kerberos V5 messages :
 * 
 *   AS-REQ    (10) : Authentication Serveur Request
 *   AS-REP    (11) : Authentication Serveur Response
 *   TGS-REQ   (12) : Ticket Granting Server Request
 *   TGS-REP   (13) : Ticket Granting Server Response
 *   AP-REQ    (14) : Application Request
 *   AP-REP    (15) : Application Response
 *   KRB-SAFE  (20) : Safe (checksummed) application message
 *   KRB-PRIV  (21) : Private (encrypted) application message
 *   KRB-CRED  (22) : Private (encrypted) message to forward credentials
 *   ENC_AP_REP_PART (27) : Encrypted application reply part
 *   ENC_PRIV_PART (28) : Encrypted private message part
 *   KRB-ERROR (30) : A kerberos error response
 *   
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 * @version $Rev$, $Date$
 */
public enum KerberosMessageType
{
    AS_REQ( 10, "initial authentication request" ),
    AS_REP( 11, "initial authentication response"),
    TGS_REQ( 12, "request for authentication based on TGT" ),
    TGS_REP( 13, "response to authentication based on TGT" ),
    AP_REQ( 14, "application request" ), 
    AP_REP( 15, "application response" ), 
    KRB_SAFE( 20, "safe (checksummed) application message" ), 
    KRB_PRIV( 21,  "private (encrypted) application message" ), 
    KRB_CRED( 22, "private (encrypted) message to forward credentials" ),
    ENC_AP_REP_PART( 27, "encrypted application reply part" ),
    ENC_PRIV_PART( 28, "encrypted private message part" ),
    KRB_ERROR( 30, "error response" );
    
    private int value;
    private String message;
    
    /**
     * Creates a new instance of KerberosMessageType.
     */
    private KerberosMessageType( int value, String message )
    {
        this.value = value;
        this.message = message;
    }

    
    /**
     * Get the int value for this element
     *
     * @return The int value of this element
     */
    public int getOrdinal()
    {
        return value;
    }
    
    
    /**
     * Get the message associated with this element
     *
     * @return The message associated with this element
     */
    public String getMessage()
    {
        return message;
    }
    
    
    /**
     * Get the instance of a KerberosMessageType from an int value
     *
     * @param value The int value 
     * @return A KerberosMessageType associated with this value
     */
    public static KerberosMessageType getTypeByOrdinal( int value )
    {
        switch ( value )
        {
            case 10 : return AS_REQ;
            case 11 : return AS_REP;
            case 12 : return TGS_REQ;
            case 13 : return TGS_REP;
            case 14 : return AP_REQ; 
            case 15 : return AP_REP; 
            case 20 : return KRB_SAFE; 
            case 21 : return KRB_PRIV; 
            case 22 : return KRB_CRED;
            case 27 : return ENC_AP_REP_PART;
            case 28 : return ENC_PRIV_PART;
            case 30 : return KRB_ERROR;
            default : return null;
        }
    }
}
