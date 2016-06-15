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


import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.text.ParseException;

import org.apache.directory.api.ldap.model.filter.EqualityNode;
import org.apache.directory.api.ldap.model.filter.ExprNode;
import org.apache.directory.api.ldap.model.filter.FilterParser;
import org.apache.directory.api.ldap.model.filter.NotNode;
import org.apache.directory.api.ldap.model.filter.PresenceNode;
import org.apache.directory.api.ldap.model.schema.SchemaManager;
import org.apache.directory.api.ldap.model.schema.normalizers.ConcreteNameComponentNormalizer;
import org.apache.directory.api.ldap.model.schema.normalizers.NameComponentNormalizer;
import org.apache.directory.api.ldap.schema.loader.JarLdifSchemaLoader;
import org.apache.directory.api.ldap.schema.manager.impl.DefaultSchemaManager;
import org.apache.directory.api.util.exception.Exceptions;
import org.apache.directory.server.core.api.normalization.FilterNormalizingVisitor;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;

import com.mycila.junit.concurrent.Concurrency;
import com.mycila.junit.concurrent.ConcurrentJunitRunner;


/**
 * A class to test the normalizing Visitor
 * 
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 *
 */
@RunWith(ConcurrentJunitRunner.class)
@Concurrency()
public class NormalizationVisitorTest
{
    /** a filter node value normalizer and undefined node remover */
    private static FilterNormalizingVisitor normVisitor;

    /** A reference to the schemaManager */
    private static SchemaManager schemaManager;


    @BeforeClass
    public static void init() throws Exception
    {
        JarLdifSchemaLoader loader = new JarLdifSchemaLoader();

        schemaManager = new DefaultSchemaManager( loader );

        boolean loaded = schemaManager.loadAllEnabled();

        if ( !loaded )
        {
            fail( "Schema load failed : " + Exceptions.printErrors( schemaManager.getErrors() ) );
        }

        NameComponentNormalizer ncn = new ConcreteNameComponentNormalizer( schemaManager );
        normVisitor = new FilterNormalizingVisitor( ncn, schemaManager );
    }


    @Test
    public void testSimpleFilter() throws ParseException
    {
        ExprNode filter = FilterParser.parse( schemaManager, "(ou=  test  1 )" );
        ExprNode result = ( ExprNode ) filter.accept( normVisitor );

        assertNotNull( result );
        assertTrue( result instanceof EqualityNode<?> );
        EqualityNode<?> equalityNode = (org.apache.directory.api.ldap.model.filter.EqualityNode<?> ) result;

        assertEquals( " test  1 ", equalityNode.getValue().getValue() );
        assertEquals( "2.5.4.11", equalityNode.getAttributeType().getOid() );
    }


    @Test
    public void testPresenceFilter() throws ParseException
    {
        ExprNode filter = FilterParser.parse( schemaManager, "(ou=*)" );
        ExprNode result = ( ExprNode ) filter.accept( normVisitor );

        assertNotNull( result );
        assertTrue( result instanceof PresenceNode );
        PresenceNode presenceNode = ( PresenceNode ) result;

        assertEquals( "2.5.4.11", presenceNode.getAttributeType().getOid() );
    }


    @Test
    public void testBranchNormalizedVisitor() throws Exception
    {
        ExprNode filter = FilterParser.parse( schemaManager,
            "(!(|(uniqueMember=cn=user1,ou=Test,dc=example,dc=com)(member=cn=user2,ou=Test,dc=example,dc=com)))" );
        ExprNode result = ( ExprNode ) filter.accept( normVisitor );

        assertNotNull( result );
        assertTrue( result instanceof NotNode );
    }

}
