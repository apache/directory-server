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

public class TicketFlags extends Options
{
	// Ticket flag - reserved
	public static final int RESERVED                 = 0;
	// Ticket flag - forwardable
	public static final int FORWARDABLE              = 1;
	// Ticket flag - forwarded
	public static final int FORWARDED                = 2;
	// Ticket flag - proxiable
	public static final int PROXIABLE                = 3;
	// Ticket flag - proxy
	public static final int PROXY                    = 4;
	// Ticket flag - may be postdated
	public static final int MAY_POSTDATE             = 5;
	// Ticket flag - postdated
	public static final int POSTDATED                = 6;
	// Ticket flag - invalid
	public static final int INVALID                  = 7;
	// Ticket flag - renewable
	public static final int RENEWABLE                = 8;
	// Ticket flag - initial
	public static final int INITIAL                  = 9;
	// Ticket flag - pre-authentication
	public static final int PRE_AUTHENT              = 10;
	// Ticket flag - hardware authentication
	public static final int HW_AUTHENT               = 11;
	// Ticket flag - transitedEncoding policy checked
	public static final int TRANSITED_POLICY_CHECKED = 12;
	// Ticket flag - OK as delegate
	public static final int OK_AS_DELEGATE           = 13;

	// Ticket flag - maximum value
	public static final int MAX_VALUE                = 32;

	/**
     * Class constructor
     */
    public TicketFlags()
    {
        super( MAX_VALUE );
    }

    public TicketFlags( byte[] options )
    {
        super( MAX_VALUE );
        setBytes( options );
    }

    /**
     * Converts the object to a printable string
     */
    public String toString()
    {
        StringBuffer result = new StringBuffer();

        if ( get( FORWARDABLE ) )
        {
            result.append( "FORWARDABLE " );
        }

        if ( get( FORWARDED ) )
        {
            result.append( "FORWARDED " );
        }

        if ( get( PROXIABLE ) )
        {
            result.append( "PROXIABLE " );
        }

        if ( get( PROXY ) )
        {
            result.append( "PROXY " );
        }

        if ( get( MAY_POSTDATE ) )
        {
            result.append( "MAY_POSTDATE " );
        }

        if ( get( POSTDATED ) )
        {
            result.append( "POSTDATED " );
        }

        if ( get( INVALID ) )
        {
            result.append( "INVALID " );
        }

        if ( get( RENEWABLE ) )
        {
            result.append( "RENEWABLE " );
        }

        if ( get( INITIAL ) )
        {
            result.append( "INITIAL " );
        }

        if ( get( PRE_AUTHENT ) )
        {
            result.append( "PRE_AUTHENT " );
        }

        if ( get( HW_AUTHENT ) )
        {
            result.append( "HW_AUTHENT " );
        }

        if ( get( TRANSITED_POLICY_CHECKED ) )
        {
            result.append( "TRANSITED_POLICY_CHECKED " );
        }

        if ( get( OK_AS_DELEGATE ) )
        {
            result.append( "OPTS_OK_AS_DELEGATE " );
        }

        return result.toString().trim();
    }
}
