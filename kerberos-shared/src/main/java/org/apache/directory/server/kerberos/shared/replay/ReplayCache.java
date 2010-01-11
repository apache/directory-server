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
package org.apache.directory.server.kerberos.shared.replay;


import javax.security.auth.kerberos.KerberosPrincipal;

import org.apache.directory.server.kerberos.shared.messages.value.KerberosTime;


/**
 * "The replay cache will store at least the server name, along with the client name,
 * time, and microsecond fields from the recently-seen authenticators, and if a
 * matching tuple is found, the KRB_AP_ERR_REPEAT error is returned."
 * 
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 * @version $Rev$, $Date$
 */
public interface ReplayCache
{
    /**
     * Returns whether a request is a replay, based on the server principal, client
     * principal, time, and microseconds.
     * 
     * @param serverPrincipal The server principal 
     * @param clientPrincipal The client principal
     * @param clientTime The client time
     * @param clientMicroSeconds The client microsecond
     * @return true if the request is a replay.
     */
    boolean isReplay( KerberosPrincipal serverPrincipal, KerberosPrincipal clientPrincipal, KerberosTime clientTime,
        int clientMicroSeconds );


    /**
     * Saves the server principal, client principal, time, and microseconds to
     * the replay cache.
     *
     * @param serverPrincipal The server principal 
     * @param clientPrincipal The client principal
     * @param clientTime The client time
     * @param clientMicroSeconds The client microsecond
     */
    void save( KerberosPrincipal serverPrincipal, KerberosPrincipal clientPrincipal, KerberosTime clientTime,
        int clientMicroSeconds );
}
