/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.directory.server.core.integ;


import javax.naming.directory.Attributes;
import javax.naming.ldap.LdapContext;

import org.apache.directory.server.core.DirectoryService;
import org.apache.directory.server.core.authn.LdapPrincipal;
import org.apache.directory.server.core.integ.annotations.Mode;
import org.apache.directory.shared.ldap.constants.AuthenticationLevel;
import org.apache.directory.shared.ldap.message.AttributesImpl;
import org.apache.directory.shared.ldap.name.LdapDN;

import org.junit.Test;
import org.junit.runner.RunWith;


import static org.junit.Assert.assertNotNull;


/**
 * Document me!
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 * @version $Rev$, $Date$
 */
@RunWith( CiRunner.class)
@Mode ( SetupMode.ROLLBACK )
public class SampleITest
{
    static DirectoryService service;
    
    static LdapContext root;
    
    
    public void createTestOu() throws Exception
    {
        LdapDN adminDn = new LdapDN( "uid=admin,ou=system" );
        adminDn.normalize( service.getRegistries().getAttributeTypeRegistry().getNormalizerMapping() );
        root = service.getJndiContext( new LdapPrincipal( adminDn, AuthenticationLevel.SIMPLE ) );
        Attributes attrs = new AttributesImpl( "objectClass", "organizationalUnit", true );
        attrs.put( "ou", "test" );
        root.createSubcontext( "ou=test,ou=system", attrs );
    }
    

    @Test public void checkService0() throws Exception
    {
        createTestOu();
        assertNotNull( service );
    }

    
    @Test public void checkService1() throws Exception
    {
        createTestOu();
        assertNotNull( service );
    }


    @Test public void checkService2() throws Exception
    {
        createTestOu();
        assertNotNull( service );
    }
}
