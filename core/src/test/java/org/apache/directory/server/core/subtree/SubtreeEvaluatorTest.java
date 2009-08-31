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


import java.io.File;
import java.util.HashSet;
import java.util.Set;

import org.apache.directory.server.core.entry.DefaultServerAttributeTest;
import org.apache.directory.server.core.entry.DefaultServerEntry;
import org.apache.directory.server.core.entry.ServerEntry;
import org.apache.directory.server.core.normalization.FilterNormalizingVisitor;
import org.apache.directory.shared.ldap.schema.ldif.extractor.SchemaLdifExtractor;
import org.apache.directory.shared.ldap.schema.normalizers.ConcreteNameComponentNormalizer;
import org.apache.directory.shared.ldap.filter.ExprNode;
import org.apache.directory.shared.ldap.filter.FilterParser;
import org.apache.directory.shared.ldap.name.LdapDN;
import org.apache.directory.shared.ldap.name.NameComponentNormalizer;
import org.apache.directory.shared.ldap.schema.registries.AttributeTypeRegistry;
import org.apache.directory.shared.ldap.schema.registries.Registries;
import org.apache.directory.shared.ldap.subtree.SubtreeSpecification;
import org.apache.directory.shared.ldap.subtree.SubtreeSpecificationModifier;
import org.apache.directory.shared.schema.loader.ldif.LdifSchemaLoader;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.assertFalse;



/**
 * Unit test cases for the SubtreeEvaluator.
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 * @version $Rev$
 */
public class SubtreeEvaluatorTest
{
    private Registries registries;
    private SubtreeEvaluator evaluator;
    FilterNormalizingVisitor visitor;
    AttributeTypeRegistry attributeRegistry;


    private void init() throws Exception
    {
    	String workingDirectory = System.getProperty( "workingDirectory" );

        if ( workingDirectory == null )
        {
            String path = DefaultServerAttributeTest.class.getResource( "" ).getPath();
            int targetPos = path.indexOf( "target" );
            workingDirectory = path.substring( 0, targetPos + 6 );
        }

        File schemaRepository = new File( workingDirectory, "schema" );
        SchemaLdifExtractor extractor = new SchemaLdifExtractor( new File( workingDirectory ) );
        extractor.extractOrCopy();
        LdifSchemaLoader loader = new LdifSchemaLoader( schemaRepository );
        registries = new Registries();
        loader.loadAllEnabled( registries );
    	
        attributeRegistry = registries.getAttributeTypeRegistry();
        
        NameComponentNormalizer ncn = new ConcreteNameComponentNormalizer( attributeRegistry );
        visitor = new FilterNormalizingVisitor( ncn, registries );
    }

    @BeforeClass
    protected void setUp() throws Exception
    {
        init();
        evaluator = new SubtreeEvaluator( registries.getOidRegistry(), registries.getAttributeTypeRegistry() );
    }


    @AfterClass
    protected void tearDown() throws Exception
    {
        evaluator = null;
        registries = null;
    }


    @Test
    public void testDefaults() throws Exception
    {
        SubtreeSpecificationModifier modifier = new SubtreeSpecificationModifier();
        SubtreeSpecification ss = modifier.getSubtreeSpecification();
        LdapDN apDn = new LdapDN( "ou=system" );
        LdapDN entryDn = new LdapDN( "ou=users,ou=system" );
        ServerEntry entry = new DefaultServerEntry( registries, entryDn, "objectClass" );

        assertTrue( evaluator.evaluate( ss, apDn, entryDn, entry ) );

        entryDn = new LdapDN( "ou=system" );
        assertTrue( evaluator.evaluate( ss, apDn, entryDn, entry ) );

        entryDn = new LdapDN( "ou=abc" );
        assertFalse( evaluator.evaluate( ss, apDn, entryDn, entry ) );
    }


    @Test
    public void testWithBase() throws Exception
    {
        SubtreeSpecificationModifier modifier = new SubtreeSpecificationModifier();
        modifier.setBase( new LdapDN( "ou=users" ) );
        SubtreeSpecification ss = modifier.getSubtreeSpecification();
        LdapDN apDn = new LdapDN( "ou=system" );
        LdapDN entryDn = new LdapDN( "ou=users,ou=system" );
        ServerEntry entry = new DefaultServerEntry( registries, entryDn, "objectClass" );

        assertTrue( evaluator.evaluate( ss, apDn, entryDn, entry ) );

        entryDn = new LdapDN( "uid=akarasulu,ou=users,ou=system" );
        assertTrue( evaluator.evaluate( ss, apDn, entryDn, entry ) );

        entryDn = new LdapDN( "ou=system" );
        assertFalse( evaluator.evaluate( ss, apDn, entryDn, entry ) );
    }


    @Test
    public void testWithMinMax() throws Exception
    {
        SubtreeSpecificationModifier modifier = new SubtreeSpecificationModifier();
        modifier.setMinBaseDistance( 1 );
        modifier.setMaxBaseDistance( 3 );
        modifier.setBase( new LdapDN( "ou=users" ) );
        SubtreeSpecification ss = modifier.getSubtreeSpecification();
        LdapDN apDn = new LdapDN( "ou=system" );
        LdapDN entryDn = new LdapDN( "ou=users,ou=system" );
        ServerEntry entry = new DefaultServerEntry( registries, entryDn, "objectClass" );

        assertFalse( evaluator.evaluate( ss, apDn, entryDn, entry ) );

        entryDn = new LdapDN( "uid=akarasulu,ou=users,ou=system" );
        assertTrue( evaluator.evaluate( ss, apDn, entryDn, entry ) );

        entryDn = new LdapDN( "ou=system" );
        assertFalse( evaluator.evaluate( ss, apDn, entryDn, entry ) );

        entryDn = new LdapDN( "ou=twolevels,uid=akarasulu,ou=users,ou=system" );
        assertTrue( evaluator.evaluate( ss, apDn, entryDn, entry ) );

        entryDn = new LdapDN( "ou=threelevels,ou=twolevels,uid=akarasulu,ou=users,ou=system" );
        assertTrue( evaluator.evaluate( ss, apDn, entryDn, entry ) );

        entryDn = new LdapDN( "ou=fourlevels,ou=threelevels,ou=twolevels,uid=akarasulu,ou=users,ou=system" );
        assertFalse( evaluator.evaluate( ss, apDn, entryDn, entry ) );
    }


    @Test
    public void testWithMinMaxAndChopAfter() throws Exception
    {
        SubtreeSpecificationModifier modifier = new SubtreeSpecificationModifier();
        Set<LdapDN> chopAfter = new HashSet<LdapDN>();
        chopAfter.add( new LdapDN( "uid=Tori Amos" ) );
        chopAfter.add( new LdapDN( "ou=twolevels,uid=akarasulu" ) );
        modifier.setChopAfterExclusions( chopAfter );
        modifier.setMinBaseDistance( 1 );
        modifier.setMaxBaseDistance( 3 );
        modifier.setBase( new LdapDN( "ou=users" ) );
        SubtreeSpecification ss = modifier.getSubtreeSpecification();
        LdapDN apDn = new LdapDN( "ou=system" );
        LdapDN entryDn = new LdapDN( "ou=users,ou=system" );
        ServerEntry entry = new DefaultServerEntry( registries, entryDn, "objectClass" );

        assertFalse( evaluator.evaluate( ss, apDn, entryDn, entry ) );

        entryDn = new LdapDN( "uid=akarasulu,ou=users,ou=system" );
        assertTrue( evaluator.evaluate( ss, apDn, entryDn, entry ) );

        entryDn = new LdapDN( "ou=system" );
        assertFalse( evaluator.evaluate( ss, apDn, entryDn, entry ) );

        entryDn = new LdapDN( "ou=twolevels,uid=akarasulu,ou=users,ou=system" );
        assertTrue( evaluator.evaluate( ss, apDn, entryDn, entry ) );

        entryDn = new LdapDN( "ou=threelevels,ou=twolevels,uid=akarasulu,ou=users,ou=system" );
        assertFalse( evaluator.evaluate( ss, apDn, entryDn, entry ) );

        entryDn = new LdapDN( "ou=fourlevels,ou=threelevels,ou=twolevels,uid=akarasulu,ou=users,ou=system" );
        assertFalse( evaluator.evaluate( ss, apDn, entryDn, entry ) );
    }


    @Test
    public void testWithMinMaxAndChopBefore() throws Exception
    {
        SubtreeSpecificationModifier modifier = new SubtreeSpecificationModifier();
        Set<LdapDN> chopBefore = new HashSet<LdapDN>();
        chopBefore.add( new LdapDN( "uid=Tori Amos" ) );
        chopBefore.add( new LdapDN( "ou=threelevels,ou=twolevels,uid=akarasulu" ) );
        modifier.setChopBeforeExclusions( chopBefore );
        modifier.setMinBaseDistance( 1 );
        modifier.setMaxBaseDistance( 3 );
        modifier.setBase( new LdapDN( "ou=users" ) );
        SubtreeSpecification ss = modifier.getSubtreeSpecification();
        LdapDN apDn = new LdapDN( "ou=system" );
        LdapDN entryDn = new LdapDN( "ou=users,ou=system" );
        ServerEntry entry = new DefaultServerEntry( registries, entryDn, "objectClass" );

        assertFalse( evaluator.evaluate( ss, apDn, entryDn, entry ) );

        entryDn = new LdapDN( "uid=akarasulu,ou=users,ou=system" );
        assertTrue( evaluator.evaluate( ss, apDn, entryDn, entry ) );

        entryDn = new LdapDN( "ou=system" );
        assertFalse( evaluator.evaluate( ss, apDn, entryDn, entry ) );

        entryDn = new LdapDN( "ou=twolevels,uid=akarasulu,ou=users,ou=system" );
        assertTrue( evaluator.evaluate( ss, apDn, entryDn, entry ) );

        entryDn = new LdapDN( "ou=threelevels,ou=twolevels,uid=akarasulu,ou=users,ou=system" );
        assertFalse( evaluator.evaluate( ss, apDn, entryDn, entry ) );

        entryDn = new LdapDN( "ou=fourlevels,ou=threelevels,ou=twolevels,uid=akarasulu,ou=users,ou=system" );
        assertFalse( evaluator.evaluate( ss, apDn, entryDn, entry ) );
    }


    @Test
    public void testWithMinMaxAndSimpleRefinement() throws Exception
    {
        ExprNode refinement = FilterParser.parse( "(objectClass=person)" );
        refinement.accept( visitor );

        SubtreeSpecificationModifier modifier = new SubtreeSpecificationModifier();
        modifier.setRefinement( refinement );
        modifier.setMinBaseDistance( 1 );
        modifier.setMaxBaseDistance( 3 );
        modifier.setBase( new LdapDN( "ou=users" ) );
        SubtreeSpecification ss = modifier.getSubtreeSpecification();
        LdapDN apDn = new LdapDN( "ou=system" );
        LdapDN entryDn = new LdapDN( "ou=users,ou=system" );
        ServerEntry entry = new DefaultServerEntry( registries, entryDn );
        entry.put( "objectClass", "person" );

        assertFalse( evaluator.evaluate( ss, apDn, entryDn, entry ) );

        entryDn = new LdapDN( "uid=akarasulu,ou=users,ou=system" );
        assertTrue( evaluator.evaluate( ss, apDn, entryDn, entry ) );

        entryDn = new LdapDN( "ou=system" );
        assertFalse( evaluator.evaluate( ss, apDn, entryDn, entry ) );

        entryDn = new LdapDN( "ou=twolevels,uid=akarasulu,ou=users,ou=system" );
        assertTrue( evaluator.evaluate( ss, apDn, entryDn, entry ) );

        entryDn = new LdapDN( "ou=threelevels,ou=twolevels,uid=akarasulu,ou=users,ou=system" );
        assertTrue( evaluator.evaluate( ss, apDn, entryDn, entry ) );

        entryDn = new LdapDN( "ou=fourlevels,ou=threelevels,ou=twolevels,uid=akarasulu,ou=users,ou=system" );
        assertFalse( evaluator.evaluate( ss, apDn, entryDn, entry ) );

        // now change the refinement so the entry is rejected
        entry = new DefaultServerEntry( registries, entryDn );
        entry.put( "objectClass", "organizationalUnit" );
        

        assertFalse( evaluator.evaluate( ss, apDn, entryDn, entry ) );

        entryDn = new LdapDN( "uid=akarasulu,ou=users,ou=system" );
        assertFalse( evaluator.evaluate( ss, apDn, entryDn, entry ) );

        entryDn = new LdapDN( "ou=system" );
        assertFalse( evaluator.evaluate( ss, apDn, entryDn, entry ) );

        entryDn = new LdapDN( "ou=twolevels,uid=akarasulu,ou=users,ou=system" );
        assertFalse( evaluator.evaluate( ss, apDn, entryDn, entry ) );

        entryDn = new LdapDN( "ou=threelevels,ou=twolevels,uid=akarasulu,ou=users,ou=system" );
        assertFalse( evaluator.evaluate( ss, apDn, entryDn, entry ) );

        entryDn = new LdapDN( "ou=fourlevels,ou=threelevels,ou=twolevels,uid=akarasulu,ou=users,ou=system" );
        assertFalse( evaluator.evaluate( ss, apDn, entryDn, entry ) );

    }
    
    
    @Test
    public void testWithFilter() throws Exception
    {
        ExprNode filter = FilterParser.parse( "(&(cn=Ersin)(objectClass=person))" );
        filter.accept( visitor );

        SubtreeSpecificationModifier modifier = new SubtreeSpecificationModifier();
        modifier.setRefinement( filter );
        modifier.setMinBaseDistance( 1 );
        modifier.setMaxBaseDistance( 3 );
        modifier.setBase( new LdapDN( "ou=users" ) );
        SubtreeSpecification ss = modifier.getSubtreeSpecification();
        LdapDN apDn = new LdapDN( "ou=system" );
        LdapDN entryDn = new LdapDN( "ou=users,ou=system" );

        ServerEntry entry = new DefaultServerEntry( registries, entryDn );;
        entry.put( "objectClass", "person" );
        entry.put( "cn", "Ersin" );

        assertFalse( evaluator.evaluate( ss, apDn, entryDn, entry ) );

        entryDn = new LdapDN( "cn=Ersin,ou=users,ou=system" );
        assertTrue( evaluator.evaluate( ss, apDn, entryDn, entry ) );

        // now change the filter so the entry is rejected
        entry = new DefaultServerEntry( registries, entryDn );;
        entry.put( "objectClass", "person" );
        entry.put( "cn", "Alex" );

        assertFalse( evaluator.evaluate( ss, apDn, entryDn, entry ) );

        entryDn = new LdapDN( "cn=Alex,ou=users,ou=system" );
        assertFalse( evaluator.evaluate( ss, apDn, entryDn, entry ) );
    }
}
