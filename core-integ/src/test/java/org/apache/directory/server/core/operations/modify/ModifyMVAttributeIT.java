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


import static org.apache.directory.server.core.integ.IntegrationUtils.getSystemContext;

import javax.naming.directory.Attribute;
import javax.naming.directory.Attributes;
import javax.naming.directory.BasicAttribute;
import javax.naming.directory.BasicAttributes;
import javax.naming.directory.DirContext;
import javax.naming.ldap.LdapContext;

import org.apache.directory.server.core.annotations.ApplyLdifs;
import org.apache.directory.server.core.annotations.CreateDS;
import org.apache.directory.server.core.integ.AbstractLdapTestUnit;
import org.apache.directory.server.core.integ.FrameworkRunner;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;


/**
 * Test the modification of an entry with a MV attribute We add one new value N times
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 */
@RunWith(FrameworkRunner.class)
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
@Ignore("Ignore atm, this is a perf test")
public void testAdd1000Members() throws Exception
{
    LdapContext sysRoot = getSystemContext( getService() );

    // Add 10000 members
    Attributes attrs = new BasicAttributes( "uniqueMember", true );
    Attribute attr = new BasicAttribute( "uniqueMember" );

    for ( int i = 0; i < 10000; i++ )
    {
        String newValue = "cn=member" + i + ",ou=people,o=sevenSeas";
        attr.add( newValue );
    }

    attrs.put( attr );

    sysRoot.modifyAttributes( "cn=testing00", DirContext.ADD_ATTRIBUTE, attrs );

    System.out.println( " Done" );
}


/**
 * With this test the Master table will grow crazy.
 */
@Test
@Ignore("Ignore atm, this is a perf test")
public void testAdd500Members() throws Exception
{
    LdapContext sysRoot = getSystemContext( getService() );
    long t0 = System.currentTimeMillis();

    // Add 600 members
    for ( int i = 0; i < 100000; i++ )
    {
        if ( i % 100 == 0 )
        {
            long t1 = System.currentTimeMillis();
            long delta = ( t1 - t0 );
            System.out.println( "Done : " + i + " in " + delta + "ms" );
            t0 = t1;
        }

        String newValue = "cn=member" + i + ",ou=people,o=sevenSeas";
        Attributes attrs = new BasicAttributes( "uniqueMember", newValue, true );
        sysRoot.modifyAttributes( "cn=testing00", DirContext.ADD_ATTRIBUTE, attrs );
    }

    System.out.println( " Done" );
}
}
