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

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import org.apache.directory.server.core.entry.DefaultServerAttribute;
import org.apache.directory.server.core.entry.ServerAttribute;
import org.apache.directory.shared.ldap.entry.client.ClientStringValue;
import org.apache.directory.shared.ldap.filter.EqualityNode;
import org.apache.directory.shared.ldap.filter.ExprNode;
import org.apache.directory.shared.ldap.filter.FilterParser;
import org.apache.directory.shared.ldap.filter.NotNode;
import org.apache.directory.shared.ldap.schema.AttributeType;
import org.apache.directory.shared.ldap.schema.SchemaManager;
import org.apache.directory.shared.ldap.schema.registries.OidRegistry;
import org.apache.directory.shared.ldap.schema.registries.Registries;
import org.apache.directory.shared.ldap.util.ExceptionUtils;
import org.apache.directory.shared.ldap.schema.DefaultSchemaManager;
import org.apache.directory.shared.ldap.schema.loader.ldif.JarLdifSchemaLoader;
import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;


/**
 * Unit test cases for testing the evaluator for refinements.
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 * @version $Rev$
 */
public class RefinementEvaluatorTest
{
    /** the registries */
    private static Registries registries;
    
    /** the refinement evaluator to test */
    private static RefinementEvaluator evaluator;

    /** The ObjectClass AttributeType */
    private static AttributeType OBJECT_CLASS;

    /** The CN AttributeType */
    private static AttributeType CN;
    
    
    /**
     * Initializes the global registries.
     * @throws javax.naming.NamingException if there is a failure loading the schema
     */
    @BeforeClass 
    public static void init() throws Exception
    {
        JarLdifSchemaLoader loader = new JarLdifSchemaLoader();

        SchemaManager sm = new DefaultSchemaManager( loader );

        boolean loaded = sm.loadAllEnabled();

        if ( !loaded )
        {
            fail( "Schema load failed : " + ExceptionUtils.printErrors( sm.getErrors() ) );
        }

        registries = sm.getRegistries();
    }


    /**
     * Initializes registries and creates the leaf evalutator
     * @throws Exception if there are schema initialization problems
     */
    @Before public void setUp() throws Exception
    {
        OidRegistry registry = registries.getGlobalOidRegistry();
        RefinementLeafEvaluator leafEvaluator = new RefinementLeafEvaluator( registry );
        evaluator = new RefinementEvaluator( leafEvaluator );
        
        OBJECT_CLASS = registries.getAttributeTypeRegistry().lookup( "objectClass" );
        CN = registries.getAttributeTypeRegistry().lookup( "cn" );
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
     * @throws Exception if something goes wrong
     */
    @Test public void testForBadArguments() throws Exception
    {
        try
        {
            assertFalse( evaluator.evaluate( null, new DefaultServerAttribute( "objectClass", OBJECT_CLASS ) ) );
            fail( "should never get here due to an IAE" );
        }
        catch ( IllegalArgumentException iae )
        {
        }

        try
        {
            assertFalse( evaluator.evaluate( new EqualityNode( "", new ClientStringValue( "" ) ), null ) );
            fail( "should never get here due to an IAE" );
        }
        catch ( IllegalArgumentException iae )
        {
        }

        try
        {
            assertFalse( evaluator.evaluate( new EqualityNode( "", new ClientStringValue( "" ) ), new DefaultServerAttribute( "cn", CN ) ) );
            fail( "should never get here due to an IAE" );
        }
        catch ( IllegalArgumentException iae )
        {
        }
    }


    @Test public void testMatchByName() throws Exception
    {
        ServerAttribute objectClasses = null;

        // positive test
        objectClasses = new DefaultServerAttribute( "objectClass", OBJECT_CLASS, "person" );
        assertTrue( evaluator.evaluate( new EqualityNode( "objectClass", new ClientStringValue( "person" ) ), objectClasses ) );

        objectClasses = new DefaultServerAttribute( "objectClass", OBJECT_CLASS, "person", "blah" );
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

        objectClasses = new DefaultServerAttribute( "objectClass", OBJECT_CLASS, "person", "blah" );
        assertTrue( evaluator.evaluate( new EqualityNode( "objectClass", new ClientStringValue( "2.5.6.6" ) ), objectClasses ) );

        // negative tests
        objectClasses = new DefaultServerAttribute( "objectClass", OBJECT_CLASS, "person" );
        assertFalse( evaluator.evaluate( new EqualityNode( "objectClass", new ClientStringValue( "2.5.6.5" ) ), objectClasses ) );

        objectClasses = new DefaultServerAttribute( "objectClass", OBJECT_CLASS, "blah" );
        assertFalse( evaluator.evaluate( new EqualityNode( "objectClass", new ClientStringValue( "2.5.6.5" ) ), objectClasses ) );
    }


    @Test public void testComplexOrRefinement() throws Exception
    {
        ExprNode refinement = null;
        ServerAttribute objectClasses = new DefaultServerAttribute( "objectClass", OBJECT_CLASS, "person" );
        String refStr = "(|(objectClass=person)(objectClass=organizationalUnit))";
        
        refinement = FilterParser.parse( refStr );

        assertTrue( evaluator.evaluate( refinement, objectClasses ) );
        
        objectClasses = new DefaultServerAttribute( "objectClass", OBJECT_CLASS, "organizationalUnit" );
        assertTrue( evaluator.evaluate( refinement, objectClasses ) );
        
        objectClasses = new DefaultServerAttribute( "objectClass", OBJECT_CLASS, "domain" );
        assertFalse( evaluator.evaluate( refinement, objectClasses ) );
    }


    @Test public void testComplexAndRefinement() throws Exception
    {
        ExprNode refinement = null;
        ServerAttribute objectClasses = new DefaultServerAttribute( "objectClass", OBJECT_CLASS, "person" );
        objectClasses.add( "organizationalUnit" );
        String refStr = "(&(objectClass=person)(objectClass=organizationalUnit))";
        
        refinement = FilterParser.parse( refStr );

        assertTrue( evaluator.evaluate( refinement, objectClasses ) );
        
        objectClasses = new DefaultServerAttribute( "objectClass", OBJECT_CLASS, "organizationalUnit" );
        assertFalse( evaluator.evaluate( refinement, objectClasses ) );
        
        objectClasses = new DefaultServerAttribute( "objectClass", OBJECT_CLASS, "person" );
        assertFalse( evaluator.evaluate( refinement, objectClasses ) );
        
        objectClasses = new DefaultServerAttribute( "objectClass", OBJECT_CLASS, "domain" );
        assertFalse( evaluator.evaluate( refinement, objectClasses ) );
    }


    @Test public void testComplexNotRefinement() throws Exception
    {
        ExprNode refinement = null;
        ServerAttribute objectClasses = new DefaultServerAttribute( "objectClass", OBJECT_CLASS, "person" );
        String refStr = "(!(objectClass=person))";

        refinement = FilterParser.parse( refStr );

        assertFalse( evaluator.evaluate( refinement, objectClasses ) );
        
        objectClasses = new DefaultServerAttribute( "objectClass", OBJECT_CLASS, "organizationalUnit" );
        assertTrue( evaluator.evaluate( refinement, objectClasses ) );
        
        objectClasses = new DefaultServerAttribute( "objectClass", OBJECT_CLASS, "domain" );
        assertTrue( evaluator.evaluate( refinement, objectClasses ) );

        try
        {
            assertFalse( evaluator.evaluate( new NotNode(), new DefaultServerAttribute( "objectClass", OBJECT_CLASS ) ) );
            fail( "should never get here due to an IAE" );
        }
        catch ( IllegalArgumentException iae )
        {
        }
    }
}
