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
 * An implementation of a BitString for the TicketFlags. The different values
 * are stored in an int, as there can't be more than 32 flags (TicketFlag).
 * 
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 * @version $Rev: 540371 $, $Date: 2007-05-22 02:00:43 +0200 (Tue, 22 May 2007) $
 */
public class TicketFlags extends AbstractKerberosFlags
{
    public static final long serialVersionUID = 1L;

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
        super( getBytes( flags ) );
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
    /*public static String toString( int flags )
    {
        StringBuilder result = new StringBuilder();

        if ( ( flags & ( 1 << TicketFlag.RESERVED.getOrdinal() ) ) != 0 )
        {
            result.append( "RESERVED " );
        }

        if ( ( flags & ( 1 << TicketFlag.FORWARDABLE.getOrdinal() ) ) != 0 )
        {
            result.append( "FORWARDABLE " );
        }

        if ( ( flags & ( 1 << TicketFlag.FORWARDED.getOrdinal() ) ) != 0 )
        {
            result.append( "FORWARDED " );
        }

        if ( ( flags & ( 1 << TicketFlag.PROXIABLE.getOrdinal() ) ) != 0 )
        {
            result.append( "PROXIABLE " );
        }

        if ( ( flags & ( 1 << TicketFlag.PROXY.getOrdinal() ) ) != 0 )
        {
            result.append( "PROXY " );
        }

        if ( ( flags & ( 1 << TicketFlag.MAY_POSTDATE.getOrdinal() ) ) != 0 )
        {
            result.append( "MAY_POSTDATE " );
        }

        if ( ( flags & ( 1 << TicketFlag.POSTDATED.getOrdinal() ) ) != 0 )
        {
            result.append( "POSTDATED " );
        }

        if ( ( flags & ( 1 << TicketFlag.INVALID.getOrdinal() ) ) != 0 )
        {
            result.append( "INVALID " );
        }

        if ( ( flags & ( 1 << TicketFlag.RENEWABLE.getOrdinal() ) ) != 0 )
        {
            result.append( "RENEWABLE " );
        }

        if ( ( flags & ( 1 << TicketFlag.INITIAL.getOrdinal() ) ) != 0 )
        {
            result.append( "INITIAL " );
        }

        if ( ( flags & ( 1 << TicketFlag.PRE_AUTHENT.getOrdinal() ) ) != 0 )
        {
            result.append( "PRE_AUTHENT " );
        }

        if ( ( flags & ( 1 << TicketFlag.HW_AUTHENT.getOrdinal() ) ) != 0 )
        {
            result.append( "HW_AUTHENT " );
        }

        if ( ( flags & ( 1 << TicketFlag.TRANSITED_POLICY_CHECKED.getOrdinal() ) ) != 0 )
        {
            result.append( "TRANSITED_POLICY_CHECKED " );
        }

        if ( ( flags & ( 1 << TicketFlag.OK_AS_DELEGATE.getOrdinal() ) ) != 0 )
        {
            result.append( "OPTS_OK_AS_DELEGATE " );
        }

        return result.toString().trim();
    }*/

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
