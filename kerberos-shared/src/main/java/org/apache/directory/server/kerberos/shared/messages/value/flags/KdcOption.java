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
 * An enum to describe all the KdcOption possible values.
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
public enum KdcOption implements KerberosFlag
{
    /**
     * KDC option - reserved.
     */
    RESERVED(0),
    /**
     * KDC option - forwardable.
     */
    FORWARDABLE(1),

    /**
     * KDC option - forwarded.
     */
    FORWARDED(2),

    /**
     * KDC option - proxiable.
     */
    PROXIABLE(3),

    /**
     * KDC option - proxy.
     */
    PROXY(4),

    /**
     * KDC option - allow postdate.
     */
    ALLOW_POSTDATE(5),

    /**
     * KDC option - postdated.
     */
    POSTDATED(6),

    /**
     * KDC option - unused7.
     */
    UNUSED7(7),

    /**
     * KDC option - renewable.
     */
    RENEWABLE(8),

    /**
     * KDC option - unused9.
     */
    UNUSED9(9),

    /**
     * KDC option - unused10.
     */
    UNUSED10(10),

    /**
     * KDC option - unused11.
     */
    OPT_HARDWARE_AUTH(11),

    /**
     * KDC option - unused12.
     */
    UNUSED12(12),

    /**
     * KDC option - unused13.
     */
    UNUSED13(13),

    /**
     * KDC option - unused15.
     */
    UNUSED15(15),

    /**
     * KDC option - disable transisted checked.
     */
    DISABLE_TRANSISTED_CHECKED(26),

    /**
     * KDC option - renewable is ok.
     */
    RENEWABLE_OK(27),

    /**
     * KDC option - encrypted key in skey.
     */
    ENC_TKT_IN_SKEY(28),

    /**
     * KDC option - renew.
     */
    RENEW(30),

    /**
     * KDC option - validate.
     */
    VALIDATE(31),

    /**
     * KDC option - maximum value.
     */
    MAX_VALUE(32);

    // The interned value.
    private int value;
    
    /**
     * Class constructor
     */
    private KdcOption( int value )
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
