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

import org.apache.directory.api.ldap.model.entry.Attribute;
import org.apache.directory.api.ldap.model.entry.DefaultAttribute;
import org.apache.directory.api.ldap.model.entry.DefaultModification;
import org.apache.directory.api.ldap.model.entry.Entry;
import org.apache.directory.api.ldap.model.entry.ModificationOperation;
import org.apache.directory.ldap.client.api.LdapConnection;
import org.apache.directory.server.core.annotations.ApplyLdifs;
import org.apache.directory.server.core.annotations.CreateDS;
import org.apache.directory.server.core.integ.AbstractLdapTestUnit;
import org.apache.directory.server.core.integ.ApacheDSTestExtension;
import org.apache.directory.server.core.integ.IntegrationUtils;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

/**
 * Test the modification of an entry with a MV attribute We add one new value N times
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 */
@ExtendWith( { ApacheDSTestExtension.class } )
@CreateDS(name = "ModifyMVAttributeIT")
@ApplyLdifs(
    {
        "dn: cn=testing00,ou=system",
        "objectClass: top",
        "objectClass: groupOfUniqueNames",
        "cn: testing00",
        "uniqueMember: cn=Horatio Hornblower,ou=people,o=sevenSeas",
        "uniqueMember: cn=William Bush,ou=people,o=sevenSeas",
        "uniqueMember: cn=Thomas Quist,ou=people,o=sevenSeas",
        "uniqueMember: cn=Moultrie Crystal,ou=people,o=sevenSeas"

    })
public class ModifyMVAttributeIT extends AbstractLdapTestUnit
{
    /**
     * With this test the Master table will grow linearily.
     */
    @Test
    @Disabled( "Ignore atm, this is a perf test" )
    public void testAdd1000Members() throws Exception
    {
        try ( LdapConnection conn = IntegrationUtils.getAdminConnection( getService() ) )
        {
            // Add 10000 members
            Attribute members = new DefaultAttribute( "uniqueMember" );
    
            for ( int i = 0; i < 10000; i++ )
            {
                String newValue = "cn=member" + i + ",ou=people,o=sevenSeas";
                members.add( newValue );
            }
    
            conn.modify( "cn=testing00,ou=system", 
                new DefaultModification( ModificationOperation.ADD_ATTRIBUTE
                    , members ) );
            
            Entry entry = conn.lookup( "cn=testing00,ou=system" );
            System.out.println(" Done, " + entry );
        }
    }

    
    /**
     * With this test the Master table will grow crazy.
     */
    @Test
    @Disabled( "Ignore atm, this is a perf test" )
    public void testAdd500Members() throws Exception
    {
        try ( LdapConnection conn = IntegrationUtils.getAdminConnection( getService() ) )
        {
            long t0 = System.currentTimeMillis();
            
            // Add 500 members
            for ( int i = 0; i < 500; i++ )
            {
                if ( i% 100 == 0)
                {
                    long t1 = System.currentTimeMillis();
                    long delta = ( t1 - t0 );
                    System.out.println( "Done : " + i + " in " + delta + "ms" );
                    t0 = t1;
                }
                
                conn.modify( "cn=testing00,ou=system", 
                    new DefaultModification( ModificationOperation.ADD_ATTRIBUTE
                        , "uniqueMember", "cn=member" + i + ",ou=people,o=sevenSeas" ) );
            }
    
            System.out.println(" Done" );
        }
    }
}
