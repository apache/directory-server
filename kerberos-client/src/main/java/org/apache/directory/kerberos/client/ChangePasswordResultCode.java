/*
 *   Licensed to the Apache Software Foundation (ASF) under one
 *   or more contributor license agreements.  See the NOTICE file
 *   distributed with this work for additional information
 *   regarding copyright ownership.  The ASF licenses this file
 *   to you under the Apache License, Version 2.0 (the
 *   "License"); you may not use this file except in compliance
 *   with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 *   Unless required by applicable law or agreed to in writing,
 *   software distributed under the License is distributed on an
 *   "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 *   KIND, either express or implied.  See the License for the
 *   specific language governing permissions and limitations
 *   under the License.
 *
 */

package org.apache.directory.kerberos.client;


/**
 * The result codes returned by the change password server as defined in the <a href="http://www.ietf.org/rfc/rfc3244.txt">rfc3244</a>
 * 
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 */
public enum ChangePasswordResultCode
{

    /** request succeeds (This value is not allowed in a KRB-ERROR message) */
    KRB5_KPASSWD_SUCCESS(0),

    /** request fails due to being malformed */
    KRB5_KPASSWD_MALFORMED(1),

    /** 
     * request fails due to "hard" error in processing the request 
     * (for example, there is a resource or other problem causing 
     * the request to fail) 
     */
    KRB5_KPASSWD_HARDERROR(2),

    /** request fails due to an error in authentication processing */
    KRB5_KPASSWD_AUTHERROR(3),

    /** request fails due to a "soft" error in processing the request */
    KRB5_KPASSWD_SOFTERROR(4),

    /** requestor not authorized */
    KRB5_KPASSWD_ACCESSDENIED(5),

    /** protocol version unsupported */
    KRB5_KPASSWD_BAD_VERSION(6),

    /** initial flag required */
    KRB5_KPASSWD_INITIAL_FLAG_NEEDED(7),
    
    /** 0xFFFF(65535) is returned if the request fails for some other reason */
    OTHER(0xFFFF);

    private int val;


    private ChangePasswordResultCode( int val )
    {
        this.val = val;
    }


    public int getVal()
    {
        return val;
    }


    public static ChangePasswordResultCode getByValue( int code )
    {
        switch ( code )
        {
            case 0: return KRB5_KPASSWD_SUCCESS;
            
            case 1: return KRB5_KPASSWD_MALFORMED;
            
            case 2: return KRB5_KPASSWD_HARDERROR;
            
            case 3: return KRB5_KPASSWD_AUTHERROR;
            
            case 4: return KRB5_KPASSWD_SOFTERROR;
            
            case 5: return KRB5_KPASSWD_ACCESSDENIED;
            
            case 6: return KRB5_KPASSWD_BAD_VERSION;
            
            case 7: return KRB5_KPASSWD_INITIAL_FLAG_NEEDED;
            
            case 0xFFFF: return OTHER;
            
            default: throw new IllegalArgumentException( "Unknown result code " + code );
        }
    }


    @Override
    public String toString()
    {
        return super.toString() + "(" + getVal() +  ")";
    }
    
}
