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


import junit.framework.TestCase;

import javax.naming.NamingException;
import javax.naming.directory.BasicAttribute;

import org.apache.directory.server.core.schema.GlobalRegistries;
import org.apache.directory.server.core.schema.OidRegistry;
import org.apache.directory.server.core.schema.bootstrap.*;
import org.apache.directory.server.core.subtree.RefinementEvaluator;
import org.apache.directory.server.core.subtree.RefinementLeafEvaluator;
import org.apache.directory.shared.ldap.filter.BranchNode;
import org.apache.directory.shared.ldap.filter.ExprNode;
import org.apache.directory.shared.ldap.filter.FilterParser;
import org.apache.directory.shared.ldap.filter.FilterParserImpl;
import org.apache.directory.shared.ldap.filter.LeafNode;
import org.apache.directory.shared.ldap.filter.SimpleNode;

import java.util.Set;
import java.util.HashSet;


/**
 * Unit test cases for testing the evaluator for refinements.
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 * @version $Rev$
 */
public class RefinementEvaluatorTest extends TestCase
{
    /** the global registries */
    private GlobalRegistries registries;
    /** the refinement evaluator to test */
    private RefinementEvaluator evaluator;


    /**
     * Initializes the global registries.
     * @throws javax.naming.NamingException if there is a failure loading the schema
     */
    private void init() throws NamingException
    {
        BootstrapRegistries bsRegistries = new BootstrapRegistries();
        BootstrapSchemaLoader loader = new BootstrapSchemaLoader();
        Set schemas = new HashSet();
        schemas.add( new SystemSchema() );
        schemas.add( new ApacheSchema() );
        schemas.add( new CoreSchema() );
        schemas.add( new CosineSchema() );
        schemas.add( new InetorgpersonSchema() );
        schemas.add( new JavaSchema() );
        loader.load( schemas, bsRegistries );
        registries = new GlobalRegistries( bsRegistries );
    }


    /**
     * Initializes registries and creates the leaf evalutator
     * @throws Exception if there are schema initialization problems
     */
    protected void setUp() throws Exception
    {
        init();
        OidRegistry registry = registries.getOidRegistry();
        RefinementLeafEvaluator leafEvaluator = new RefinementLeafEvaluator( registry );
        evaluator = new RefinementEvaluator( leafEvaluator );
    }


    /**
     * Sets evaluator and registries to null.
     */
    protected void tearDown()
    {
        evaluator = null;
        registries = null;
    }


    /**
     * Test cases for various bad combinations of arguments
     * @throws Exception if something goes wrongg
     */
    public void testForBadArguments() throws Exception
    {
        try
        {
            assertFalse( evaluator.evaluate( null, new BasicAttribute( "objectClass" ) ) );
            fail( "should never get here due to an IAE" );
        }
        catch ( IllegalArgumentException iae )
        {
        }

        try
        {
            assertFalse( evaluator.evaluate( new SimpleNode( "", "", LeafNode.EQUALITY ), null ) );
            fail( "should never get here due to an IAE" );
        }
        catch ( IllegalArgumentException iae )
        {
        }

        try
        {
            assertFalse( evaluator.evaluate( new SimpleNode( "", "", LeafNode.EQUALITY ), new BasicAttribute( "blah" ) ) );
            fail( "should never get here due to an IAE" );
        }
        catch ( IllegalArgumentException iae )
        {
        }
    }


    public void testMatchByName() throws Exception
    {
        BasicAttribute objectClasses = null;

        // positive test
        objectClasses = new BasicAttribute( "objectClass", "person" );
        assertTrue( evaluator.evaluate( new SimpleNode( "objectClass", "person", LeafNode.EQUALITY ), objectClasses ) );

        objectClasses = new BasicAttribute( "objectClass" );
        objectClasses.add( "person" );
        objectClasses.add( "blah" );
        assertTrue( evaluator.evaluate( new SimpleNode( "objectClass", "person", LeafNode.EQUALITY ), objectClasses ) );

        // negative tests
        objectClasses = new BasicAttribute( "objectClass", "person" );
        assertFalse( evaluator.evaluate( new SimpleNode( "objectClass", "blah", LeafNode.EQUALITY ), objectClasses ) );

        objectClasses = new BasicAttribute( "objectClass", "blah" );
        assertFalse( evaluator.evaluate( new SimpleNode( "objectClass", "person", LeafNode.EQUALITY ), objectClasses ) );
    }


    public void testMatchByOID() throws Exception
    {
        BasicAttribute objectClasses = null;

        // positive test
        objectClasses = new BasicAttribute( "objectClass", "person" );
        assertTrue( evaluator.evaluate( new SimpleNode( "objectClass", "2.5.6.6", LeafNode.EQUALITY ), objectClasses ) );

        objectClasses = new BasicAttribute( "objectClass" );
        objectClasses.add( "person" );
        objectClasses.add( "blah" );
        assertTrue( evaluator.evaluate( new SimpleNode( "objectClass", "2.5.6.6", LeafNode.EQUALITY ), objectClasses ) );

        // negative tests
        objectClasses = new BasicAttribute( "objectClass", "person" );
        assertFalse( evaluator.evaluate( new SimpleNode( "objectClass", "2.5.6.5", LeafNode.EQUALITY ), objectClasses ) );

        objectClasses = new BasicAttribute( "objectClass", "blah" );
        assertFalse( evaluator.evaluate( new SimpleNode( "objectClass", "2.5.6.5", LeafNode.EQUALITY ), objectClasses ) );
    }


    public void testComplexOrRefinement() throws Exception
    {
        ExprNode refinement = null;
        BasicAttribute objectClasses = new BasicAttribute( "objectClass", "person" );
        FilterParser parser = new FilterParserImpl();
        String refStr = "(| (objectClass=person) (objectClass=organizationalUnit) )";
        refinement = parser.parse( refStr );

        assertTrue( evaluator.evaluate( refinement, objectClasses ) );
        objectClasses = new BasicAttribute( "objectClass", "organizationalUnit" );
        assertTrue( evaluator.evaluate( refinement, objectClasses ) );
        objectClasses = new BasicAttribute( "objectClass", "domain" );
        assertFalse( evaluator.evaluate( refinement, objectClasses ) );
    }


    public void testComplexAndRefinement() throws Exception
    {
        ExprNode refinement = null;
        BasicAttribute objectClasses = new BasicAttribute( "objectClass", "person" );
        objectClasses.add( "organizationalUnit" );
        FilterParser parser = new FilterParserImpl();
        String refStr = "(& (objectClass=person) (objectClass=organizationalUnit) )";
        refinement = parser.parse( refStr );

        assertTrue( evaluator.evaluate( refinement, objectClasses ) );
        objectClasses = new BasicAttribute( "objectClass", "organizationalUnit" );
        assertFalse( evaluator.evaluate( refinement, objectClasses ) );
        objectClasses = new BasicAttribute( "objectClass", "person" );
        assertFalse( evaluator.evaluate( refinement, objectClasses ) );
        objectClasses = new BasicAttribute( "objectClass", "domain" );
        assertFalse( evaluator.evaluate( refinement, objectClasses ) );
    }


    public void testComplexNotRefinement() throws Exception
    {
        ExprNode refinement = null;
        BasicAttribute objectClasses = new BasicAttribute( "objectClass", "person" );
        FilterParser parser = new FilterParserImpl();
        String refStr = "(! (objectClass=person) )";
        refinement = parser.parse( refStr );

        assertFalse( evaluator.evaluate( refinement, objectClasses ) );
        objectClasses = new BasicAttribute( "objectClass", "organizationalUnit" );
        assertTrue( evaluator.evaluate( refinement, objectClasses ) );
        objectClasses = new BasicAttribute( "objectClass", "domain" );
        assertTrue( evaluator.evaluate( refinement, objectClasses ) );

        try
        {
            assertFalse( evaluator.evaluate( new BranchNode( 1000 ), new BasicAttribute( "objectClass" ) ) );
            fail( "should never get here due to an IAE" );
        }
        catch ( IllegalArgumentException iae )
        {
        }

        try
        {
            assertFalse( evaluator.evaluate( new BranchNode( BranchNode.NOT ), new BasicAttribute( "objectClass" ) ) );
            fail( "should never get here due to an IAE" );
        }
        catch ( IllegalArgumentException iae )
        {
        }
    }
}
