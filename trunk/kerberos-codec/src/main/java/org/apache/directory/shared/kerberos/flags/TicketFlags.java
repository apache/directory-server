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
package org.apache.directory.shared.kerberos.flags;


/**
 * An implementation of a BitString for the TicketFlags. The different values
 * are stored in an int, as there can't be more than 32 flags (TicketFlag).
 * 
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 */
public class TicketFlags extends AbstractKerberosFlags
{
    private static final long serialVersionUID = 1L;


    /**
      * Basic constructor of a TicketFlags BitString
      */
    public TicketFlags()
    {
        super();
    }


    /**
     * Constructor of a TicketFlags BitString with an int value
     */
    public TicketFlags( int flags )
    {
        super( flags );
    }


    /**
     * Basic constructor of a TicketFlags BitString with a byte array
     */
    public TicketFlags( byte[] flags )
    {
        super( flags );
    }


    /**
     * Ticket flag - reserved
     */
    public boolean isReserved()
    {
        return isFlagSet( TicketFlag.RESERVED );
    }


    /**
     * Ticket flag - forwardable
     */
    public boolean isForwardable()
    {
        return isFlagSet( TicketFlag.FORWARDABLE );
    }


    /**
     * Ticket flag - forwarded
     */
    public boolean isForwarded()
    {
        return isFlagSet( TicketFlag.FORWARDED );
    }


    /**
     * Ticket flag - proxiable
     */
    public boolean isProxiable()
    {
        return isFlagSet( TicketFlag.PROXIABLE );
    }


    /**
     * Ticket flag - proxy
     */
    public boolean isProxy()
    {
        return isFlagSet( TicketFlag.PROXY );
    }


    /**
     * Ticket flag - may be postdated
     */
    public boolean isMayPosdate()
    {
        return isFlagSet( TicketFlag.MAY_POSTDATE );
    }


    /**
     * Ticket flag - postdated
     */
    public boolean isPostdated()
    {
        return isFlagSet( TicketFlag.POSTDATED );
    }


    /**
     * Ticket flag - invalid
     */
    public boolean isInvalid()
    {
        return isFlagSet( TicketFlag.INVALID );
    }


    /**
     * Ticket flag - renewable
     */
    public boolean isRenewable()
    {
        return isFlagSet( TicketFlag.RENEWABLE );
    }


    /**
     * Ticket flag - initial
     */
    public boolean isInitial()
    {
        return isFlagSet( TicketFlag.INITIAL );
    }


    /**
     * Ticket flag - pre-authentication
     */
    public boolean isPreAuth()
    {
        return isFlagSet( TicketFlag.PRE_AUTHENT );
    }


    /**
     * Ticket flag - hardware authentication
     */
    public boolean isHwAuthent()
    {
        return isFlagSet( TicketFlag.HW_AUTHENT );
    }


    /**
     * Ticket flag - transitedEncoding policy checked
     */
    public boolean isTransitedPolicyChecked()
    {
        return isFlagSet( TicketFlag.TRANSITED_POLICY_CHECKED );
    }


    /**
     * Ticket flag - OK as delegate
     */
    public boolean isOkAsDelegate()
    {
        return isFlagSet( TicketFlag.OK_AS_DELEGATE );
    }


    /**
     * Converts the object to a printable string.
     */
    public String toString()
    {
        StringBuilder result = new StringBuilder();

        if ( isFlagSet( TicketFlag.RESERVED ) )
        {
            result.append( "RESERVED(0) " );
        }

        if ( isFlagSet( TicketFlag.FORWARDABLE ) )
        {
            result.append( "FORWARDABLE(1) " );
        }

        if ( isFlagSet( TicketFlag.FORWARDED ) )
        {
            result.append( "FORWARDED(2) " );
        }

        if ( isFlagSet( TicketFlag.PROXIABLE ) )
        {
            result.append( "PROXIABLE(3) " );
        }

        if ( isFlagSet( TicketFlag.PROXY ) )
        {
            result.append( "PROXY(4) " );
        }

        if ( isFlagSet( TicketFlag.MAY_POSTDATE ) )
        {
            result.append( "MAY_POSTDATE(5) " );
        }

        if ( isFlagSet( TicketFlag.POSTDATED ) )
        {
            result.append( "POSTDATED(6) " );
        }

        if ( isFlagSet( TicketFlag.INVALID ) )
        {
            result.append( "INVALID(7) " );
        }

        if ( isFlagSet( TicketFlag.RENEWABLE ) )
        {
            result.append( "RENEWABLE(8) " );
        }

        if ( isFlagSet( TicketFlag.INITIAL ) )
        {
            result.append( "INITIAL(9) " );
        }

        if ( isFlagSet( TicketFlag.PRE_AUTHENT ) )
        {
            result.append( "PRE_AUTHENT(10) " );
        }

        if ( isFlagSet( TicketFlag.HW_AUTHENT ) )
        {
            result.append( "HW_AUTHENT(11) " );
        }

        if ( isFlagSet( TicketFlag.TRANSITED_POLICY_CHECKED ) )
        {
            result.append( "TRANSITED_POLICY_CHECKED(12) " );
        }

        if ( isFlagSet( TicketFlag.OK_AS_DELEGATE ) )
        {
            result.append( "OK_AS_DELEGATE(13) " );
        }

        return result.toString().trim();
    }
}
