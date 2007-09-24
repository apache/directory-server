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
package org.apache.directory.server.kerberos.shared.messages.value.flags;

/**
 * The KDCOptions class. The ASN.1 grammar is :
 * 
 *  KDCOptions      ::= KerberosFlags
 *           -- reserved(0),
 *           -- forwardable(1),
 *           -- forwarded(2),
 *           -- proxiable(3),
 *           -- proxy(4),
 *           -- allow-postdate(5),
 *           -- postdated(6),
 *           -- unused7(7),
 *           -- renewable(8),
 *           -- unused9(9),
 *           -- unused10(10),
 *           -- opt-hardware-auth(11),
 *           -- unused12(12),
 *           -- unused13(13),
 *           -- 15 is reserved for canonicalize
 *           -- unused15(15),
 *           -- 26 was unused in 1510
 *           -- disable-transited-check(26),
 *           -- renewable-ok(27),
 *           -- enc-tkt-in-skey(28),
 *           -- renew(30),
 *           -- validate(31)
 *           
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 * @version $Rev: 540371 $, $Date: 2007-05-22 02:00:43 +0200 (Tue, 22 May 2007) $
 */
public class KdcOptions extends AbstractKerberosFlags
{
    public static final long serialVersionUID = 1L;
    
    /**
     * Basic constructor of a KdcOptions BitString
     */
    public KdcOptions()
    {
        super();
    }
    
    /**
     * Constructor of a KdcOptions BitString with an int value
     */
    public KdcOptions( int flags )
    {
        super( getBytes( flags ) );
    }
    
    /**
     * Basic constructor of a KdcOptions BitString with a byte array
     */
    public KdcOptions( byte[] flags )
    {
        super( flags );
    }
    
    /**
     * Converts the object to a printable string.
     */
    public String toString()
    {
        StringBuilder result = new StringBuilder();

        if ( isFlagSet( KdcOption.RESERVED ) )
        {
            result.append( "RESERVED(0) " );
        }

        if ( isFlagSet( KdcOption.FORWARDABLE ) )
        {
            result.append( "FORWARDABLE(1) " );
        }

        if ( isFlagSet( KdcOption.FORWARDED ) )
        {
            result.append( "FORWARDED(2) " );
        }
        
        if ( isFlagSet( KdcOption.PROXIABLE ) )
        {
            result.append( "PROXIABLE(3) " );
        }

        if ( isFlagSet( KdcOption.PROXY ) )
        {
            result.append( "PROXY(4) " );
        }

        if ( isFlagSet( KdcOption.ALLOW_POSTDATE ) )
        {
            result.append( "ALLOW_POSTDATE(5) " );
        }

        if ( isFlagSet( KdcOption.POSTDATED ) )
        {
            result.append( "POSTDATED(6) " );
        }

        if ( isFlagSet( KdcOption.UNUSED7 ) )
        {
            result.append( "UNUSED(7) " );
        }

        if ( isFlagSet( KdcOption.RENEWABLE ) )
        {
            result.append( "RENEWABLE(8) " );
        }

        if ( isFlagSet( KdcOption.UNUSED9 ) )
        {
            result.append( "UNUSED(9) " );
        }

        if ( isFlagSet( KdcOption.UNUSED10 ) )
        {
            result.append( "UNUSED(10) " );
        }

        if ( isFlagSet( KdcOption.OPT_HARDWARE_AUTH ) )
        {
            result.append( "OPT_HARDWARE_AUTH(11) " );
        }

        if ( isFlagSet( KdcOption.UNUSED12 ) )
        {
            result.append( "UNUSED(12) " );
        }

        if ( isFlagSet( KdcOption.UNUSED13 ) )
        {
            result.append( "UNUSED(13) " );
        }

        if ( isFlagSet( KdcOption.UNUSED15 ) )
        {
            result.append( "UNUSED(15) " );
        }

        if ( isFlagSet( KdcOption.DISABLE_TRANSISTED_CHECKED ) )
        {
            result.append( "DISABLE_TRANSISTED_CHECKED(26) " );
        }

        if ( isFlagSet( KdcOption.RENEWABLE_OK ) )
        {
            result.append( "RENEWABLE_OK(27) " );
        }

        if ( isFlagSet( KdcOption.ENC_TKT_IN_SKEY ) )
        {
            result.append( "ENC_TKT_IN_SKEY(28) " );
        }

        if ( isFlagSet( KdcOption.RENEW ) )
        {
            result.append( "RENEW(30) " );
        }

        if ( isFlagSet( KdcOption.VALIDATE ) )
        {
            result.append( "VALIDATE(31) " );
        }
        
        return result.toString().trim();
    }
}
