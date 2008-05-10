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
package org.apache.directory.server.ldap;


import org.junit.Test;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import org.apache.directory.server.ldap.handlers.extended.StartTlsHandler;
import org.apache.directory.server.ldap.handlers.bind.MechanismHandler;
import org.apache.directory.server.ldap.handlers.bind.SimpleMechanismHandler;
import org.apache.directory.shared.ldap.constants.SupportedSaslMechanisms;

import javax.naming.NamingException;
import java.util.List;
import java.util.ArrayList;
import java.util.Map;
import java.util.HashMap;


/**
 * Test to confirm correct behavoir for settings on LdapServer bean.
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 * @version $$Rev$$
 */
public class LdapServerSettingsTest
{
    @Test
    public void testAddExtendedOperationHandler() throws NamingException
    {
        LdapServer server = new LdapServer();
        StartTlsHandler handler = new StartTlsHandler();
        server.addExtendedOperationHandler( handler );
        assertEquals( handler, server.getExtendedOperationHandler( handler.getOid() ) );
        server.removeExtendedOperationHandler( handler.getOid() );
        assertNull( server.getExtendedOperationHandler( handler.getOid() ) );
    }


    @Test
    public void testSetExtendedOperationHandlers()
    {
        LdapServer server = new LdapServer();
        StartTlsHandler handler = new StartTlsHandler();
        List<ExtendedOperationHandler> handlers = new ArrayList<ExtendedOperationHandler>();
        handlers.add( handler );
        server.setExtendedOperationHandlers( handlers );
        assertEquals( handler, server.getExtendedOperationHandler( handler.getOid() ) );
        server.removeExtendedOperationHandler( handler.getOid() );
        assertNull( server.getExtendedOperationHandler( handler.getOid() ) );
    }


    @Test
    public void testSetSaslMechanismHandlers()
    {
        LdapServer server = new LdapServer();
        Map<String, MechanismHandler> handlers = new HashMap<String,MechanismHandler>();
        MechanismHandler handler = new SimpleMechanismHandler();
        handlers.put( SupportedSaslMechanisms.SIMPLE, handler );
        server.setSaslMechanismHandlers( handlers );
        assertEquals( handler, server.getMechanismHandler( SupportedSaslMechanisms.SIMPLE ) );
        assertTrue( server.getSupportedMechanisms().contains( SupportedSaslMechanisms.SIMPLE ) );
        server.removeSaslMechanismHandler( SupportedSaslMechanisms.SIMPLE );
        assertNull( server.getMechanismHandler( SupportedSaslMechanisms.SIMPLE ) );
    }
}
