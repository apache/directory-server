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
package org.apache.directory.server.core.normalization;


import static org.apache.directory.server.core.integ.IntegrationUtils.getSystemContext;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import javax.naming.directory.Attribute;
import javax.naming.directory.Attributes;
import javax.naming.directory.BasicAttributes;
import javax.naming.ldap.LdapContext;

import org.apache.directory.server.core.annotations.CreateDS;
import org.apache.directory.server.core.integ.AbstractLdapTestUnit;
import org.apache.directory.server.core.integ.FrameworkRunner;
import org.junit.Test;
import org.junit.runner.RunWith;



/**
 * Test cases for the normalization service.
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 */
@RunWith ( FrameworkRunner.class )
@CreateDS(name = "NormalizationServiceIT")
public final class NormalizationServiceIT extends AbstractLdapTestUnit
{

    @Test
    public void testDireve308Example() throws Exception
    {
        /*

        Use @Ldif to load this data but for now we can do it with code.

dn: ou=direct report view,ou=system
objectClass: organizationalUnit
ou: direct report view

dn: ou=corporate category\, operations,ou=direct report view,ou=system
objectClass: organizationalUnit
ou: corporate category\, operations

         */

        LdapContext sysRoot = getSystemContext( getService() );

        Attributes attrs = new BasicAttributes( "objectClass", "organizationalUnit", true );
        attrs.put( "ou", "direct report view" );
        sysRoot.createSubcontext( "ou=direct report view", attrs );

        attrs = new BasicAttributes( "objectClass", "organizationalUnit", true );
        attrs.put( "ou", "corporate category\\, operations" );
        sysRoot.createSubcontext( "ou=corporate category\\, operations,ou=direct report view", attrs );

        attrs = sysRoot.getAttributes( "ou=corporate category\\, operations,ou=direct report view" );
        assertNotNull( attrs );
        Attribute ou = attrs.get( "ou" );
        assertEquals( "corporate category, operations", ou.get() );
        Attribute oc = attrs.get( "objectClass" );
        assertTrue( oc.contains( "top" ) );
        assertTrue( oc.contains( "organizationalUnit" ) );
    }
}
