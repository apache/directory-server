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
package org.apache.directory.server.core.prefs;


import javax.naming.NamingException;

import org.apache.directory.server.core.prefs.PreferencesUtils;
import org.apache.directory.shared.ldap.name.LdapDN;

import junit.framework.TestCase;


/**
 * Test caseses for preference utility methods.
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 * @version $Rev$
 */
public class PreferencesUtilsTest extends TestCase
{
    /**
     * Tests to confirm the toSysDn() method can translate an absolute
     * preference node path into an LDAP distinguished name.
     *
     * @throws NamingException if there are problems transforming the name
     */
    public void testToSysDn() throws NamingException
    {
        // simple test

        String test = "/org/apache/kerberos/";

        LdapDN dn = ( LdapDN ) PreferencesUtils.toSysDn( test );

        assertEquals( dn.toUpName(), "prefNodeName=kerberos,prefNodeName=apache,prefNodeName=org,"
            + PreferencesUtils.SYSPREF_BASE );

        // simple test without trailing '/'

        test = "/org/apache/kerberos";

        dn = ( LdapDN ) PreferencesUtils.toSysDn( test );

        assertEquals( dn.toUpName(), "prefNodeName=kerberos,prefNodeName=apache,prefNodeName=org,"
            + PreferencesUtils.SYSPREF_BASE );

        // basis condition tests

        test = "/";

        dn = ( LdapDN ) PreferencesUtils.toSysDn( test );

        assertEquals( dn.toUpName(), PreferencesUtils.SYSPREF_BASE );

        // endpoint tests

        test = "//////";

        dn = ( LdapDN ) PreferencesUtils.toSysDn( test );

        assertEquals( dn.toUpName(), PreferencesUtils.SYSPREF_BASE );

    }
}
