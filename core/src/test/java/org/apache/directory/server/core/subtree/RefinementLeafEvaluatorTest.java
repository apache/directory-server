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
package org.apache.directory.server.core.subtree;


import javax.naming.NamingException;

import org.apache.directory.server.core.entry.DefaultServerAttribute;
import org.apache.directory.server.core.entry.ServerAttribute;
import org.apache.directory.server.core.subtree.RefinementLeafEvaluator;
import org.apache.directory.shared.ldap.entry.client.ClientStringValue;
import org.apache.directory.shared.ldap.filter.EqualityNode;
import org.apache.directory.shared.ldap.filter.GreaterEqNode;
import org.apache.directory.shared.ldap.schema.AttributeType;
import org.apache.directory.shared.ldap.schema.registries.OidRegistry;
import org.apache.directory.shared.ldap.schema.registries.Registries;
import org.apache.directory.shared.schema.loader.ldif.JarLdifSchemaLoader;
import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;


/**
 * Unit test cases for testing the evaluator for refinement leaf nodes.
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 * @version $Rev$
 */
public class RefinementLeafEvaluatorTest
{
    /** The ObjectClass AttributeType */
    private static AttributeType OBJECT_CLASS;
    
    /** the registries */
    private static Registries registries;

    /** the refinement leaf evaluator to test */
    private RefinementLeafEvaluator evaluator;


    /**
     * Initializes the global registries.
     * @throws javax.naming.NamingException if there is a failure loading the schema
     */
    @BeforeClass public static void init() throws Exception
    {
        registries = new Registries();
        JarLdifSchemaLoader loader = new JarLdifSchemaLoader();
        loader.loadAllEnabled( registries );
        OBJECT_CLASS = registries.getAttributeTypeRegistry().lookup( "objectClass" );
    }
    

    /**
     * Initializes registries and creates the leaf evalutator
     * @throws Exception if there are schema initialization problems
     */
    @Before public void setUp() throws Exception
    {
        OidRegistry registry = registries.getOidRegistry();
        evaluator = new RefinementLeafEvaluator( registry );
    }


    /**
     * Sets evaluator and registries to null.
     */
    @After public void tearDown()
    {
        evaluator = null;
    }


    /**
     * Test cases for various bad combinations of arguments
     * @throws Exception if something goes wrongg
     */
    @Test public void testForBadArguments() throws Exception
    {
        ServerAttribute objectClasses = null;

        try
        {
            assertFalse( evaluator.evaluate( null, null ) );
            fail( "should never get here due to an IAE" );
        }
        catch ( IllegalArgumentException iae )
        {
        }

        try
        {
            assertFalse( evaluator.evaluate( new GreaterEqNode( "", new ClientStringValue( "" ) ), objectClasses ) );
            fail( "should never get here due to an NE" );
        }
        catch ( NamingException ne )
        {
        }

        try
        {
            assertFalse( evaluator.evaluate( new EqualityNode( "", new ClientStringValue( "" ) ), objectClasses ) );
            fail( "should never get here due to an NE" );
        }
        catch ( NamingException ne )
        {
        }

        try
        {
            assertFalse( evaluator.evaluate( new EqualityNode( "objectClass", new ClientStringValue( "" ) ), objectClasses ) );
            fail( "should never get here due to an IAE" );
        }
        catch ( IllegalArgumentException iae )
        {
        }

        try
        {
            objectClasses = new DefaultServerAttribute( "cn", OBJECT_CLASS );
            assertFalse( evaluator.evaluate( new EqualityNode( "cn", new ClientStringValue( "" ) ), objectClasses ) );
            fail( "should never get here due to an IAE" );
        }
        catch ( NamingException ne )
        {
            assertTrue( true );
        }
    }


    @Test public void testMatchByName() throws Exception
    {
        // positive test
        ServerAttribute objectClasses = new DefaultServerAttribute( "objectClass", OBJECT_CLASS, "person" );
        assertTrue( evaluator.evaluate( new EqualityNode( "objectClass", new ClientStringValue( "person" ) ), objectClasses ) );

        objectClasses = new DefaultServerAttribute( "objectClass", OBJECT_CLASS );
        objectClasses.add( "person" );
        objectClasses.add( "blah" );
        assertTrue( evaluator.evaluate( new EqualityNode( "objectClass", new ClientStringValue( "person" ) ), objectClasses ) );

        // negative tests
        objectClasses = new DefaultServerAttribute( "objectClass", OBJECT_CLASS, "person" );
        assertFalse( evaluator.evaluate( new EqualityNode( "objectClass", new ClientStringValue( "blah" ) ), objectClasses ) );

        objectClasses = new DefaultServerAttribute( "objectClass", OBJECT_CLASS, "blah" );
        assertFalse( evaluator.evaluate( new EqualityNode( "objectClass", new ClientStringValue( "person" ) ), objectClasses ) );
    }


    @Test public void testMatchByOID() throws Exception
    {
        ServerAttribute objectClasses = new DefaultServerAttribute( "objectClass", OBJECT_CLASS, "person" );

        // positive test
        assertTrue( evaluator.evaluate( new EqualityNode( "objectClass", new ClientStringValue( "2.5.6.6" ) ), objectClasses ) );

        objectClasses = new DefaultServerAttribute( "objectClass", OBJECT_CLASS );
        objectClasses.add( "person" );
        objectClasses.add( "blah" );
        assertTrue( evaluator.evaluate( new EqualityNode( "objectClass", new ClientStringValue( "2.5.6.6" ) ), objectClasses ) );

        // negative tests
        objectClasses = new DefaultServerAttribute( "objectClass", OBJECT_CLASS, "person" );
        assertFalse( evaluator.evaluate( new EqualityNode( "objectClass", new ClientStringValue( "2.5.6.5" ) ), objectClasses ) );

        objectClasses = new DefaultServerAttribute( "objectClass", OBJECT_CLASS, "blah" );
        assertFalse( evaluator.evaluate( new EqualityNode( "objectClass", new ClientStringValue( "2.5.6.5" ) ), objectClasses ) );
    }
}
