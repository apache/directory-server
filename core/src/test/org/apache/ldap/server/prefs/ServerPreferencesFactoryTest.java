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
package org.apache.ldap.server.prefs;


import java.util.Hashtable;
import java.util.prefs.Preferences;
import java.io.File;
import java.io.IOException;
import javax.naming.ldap.LdapContext;
import javax.naming.ldap.InitialLdapContext;
import javax.naming.NamingException;
import javax.naming.Context;
import javax.naming.InitialContext;

import junit.framework.TestCase;
import org.apache.ldap.server.jndi.EnvKeys;
import org.apache.ldap.server.jndi.AbstractJndiTest;
import org.apache.apseda.listener.AvailablePortFinder;
import org.apache.commons.io.FileUtils;


/**
 * Test cases for the server PreferencessFactory.
 *
 * @author <a href="mailto:directory-dev@incubator.apache.org">Apache Directory Project</a>
 * @version $Rev$
 */
public class ServerPreferencesFactoryTest extends AbstractJndiTest
{
    public void testSystemRoot()
    {
        ServerPreferencesFactory factory = new ServerPreferencesFactory();

        Preferences prefs = factory.systemRoot();

        assertNotNull( prefs );

        assertEquals( "abc123", prefs.get( "test", "blah" ) );
    }
}
