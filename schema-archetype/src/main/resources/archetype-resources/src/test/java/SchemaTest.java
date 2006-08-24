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
package ${groupId};


import javax.naming.NamingEnumeration;
import javax.naming.NamingException;
import javax.naming.directory.Attribute;
import javax.naming.directory.Attributes;
import javax.naming.directory.BasicAttributes;
import javax.naming.directory.DirContext;
import javax.naming.directory.InitialDirContext;

import org.apache.directory.server.unit.AbstractServerTest;

import java.util.HashSet;
import java.util.Hashtable;
import java.util.Set;


/**
 * Verify the use of our new schema and test that we can create entries using it.  
 * 
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 * @version $Rev$
 */
public class SchemaTest extends AbstractServerTest
{
    private DirContext ctx = null;


    /**
     * Make sure embedded instance uses our schema and get a connection to 
     * the embedded server using the SUN JNDI LDAP provider over the wire.
     */
    public void setUp() throws Exception
    {
        // -------------------------------------------------------------------
        // Setup our schema to load       
        // -------------------------------------------------------------------

        Set schemas = configuration.getBootstrapSchemas();
        Set newset = new HashSet();
        newset.addAll( schemas );
        newset.add( new CarSchema() );
        configuration.setBootstrapSchemas( newset );
        
        super.setUp();

        // -------------------------------------------------------------------
        // Get a connection (JNDI context) to the LDAP server       
        // -------------------------------------------------------------------

        Hashtable env = new Hashtable();
        env.put( "java.naming.factory.initial", "com.sun.jndi.ldap.LdapCtxFactory" );
        env.put( "java.naming.provider.url", "ldap://localhost:" + port + "/ou=system" );
        env.put( "java.naming.security.principal", "uid=admin,ou=system" );
        env.put( "java.naming.security.credentials", "secret" );
        env.put( "java.naming.security.authentication", "simple" );
        ctx = new InitialDirContext( env );
    }


    /**
     * Closes our context before shuting down the server.
     */
    public void tearDown() throws Exception
    {
        ctx.close();
        super.tearDown();
    }


    /**
     * Let's test to make sure our schema elements have been installed and are
     * ready to be used.
     */
    public void testSchemaPresence() throws NamingException
    {
        Attributes attrs = ctx.getAttributes( "cn=schema", new String[] { "attributeTypes", "objectClasses" } );

        // -------------------------------------------------------------------
        // Check that our 3 attributeTypes are present in cn=schema,ou=system
        // -------------------------------------------------------------------
        
        Attribute attributeTypes = attrs.get( "attributeTypes" );
        boolean isCarMakeFound = false;
        boolean isCarModelFound = false;
        boolean isCarYearFound = false;
        for ( NamingEnumeration ii = attributeTypes.getAll(); ii.hasMore(); /**/ )
        {
            String id = ( String ) ii.next();

            if ( id.indexOf( "1.2.6.1.4.1.18060.1.1.1.3.1001" ) != -1 )
            {
                isCarMakeFound = true;
            }
            
            if ( id.indexOf( "1.2.6.1.4.1.18060.1.1.1.3.1002" ) != -1 )
            {
                isCarModelFound = true;
            }
            
            if ( id.indexOf( "1.2.6.1.4.1.18060.1.1.1.3.1003" ) != -1 )
            {
                isCarYearFound = true;
            }
        }
        assertTrue( isCarMakeFound );
        assertTrue( isCarModelFound );
        assertTrue( isCarYearFound );
        
        // -------------------------------------------------------------------
        // Check that our 1 objectClass is present in cn=schema,ou=system
        // -------------------------------------------------------------------
        
        Attribute objectClasses = attrs.get( "objectClasses" );
        boolean isCarFound = false;
        for ( NamingEnumeration ii = objectClasses.getAll(); ii.hasMore(); /**/ )
        {
            String id = ( String ) ii.next();

            if ( id.indexOf( "1.2.6.1.4.1.18060.1.1.1.4.1001" ) != -1 )
            {
                isCarFound = true;
            }
        }
        assertTrue( isCarFound );
    }


    /**
     * Adds a Car objectClass to the DIT under ou=system.
     */
    public void testAddCar() throws NamingException
    {
        Attributes newCar = new BasicAttributes( "objectClass", "car", true );
        newCar.put( "carMake", "Porsche" );
        newCar.put( "carModel", "Boxter" );
        newCar.put( "carYear", "2006" );
        ctx.createSubcontext( "carMake=Porsche", newCar );
        
        Attributes porsche = ctx.getAttributes( "carMake=Porsche" );
        assertNotNull( porsche );
        assertEquals( "Porsche", porsche.get( "carMake" ).get() );
        assertEquals( "Boxter", porsche.get( "carModel" ).get() );
        assertEquals( "2006", porsche.get( "carYear" ).get() );
    }
}
