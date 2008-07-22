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
package org.apache.directory.server.newldap.handlers.bind;


import org.apache.directory.server.newldap.LdapSession;
import org.apache.directory.shared.ldap.message.BindRequest;

import javax.security.sasl.SaslServer;


/**
 * A Dummy mechanism handler for Simple mechanism: not really used but needed
 * for the mechanism map.
 *
 * @org.apache.xbean.XBean
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 * @version $$Rev$$
 */
public class SimpleMechanismHandler implements MechanismHandler
{
    public SaslServer handleMechanism( LdapSession session, BindRequest bindRequest ) throws Exception
    {
        return null;
    }
}
