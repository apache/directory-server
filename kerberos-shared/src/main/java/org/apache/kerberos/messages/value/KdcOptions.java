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
package org.apache.kerberos.messages.value;

public class KdcOptions extends Options
{
    // KDC option - reserved
	public static final int RESERVED                    = 0;
    // KDC option - forwardable
	public static final int FORWARDABLE                 = 1;
    // KDC option - forwarded
	public static final int FORWARDED                   = 2;
    // KDC option - proxiable
	public static final int PROXIABLE                   = 3;
    // KDC option - proxy
	public static final int PROXY                       = 4;
    // KDC option - allow postdate
	public static final int ALLOW_POSTDATE              = 5;
    // KDC option - postdated
	public static final int POSTDATED                   = 6;
    // KDC option - unused7
	public static final int UNUSED7                     = 7;
    // KDC option - renewable
	public static final int RENEWABLE                   = 8;
    // KDC option - unused9
	public static final int UNUSED9                     = 9;
    // KDC option - unused10
	public static final int UNUSED10                    = 10;
    // KDC option - unused11
	public static final int UNUSED11                    = 11;
    // KDC option - unused12
	public static final int UNUSED12                    = 12;
    // KDC option - unused13
	public static final int UNUSED13                    = 13;
    // KDC option - disable transisted checked
	public static final int DISABLE_TRANSISTED_CHECKED  = 26;
    // KDC option - renewable is ok
	public static final int RENEWABLE_OK                = 27;
    // KDC option - encrypted key in skey
	public static final int ENC_TKT_IN_SKEY             = 28;
    // KDC option - renew
	public static final int RENEW                       = 30;
    // KDC option - validate
	public static final int VALIDATE                    = 31;

    // KDC option - maximum value
	public static final int MAX_VALUE                   = 32;

	/**
     * Class constructors
     */
    public KdcOptions()
    {
        super( MAX_VALUE );
    }

    public KdcOptions( byte[] bytes )
    {
        super( MAX_VALUE );
        setBytes( bytes );
    }

    /**
     * Converts the object to a printable string
     */
    public String toString()
    {
        StringBuffer result = new StringBuffer();

        if ( get( ALLOW_POSTDATE ) )
        {
            result.append( "ALLOW_POSTDATE " );
        }

        if ( get( DISABLE_TRANSISTED_CHECKED ) )
        {
            result.append( "DISABLE_TRANSISTED_CHECKED " );
        }

        if ( get( ENC_TKT_IN_SKEY ) )
        {
            result.append( "ENC_TKT_IN_SKEY " );
        }

        if ( get( FORWARDABLE ) )
        {
            result.append( "FORWARDABLE " );
        }

        if ( get( FORWARDED ) )
        {
            result.append( "FORWARDED " );
        }

        if ( get( POSTDATED ) )
        {
            result.append( "POSTDATED " );
        }

        if ( get( PROXIABLE ) )
        {
            result.append( "PROXIABLE " );
        }

        if ( get( PROXY ) )
        {
            result.append( "PROXY " );
        }

        if ( get( RENEW ) )
        {
            result.append( "RENEW " );
        }

        if ( get( RENEWABLE ) )
        {
            result.append( "RENEWABLE " );
        }

        if ( get( RENEWABLE_OK ) )
        {
            result.append( "RENEWABLE_OK " );
        }

        if ( get( RESERVED ) )
        {
            result.append( "RESERVED " );
        }

        if ( get( VALIDATE ) )
        {
            result.append( "VALIDATE " );
        }

        return result.toString().trim();
    }
}
