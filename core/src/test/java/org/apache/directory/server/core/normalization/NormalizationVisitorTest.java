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

import java.text.ParseException;

import org.apache.directory.server.core.DefaultDirectoryService;
import org.apache.directory.server.core.DirectoryService;
import org.apache.directory.server.schema.ConcreteNameComponentNormalizer;
import org.apache.directory.server.schema.registries.AttributeTypeRegistry;
import org.apache.directory.server.schema.registries.OidRegistry;
import org.apache.directory.shared.ldap.filter.EqualityNode;
import org.apache.directory.shared.ldap.filter.ExprNode;
import org.apache.directory.shared.ldap.filter.FilterParser;
import org.apache.directory.shared.ldap.filter.NotNode;
import org.apache.directory.shared.ldap.name.NameComponentNormalizer;
import org.junit.Before;
import org.junit.Test;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.assertEquals;


/**
 * A class to test the normalizing Visitor
 * 
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 *
 */
public class NormalizationVisitorTest
{
    /** a filter node value normalizer and undefined node remover */
    private static FilterNormalizingVisitor normVisitor;
    
    /** A reference to the directory service */
    private static DirectoryService service;
    
    @Before
    public void init() throws Exception
    {
        service = new DefaultDirectoryService();

        OidRegistry oidRegistry = service.getRegistries().getOidRegistry();
        AttributeTypeRegistry attributeRegistry = service.getRegistries().getAttributeTypeRegistry();
        NameComponentNormalizer ncn = new ConcreteNameComponentNormalizer( attributeRegistry, oidRegistry );
        normVisitor = new FilterNormalizingVisitor( ncn, service.getRegistries() );
    }

    @Test
    public void testSimpleFilter() throws ParseException
    {
        ExprNode filter = FilterParser.parse( "(ou=  test  1 )" );
        ExprNode result = ( ExprNode ) filter.accept( normVisitor );
        
        assertNotNull( result );
        assertTrue( result instanceof EqualityNode<?> );
        EqualityNode<?> equalityNode = (EqualityNode<?>)result;
        
        assertEquals( "test 1", equalityNode.getValue().getNormalizedValue() );
        assertEquals( "2.5.4.11", equalityNode.getAttribute() );
    }
    
    
    @Test
    public void testBranchNormalizedVisitor() throws Exception
    {
        ExprNode filter = FilterParser.parse( "(!(|(uniqueMember=cn=user1,ou=Test,dc=example,dc=com)(member=cn=user2,ou=Test,dc=example,dc=com)))" );
        ExprNode result = ( ExprNode ) filter.accept( normVisitor );

        assertNotNull( result );
        assertTrue( result instanceof NotNode );
    }

}
