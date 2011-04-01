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

import com.mycila.junit.concurrent.Concurrency;
import com.mycila.junit.concurrent.ConcurrentJunitRunner;
import org.apache.directory.shared.ldap.model.constants.SchemaConstants;
import org.apache.directory.shared.ldap.model.entry.DefaultAttribute;
import org.apache.directory.shared.ldap.model.entry.Attribute;
import org.apache.directory.shared.ldap.model.entry.StringValue;
import org.apache.directory.shared.ldap.model.exception.LdapException;
import org.apache.directory.shared.ldap.model.filter.EqualityNode;
import org.apache.directory.shared.ldap.model.filter.GreaterEqNode;
import org.apache.directory.shared.ldap.model.schema.AttributeType;
import org.apache.directory.shared.ldap.model.schema.SchemaManager;
import org.apache.directory.shared.ldap.schemaloader.JarLdifSchemaLoader;
import org.apache.directory.shared.ldap.schemamanager.impl.DefaultSchemaManager;
import org.apache.directory.shared.util.exception.Exceptions;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;


/**
 * Unit test cases for testing the evaluator for refinement leaf nodes.
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 */
@RunWith(ConcurrentJunitRunner.class)
@Concurrency()
public class RefinementLeafEvaluatorTest
{
    /** the SchemaManager instance */
    private static SchemaManager schemaManager;

    /** The ObjectClass AttributeType */
    private static AttributeType OBJECT_CLASS_AT;
    
    /** The CN AttributeType */
    private static AttributeType CN_AT;
    
    /** the refinement leaf evaluator to test */
    private static RefinementLeafEvaluator evaluator;


    /**
     * Initializes the global registries.
     * @throws javax.naming.NamingException if there is a failure loading the schema
     */
    @BeforeClass 
    public static void init() throws Exception
    {
        JarLdifSchemaLoader loader = new JarLdifSchemaLoader();

        schemaManager = new DefaultSchemaManager( loader );

        boolean loaded = schemaManager.loadAllEnabled();

        if ( !loaded )
        {
            fail( "Schema load failed : " + Exceptions.printErrors(schemaManager.getErrors()) );
        }
        
        OBJECT_CLASS_AT = schemaManager.getAttributeType( SchemaConstants.OBJECT_CLASS_AT );
        CN_AT = schemaManager.getAttributeType( SchemaConstants.CN_AT );

        evaluator = new RefinementLeafEvaluator( schemaManager );
    }
    

    /**
     * Sets evaluator and registries to null.
     */
    @AfterClass 
    public static void tearDown()
    {
        evaluator = null;
    }


    /**
     * Test cases for various bad combinations of arguments
     * @throws Exception if something goes wrongg
     */
    @Test 
    public void testForBadArguments() throws Exception
    {
        Attribute objectClasses = null;

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
            assertFalse( evaluator.evaluate( new GreaterEqNode( "", new StringValue( "" ) ), objectClasses ) );
            fail( "should never get here due to an NE" );
        }
        catch ( LdapException ne )
        {
        }

        try
        {
            assertFalse( evaluator.evaluate( new EqualityNode( "", new StringValue( "" ) ), objectClasses ) );
            fail( "should never get here due to an NE" );
        }
        catch ( IllegalArgumentException iae )
        {
        }

        try
        {
            assertFalse( evaluator.evaluate( new EqualityNode( OBJECT_CLASS_AT, new StringValue( "" ) ), objectClasses ) );
            fail( "should never get here due to an IAE" );
        }
        catch ( IllegalArgumentException iae )
        {
        }

        try
        {
            objectClasses = new DefaultAttribute( "cn", OBJECT_CLASS_AT.getName() );
            assertFalse( evaluator.evaluate( new EqualityNode( CN_AT, new StringValue( "" ) ), objectClasses ) );
            fail( "should never get here due to an IAE" );
        }
        catch ( IllegalArgumentException iae )
        {
            assertTrue( true );
        }
    }


    @Test 
    public void testMatchByName() throws Exception
    {
        // positive test
        Attribute objectClasses = new DefaultAttribute( OBJECT_CLASS_AT, "person" );
        assertTrue( evaluator.evaluate( new EqualityNode( OBJECT_CLASS_AT, new StringValue( "person" ) ), objectClasses ) );

        objectClasses = new DefaultAttribute( OBJECT_CLASS_AT );
        objectClasses.add( "person" );
        objectClasses.add( "blah" );
        assertTrue( evaluator.evaluate( new EqualityNode( OBJECT_CLASS_AT, new StringValue( "person" ) ), objectClasses ) );

        // negative tests
        objectClasses = new DefaultAttribute( OBJECT_CLASS_AT, "person" );
        assertFalse( evaluator.evaluate( new EqualityNode( OBJECT_CLASS_AT, new StringValue( "blah" ) ), objectClasses ) );

        objectClasses = new DefaultAttribute( OBJECT_CLASS_AT, "blah" );
        assertFalse( evaluator.evaluate( new EqualityNode( OBJECT_CLASS_AT, new StringValue( "person" ) ), objectClasses ) );
    }


    @Test 
    public void testMatchByOID() throws Exception
    {
        Attribute objectClasses = new DefaultAttribute( OBJECT_CLASS_AT, "person" );

        // positive test
        assertTrue( evaluator.evaluate( new EqualityNode( OBJECT_CLASS_AT, new StringValue( "2.5.6.6" ) ), objectClasses ) );

        objectClasses = new DefaultAttribute( OBJECT_CLASS_AT );
        objectClasses.add( "person" );
        objectClasses.add( "blah" );
        assertTrue( evaluator.evaluate( new EqualityNode( OBJECT_CLASS_AT, new StringValue( "2.5.6.6" ) ), objectClasses ) );

        // negative tests
        objectClasses = new DefaultAttribute( OBJECT_CLASS_AT, "person" );
        assertFalse( evaluator.evaluate( new EqualityNode( OBJECT_CLASS_AT, new StringValue( "2.5.6.5" ) ), objectClasses ) );

        objectClasses = new DefaultAttribute( OBJECT_CLASS_AT, "blah" );
        assertFalse( evaluator.evaluate( new EqualityNode( OBJECT_CLASS_AT, new StringValue( "2.5.6.5" ) ), objectClasses ) );
    }
}
