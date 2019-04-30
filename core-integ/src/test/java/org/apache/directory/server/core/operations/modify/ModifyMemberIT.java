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


import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.apache.directory.api.ldap.model.entry.DefaultModification;
import org.apache.directory.api.ldap.model.entry.Modification;
import org.apache.directory.api.ldap.model.entry.ModificationOperation;
import org.apache.directory.api.ldap.model.entry.Value;
import org.apache.directory.api.ldap.model.schema.AttributeType;
import org.apache.directory.api.ldap.model.schema.SchemaManager;
import org.apache.directory.ldap.client.api.LdapConnection;
import org.apache.directory.server.core.annotations.ApplyLdifs;
import org.apache.directory.server.core.annotations.CreateDS;
import org.apache.directory.server.core.integ.AbstractLdapTestUnit;
import org.apache.directory.server.core.integ.FrameworkRunner;
import org.apache.directory.server.core.integ.IntegrationUtils;
import org.junit.Test;
import org.junit.runner.RunWith;


/**
 * Tests modify the member attribute.
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 */
@RunWith(FrameworkRunner.class)
@CreateDS(name = "ModifyMemberIT")
@ApplyLdifs(
    {
        "dn: cn=group1,ou=system",
        "objectClass: top",
        "objectClass: groupOfNames",
        "cn: group1",
        "member: cn=user1,ou=system"
    })
public class ModifyMemberIT extends AbstractLdapTestUnit
{
    @Test
    public void testAddDeleteMemberSimple() throws Exception
    {
        LdapConnection conn = IntegrationUtils.getAdminConnection( getService() );

        String dn = "cn=group1,ou=system";
        String memberAttribute = "member";
        String memberValue = "cn=user2,ou=system";

        Modification add = new DefaultModification( ModificationOperation.ADD_ATTRIBUTE,
            memberAttribute, memberValue );
        conn.modify( dn, add );
        assertTrue( conn.lookup( dn ).contains( memberAttribute, memberValue ) );

        Modification del = new DefaultModification( ModificationOperation.REMOVE_ATTRIBUTE,
            memberAttribute, memberValue );
        conn.modify( dn, del );
        assertFalse( conn.lookup( dn ).contains( memberAttribute, memberValue ) );
    }


    @Test
    public void testAddDeleteMemberComplex() throws Exception
    {
        LdapConnection conn = IntegrationUtils.getAdminConnection( getService() );

        String dn = "cn=group1,ou=system";
        String memberAttribute = "member";
        String memberValue = "cn=\\#\\\\\\+\\, \\\"\u00F6\u00E9\\\",ou=system";

        Modification add = new DefaultModification( ModificationOperation.ADD_ATTRIBUTE,
            memberAttribute, memberValue );
        conn.modify( dn, add );
        assertTrue( conn.lookup( dn ).contains( memberAttribute, memberValue ) );

        Modification del = new DefaultModification( ModificationOperation.REMOVE_ATTRIBUTE,
            memberAttribute, memberValue );
        conn.modify( dn, del );
        assertFalse( conn.lookup( dn ).contains( memberAttribute, memberValue ) );
    }


    @Test
    public void testDnValueEquality() throws Exception
    {
        SchemaManager schemaManager = getService().getSchemaManager();
        AttributeType memberAT = schemaManager.getAttributeType( "member" );
        String dn = "cn=\\#\\\\\\+\\, \\\"\u00F6\u00E9\\\",ou=system";
        Value value1 = new Value( memberAT, dn );
        Value value2 = new Value( memberAT, dn );
        assertTrue( value1.equals( value2 ) );
    }

}
