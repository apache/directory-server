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
package org.apache.directory.server.ldap.support.bind;


import javax.security.sasl.SaslServer;

import org.apache.mina.common.IoSession;


/**
 * An interface for retrieving a {@link SaslServer} for a session.
 * 
 * @see javax.security.sasl.SaslServer
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 * @version $Rev$, $Date$
 */
public interface MechanismHandler
{
    /**
     * A key constant ({@value}) for storing the SASL context in the session.
     */
    public static final String SASL_CONTEXT = "saslContext";


    /**
     * Implementors will use the session and message to determine what kind of
     * {@link SaslServer} to create and what initialization parameters it will require.
     *
     * @param session
     * @param message
     * @return The {@link SaslServer} to use for the duration of the bound session.
     * @throws Exception
     */
    public SaslServer handleMechanism( IoSession session, Object message ) throws Exception;
}
