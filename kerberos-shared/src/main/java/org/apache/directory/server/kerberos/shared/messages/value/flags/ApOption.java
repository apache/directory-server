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
 * An enum to describe all the ApOption possible values.
 * 
 *  APOptions       ::= KerberosFlags
 *           -- reserved(0),
 *           -- use-session-key(1),
 *           -- mutual-required(2)
 *
 * 
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 * @version $Rev: 540371 $, $Date: 2007-05-22 02:00:43 +0200 (Tue, 22 May 2007) $
 */
public enum ApOption implements KerberosFlag
{
    /**
     * AP Request option - reserved
     */
    RESERVED(0),

    /**
     * AP Request option - use session key
     */
    USE_SESSION_KEY(1),

    /**
     * AP Request option - mutual authentication required
     */
    MUTUAL_REQUIRED(2),

    /**
     * AP Request option - maximum value
     */
    MAX_VALUE(32);


    // The interned value.
    private int value;
    
    /**
     * Class constructor
     */
    private ApOption( int value )
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
