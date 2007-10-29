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
 * An enum to describe all the TicketFlag possible values.
 * 
 *  TicketFlags     ::= KerberosFlags
 *           -- reserved(0),
 *           -- forwardable(1),
 *           -- forwarded(2),
 *           -- proxiable(3),
 *           -- proxy(4),
 *           -- may-postdate(5),
 *           -- postdated(6),
 *           -- invalid(7),
 *           -- renewable(8),
 *           -- initial(9),
 *           -- pre-authent(10),
 *           -- hw-authent(11),
 *       -- the following are new since 1510
 *           -- transited-policy-checked(12),
 *           -- ok-as-delegate(13)
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 * @version $Rev: 540371 $, $Date: 2007-05-22 02:00:43 +0200 (Tue, 22 May 2007) $
 */
public enum TicketFlag implements KerberosFlag
{
    /**
     * Ticket flag - reserved
     */
    RESERVED(0),

    /**
     * Ticket flag - forwardable
     */
    FORWARDABLE(1),

    /**
     * Ticket flag - forwarded
     */
    FORWARDED(2),

    /**
     * Ticket flag - proxiable
     */
    PROXIABLE(3),

    /**
     * Ticket flag - proxy
     */
    PROXY(4),

    /**
     * Ticket flag - may be postdated
     */
    MAY_POSTDATE(5),

    /**
     * Ticket flag - postdated
     */
    POSTDATED(6),
    /**
     * Ticket flag - invalid
     */
    INVALID(7),

    /**
     * Ticket flag - renewable
     */
    RENEWABLE(8),

    /**
     * Ticket flag - initial
     */
    INITIAL(9),

    /**
     * Ticket flag - pre-authentication
     */
    PRE_AUTHENT(10),

    /**
     * Ticket flag - hardware authentication
     */
    HW_AUTHENT(11),

    /**
     * Ticket flag - transitedEncoding policy checked
     */
    TRANSITED_POLICY_CHECKED(12),

    /**
     * Ticket flag - OK as delegate
     */
    OK_AS_DELEGATE(13),

    /**
     * Ticket flag - maximum value
     */
    MAX_VALUE(32);

    // The interned value.
    private int value;
    
    /**
     * Class constructor
     */
    private TicketFlag( int value )
    {
        this.value = value;
    }
    
    /**
     * @return The ordinal value associated with this flag
     */
    public int getOrdinal()
    {
        return value;
    }
}
