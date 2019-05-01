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
package org.apache.directory.server.core.operations.modify;


import static org.junit.Assert.assertEquals;

import org.apache.directory.api.ldap.model.entry.Attribute;
import org.apache.directory.api.ldap.model.entry.DefaultAttribute;
import org.apache.directory.api.ldap.model.entry.DefaultModification;
import org.apache.directory.api.ldap.model.entry.Entry;
import org.apache.directory.api.ldap.model.entry.Modification;
import org.apache.directory.api.ldap.model.entry.ModificationOperation;
import org.apache.directory.api.ldap.model.message.ModifyRequest;
import org.apache.directory.api.ldap.model.message.ModifyRequestImpl;
import org.apache.directory.api.ldap.model.name.Dn;
import org.apache.directory.ldap.client.api.LdapConnection;
import org.apache.directory.server.core.annotations.ApplyLdifs;
import org.apache.directory.server.core.annotations.CreateDS;
import org.apache.directory.server.core.integ.AbstractLdapTestUnit;
import org.apache.directory.server.core.integ.FrameworkRunner;
import org.apache.directory.server.core.integ.IntegrationUtils;
import org.junit.Test;
import org.junit.runner.RunWith;
 

/**
 * Tests the modify() methods of the provider, for the Increment operation.
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 */
@RunWith ( FrameworkRunner.class )
@CreateDS(name = "ModifyAddIT")
@ApplyLdifs(
    {
        "dn: m-oid=2.2.0, ou=attributeTypes, cn=apachemeta, ou=schema",
        "objectclass: metaAttributeType",
        "objectclass: metaTop",
        "objectclass: top",
        "m-oid: 2.2.0",
        "m-name: integerAttribute",
        "m-description: the precursor for all integer attributes",
        "m-equality: integerMatch",
        "m-ordering: integerOrderingMatch",
        "m-syntax: 1.3.6.1.4.1.1466.115.121.1.27",
        "m-length: 0",
        "",
        "dn: ou=testing00,ou=system",
        "objectClass: top",
        "objectClass: organizationalUnit",
        "objectClass: extensibleObject",
        "ou: testing00",
        "integerAttribute: 0",
        "",
        "dn: ou=testing01,ou=system",
        "objectClass: top",
        "objectClass: organizationalUnit",
        "objectClass: extensibleObject",
        "ou: testing01",
        "integerAttribute: 2147483647",     // Integer.MAX_VALUE
    }
)
public class ModifyIncrementIT extends AbstractLdapTestUnit
{
    //---------------------------------------------------------------------------------------------
    // Increment operation
    //---------------------------------------------------------------------------------------------
    // 1 Entry exists
    //  1.1 AT does not exist.
    //  1.2 AT exists but is not an Integer
    //  1.3 AT exists and is an Integer
    //  1.4 AT exists and is an Integer, but its value is INTEGER.MAX
    //---------------------------------------------------------------------------------------------
    @Test
    public void testModifyIncrement() throws Exception
    {
        LdapConnection connection = IntegrationUtils.getAdminConnection( getService() );
        Dn dn = new Dn( "ou=testing00,ou=system" );

        ModifyRequest modRequest = new ModifyRequestImpl();
        modRequest.setName( dn );
        Modification modification = new DefaultModification();
        Attribute attribute = new DefaultAttribute( "integerAttribute" );

        modification.setAttribute( attribute );
        modification.setOperation( ModificationOperation.INCREMENT_ATTRIBUTE );
        modRequest.addModification( modification );

        connection.modify( modRequest );

        // Verify that the attribute value has been incremented
        Entry modified = connection.lookup( dn );
        
        assertEquals( "1", modified.get( "integerAttribute" ).getString() );

        // Do it again 10 times
        for ( int i=0; i < 10; i++ ) 
        {
            connection.modify( modRequest );
        }

        modified = connection.lookup( dn );
        
        assertEquals( "11", modified.get( "integerAttribute" ).getString() );
    }
    
    
    @Test
    public void testModifyIncrementNumber() throws Exception
    {
        LdapConnection connection = IntegrationUtils.getAdminConnection( getService() );
        Dn dn = new Dn( "ou=testing00,ou=system" );

        ModifyRequest modRequest = new ModifyRequestImpl();
        modRequest.setName( dn );
        Modification modification = new DefaultModification();
        Attribute attribute = new DefaultAttribute( "integerAttribute", "3" );

        modification.setAttribute( attribute );
        modification.setOperation( ModificationOperation.INCREMENT_ATTRIBUTE );
        modRequest.addModification( modification );

        connection.modify( modRequest );

        // Verify that the attribute value has been incremented
        Entry modified = connection.lookup( dn );
        
        assertEquals( "3", modified.get( "integerAttribute" ).getString() );

        // Do it again 10 times
        for ( int i=0; i < 10; i++ ) 
        {
            connection.modify( modRequest );
        }

        modified = connection.lookup( dn );
        
        assertEquals( "33", modified.get( "integerAttribute" ).getString() );
    }
}
