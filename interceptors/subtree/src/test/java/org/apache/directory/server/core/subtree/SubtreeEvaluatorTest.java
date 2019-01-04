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

import java.io.File;
import java.util.HashSet;
import java.util.Set;

import org.apache.directory.api.ldap.model.entry.DefaultEntry;
import org.apache.directory.api.ldap.model.entry.Entry;
import org.apache.directory.api.ldap.model.filter.ExprNode;
import org.apache.directory.api.ldap.model.filter.FilterParser;
import org.apache.directory.api.ldap.model.name.Dn;
import org.apache.directory.api.ldap.model.schema.SchemaManager;
import org.apache.directory.api.ldap.model.schema.normalizers.ConcreteNameComponentNormalizer;
import org.apache.directory.api.ldap.model.subtree.SubtreeSpecification;
import org.apache.directory.api.ldap.model.subtree.SubtreeSpecificationModifier;
import org.apache.directory.api.ldap.schema.extractor.SchemaLdifExtractor;
import org.apache.directory.api.ldap.schema.extractor.impl.DefaultSchemaLdifExtractor;
import org.apache.directory.api.ldap.schema.loader.LdifSchemaLoader;
import org.apache.directory.api.ldap.schema.manager.impl.DefaultSchemaManager;
import org.apache.directory.api.util.exception.Exceptions;
import org.apache.directory.server.core.api.DnFactory;
import org.apache.directory.server.core.api.normalization.FilterNormalizingVisitor;
import org.apache.directory.server.core.api.subtree.SubtreeEvaluator;
import org.apache.directory.server.core.shared.DefaultDnFactory;
import org.ehcache.Cache;
import org.ehcache.CacheManager;
import org.ehcache.config.builders.CacheConfigurationBuilder;
import org.ehcache.config.builders.CacheManagerBuilder;
import org.ehcache.config.builders.ResourcePoolsBuilder;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;

import com.mycila.junit.concurrent.Concurrency;
import com.mycila.junit.concurrent.ConcurrentJunitRunner;


/**
 * Unit test cases for the SubtreeEvaluator.
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 */
@RunWith(ConcurrentJunitRunner.class)
@Concurrency()
public class SubtreeEvaluatorTest
{
    private static DnFactory dnFactory;
    private static SchemaManager schemaManager;
    private static SubtreeEvaluator evaluator;
    private static FilterNormalizingVisitor visitor;
    private static ConcreteNameComponentNormalizer ncn;
    private static Cache<String, Dn> dnCache;


    @BeforeClass
    public static void init() throws Exception
    {
        String workingDirectory = System.getProperty( "workingDirectory" );

        if ( workingDirectory == null )
        {
            String path = SubtreeEvaluatorTest.class.getResource( "" ).getPath();
            int targetPos = path.indexOf( "target" );
            workingDirectory = path.substring( 0, targetPos + 6 );
        }

        File schemaRepository = new File( workingDirectory, "schema" );
        SchemaLdifExtractor extractor = new DefaultSchemaLdifExtractor( new File( workingDirectory ) );
        extractor.extractOrCopy( true );
        LdifSchemaLoader loader = new LdifSchemaLoader( schemaRepository );
        schemaManager = new DefaultSchemaManager( loader );

        boolean loaded = schemaManager.loadAllEnabled();

        if ( !loaded )
        {
            fail( "Schema load failed : " + Exceptions.printErrors( schemaManager.getErrors() ) );
        }

        CacheManager cm = CacheManagerBuilder.newCacheManagerBuilder().withCache( "dnCache", CacheConfigurationBuilder.newCacheConfigurationBuilder( String.class, Dn.class, ResourcePoolsBuilder.heap(1000)).build()).build();
        cm.init();
        dnCache = cm.getCache( "dnCache", String.class, Dn.class );        dnFactory = new DefaultDnFactory( schemaManager, dnCache );

        ncn = new ConcreteNameComponentNormalizer( schemaManager );

        visitor = new FilterNormalizingVisitor( ncn, schemaManager );
        evaluator = new SubtreeEvaluator( schemaManager );
    }


    @AfterClass
    public static void destroyTest()
    {
        visitor = null;
        evaluator = null;
        dnCache.clear();
    }


    @AfterClass
    public static void tearDown() throws Exception
    {
        schemaManager = null;
    }


    @Test
    public void testDefaults() throws Exception
    {
        SubtreeSpecificationModifier modifier = new SubtreeSpecificationModifier();
        SubtreeSpecification ss = modifier.getSubtreeSpecification();
        Dn apDn = dnFactory.create( "ou=system" );
        Dn entryDn = dnFactory.create( "ou=users,ou=system" );
        Entry entry = new DefaultEntry( schemaManager, entryDn );

        assertTrue( evaluator.evaluate( ss, apDn, entryDn, entry ) );

        entryDn = dnFactory.create( "ou=system" );
        assertTrue( evaluator.evaluate( ss, apDn, entryDn, entry ) );

        entryDn = dnFactory.create( "ou=abc" );
        assertFalse( evaluator.evaluate( ss, apDn, entryDn, entry ) );
    }


    @Test
    public void testWithBase() throws Exception
    {
        SubtreeSpecificationModifier modifier = new SubtreeSpecificationModifier();
        modifier.setBase( dnFactory.create( "ou=users" ) );
        SubtreeSpecification ss = modifier.getSubtreeSpecification();
        Dn apDn = dnFactory.create( "ou=system" );
        Dn entryDn = dnFactory.create( "ou=users,ou=system" );
        Entry entry = new DefaultEntry( schemaManager, entryDn );

        assertTrue( evaluator.evaluate( ss, apDn, entryDn, entry ) );

        entryDn = dnFactory.create( "uid=akarasulu,ou=users,ou=system" );
        assertTrue( evaluator.evaluate( ss, apDn, entryDn, entry ) );

        entryDn = dnFactory.create( "ou=system" );
        assertFalse( evaluator.evaluate( ss, apDn, entryDn, entry ) );
    }


    @Test
    public void testWithMinMax() throws Exception
    {
        SubtreeSpecificationModifier modifier = new SubtreeSpecificationModifier();
        modifier.setMinBaseDistance( 1 );
        modifier.setMaxBaseDistance( 3 );
        modifier.setBase( dnFactory.create( "ou=users" ) );
        SubtreeSpecification ss = modifier.getSubtreeSpecification();
        Dn apDn = dnFactory.create( "ou=system" );
        Dn entryDn = dnFactory.create( "ou=users,ou=system" );
        Entry entry = new DefaultEntry( schemaManager, entryDn );

        assertFalse( evaluator.evaluate( ss, apDn, entryDn, entry ) );

        entryDn = dnFactory.create( "uid=akarasulu,ou=users,ou=system" );
        assertTrue( evaluator.evaluate( ss, apDn, entryDn, entry ) );

        entryDn = dnFactory.create( "ou=system" );
        assertFalse( evaluator.evaluate( ss, apDn, entryDn, entry ) );

        entryDn = dnFactory.create( "ou=twolevels,uid=akarasulu,ou=users,ou=system" );
        assertTrue( evaluator.evaluate( ss, apDn, entryDn, entry ) );

        entryDn = dnFactory.create( "ou=threelevels,ou=twolevels,uid=akarasulu,ou=users,ou=system" );
        assertTrue( evaluator.evaluate( ss, apDn, entryDn, entry ) );

        entryDn = dnFactory.create( "ou=fourlevels,ou=threelevels,ou=twolevels,uid=akarasulu,ou=users,ou=system" );
        assertFalse( evaluator.evaluate( ss, apDn, entryDn, entry ) );
    }


    @Test
    public void testWithMinMaxAndChopAfter() throws Exception
    {
        SubtreeSpecificationModifier modifier = new SubtreeSpecificationModifier();
        Set<Dn> chopAfter = new HashSet<Dn>();
        chopAfter.add( dnFactory.create( "uid=Tori Amos" ) );
        chopAfter.add( dnFactory.create( "ou=twolevels,uid=akarasulu" ) );
        modifier.setChopAfterExclusions( chopAfter );
        modifier.setMinBaseDistance( 1 );
        modifier.setMaxBaseDistance( 3 );
        modifier.setBase( dnFactory.create( "ou=users" ) );
        SubtreeSpecification ss = modifier.getSubtreeSpecification();
        Dn apDn = dnFactory.create( "ou=system" );
        Dn entryDn = dnFactory.create( "ou=users,ou=system" );
        Entry entry = new DefaultEntry( schemaManager, entryDn );

        assertFalse( evaluator.evaluate( ss, apDn, entryDn, entry ) );

        entryDn = dnFactory.create( "uid=akarasulu,ou=users,ou=system" );
        assertTrue( evaluator.evaluate( ss, apDn, entryDn, entry ) );

        entryDn = dnFactory.create( "ou=system" );
        assertFalse( evaluator.evaluate( ss, apDn, entryDn, entry ) );

        entryDn = dnFactory.create( "ou=twolevels,uid=akarasulu,ou=users,ou=system" );
        assertTrue( evaluator.evaluate( ss, apDn, entryDn, entry ) );

        entryDn = dnFactory.create( "ou=threelevels,ou=twolevels,uid=akarasulu,ou=users,ou=system" );
        assertFalse( evaluator.evaluate( ss, apDn, entryDn, entry ) );

        entryDn = dnFactory.create( "ou=fourlevels,ou=threelevels,ou=twolevels,uid=akarasulu,ou=users,ou=system" );
        assertFalse( evaluator.evaluate( ss, apDn, entryDn, entry ) );
    }


    @Test
    public void testWithMinMaxAndChopBefore() throws Exception
    {
        SubtreeSpecificationModifier modifier = new SubtreeSpecificationModifier();
        Set<Dn> chopBefore = new HashSet<Dn>();
        chopBefore.add( dnFactory.create( "uid=Tori Amos" ) );
        chopBefore.add( dnFactory.create( "ou=threelevels,ou=twolevels,uid=akarasulu" ) );
        modifier.setChopBeforeExclusions( chopBefore );
        modifier.setMinBaseDistance( 1 );
        modifier.setMaxBaseDistance( 3 );
        modifier.setBase( dnFactory.create( "ou=users" ) );
        SubtreeSpecification ss = modifier.getSubtreeSpecification();
        Dn apDn = dnFactory.create( "ou=system" );
        Dn entryDn = dnFactory.create( "ou=users,ou=system" );
        Entry entry = new DefaultEntry( schemaManager, entryDn );

        assertFalse( evaluator.evaluate( ss, apDn, entryDn, entry ) );

        entryDn = dnFactory.create( "uid=akarasulu,ou=users,ou=system" );
        assertTrue( evaluator.evaluate( ss, apDn, entryDn, entry ) );

        entryDn = dnFactory.create( "ou=system" );
        assertFalse( evaluator.evaluate( ss, apDn, entryDn, entry ) );

        entryDn = dnFactory.create( "ou=twolevels,uid=akarasulu,ou=users,ou=system" );
        assertTrue( evaluator.evaluate( ss, apDn, entryDn, entry ) );

        entryDn = dnFactory.create( "ou=threelevels,ou=twolevels,uid=akarasulu,ou=users,ou=system" );
        assertFalse( evaluator.evaluate( ss, apDn, entryDn, entry ) );

        entryDn = dnFactory.create( "ou=fourlevels,ou=threelevels,ou=twolevels,uid=akarasulu,ou=users,ou=system" );
        assertFalse( evaluator.evaluate( ss, apDn, entryDn, entry ) );
    }


    @Test
    public void testWithMinMaxAndSimpleRefinement() throws Exception
    {
        ExprNode refinement = FilterParser.parse( schemaManager, "(objectClass=person)" );
        refinement.accept( visitor );

        SubtreeSpecificationModifier modifier = new SubtreeSpecificationModifier();
        modifier.setRefinement( refinement );
        modifier.setMinBaseDistance( 1 );
        modifier.setMaxBaseDistance( 3 );
        modifier.setBase( dnFactory.create( "ou=users" ) );
        SubtreeSpecification ss = modifier.getSubtreeSpecification();
        Dn apDn = dnFactory.create( "ou=system" );
        Dn entryDn = dnFactory.create( "ou=users,ou=system" );
        Entry entry = new DefaultEntry( schemaManager, entryDn );
        entry.put( "objectClass", "person" );

        assertFalse( evaluator.evaluate( ss, apDn, entryDn, entry ) );

        entryDn = dnFactory.create( "uid=akarasulu,ou=users,ou=system" );
        assertTrue( evaluator.evaluate( ss, apDn, entryDn, entry ) );

        entryDn = dnFactory.create( "ou=system" );
        assertFalse( evaluator.evaluate( ss, apDn, entryDn, entry ) );

        entryDn = dnFactory.create( "ou=twolevels,uid=akarasulu,ou=users,ou=system" );
        assertTrue( evaluator.evaluate( ss, apDn, entryDn, entry ) );

        entryDn = dnFactory.create( "ou=threelevels,ou=twolevels,uid=akarasulu,ou=users,ou=system" );
        assertTrue( evaluator.evaluate( ss, apDn, entryDn, entry ) );

        entryDn = dnFactory.create( "ou=fourlevels,ou=threelevels,ou=twolevels,uid=akarasulu,ou=users,ou=system" );
        assertFalse( evaluator.evaluate( ss, apDn, entryDn, entry ) );

        // now change the refinement so the entry is rejected
        entry = new DefaultEntry( schemaManager, entryDn );
        entry.put( "objectClass", "organizationalUnit" );

        assertFalse( evaluator.evaluate( ss, apDn, entryDn, entry ) );

        entryDn = dnFactory.create( "uid=akarasulu,ou=users,ou=system" );
        assertFalse( evaluator.evaluate( ss, apDn, entryDn, entry ) );

        entryDn = dnFactory.create( "ou=system" );
        assertFalse( evaluator.evaluate( ss, apDn, entryDn, entry ) );

        entryDn = dnFactory.create( "ou=twolevels,uid=akarasulu,ou=users,ou=system" );
        assertFalse( evaluator.evaluate( ss, apDn, entryDn, entry ) );

        entryDn = dnFactory.create( "ou=threelevels,ou=twolevels,uid=akarasulu,ou=users,ou=system" );
        assertFalse( evaluator.evaluate( ss, apDn, entryDn, entry ) );

        entryDn = dnFactory.create( "ou=fourlevels,ou=threelevels,ou=twolevels,uid=akarasulu,ou=users,ou=system" );
        assertFalse( evaluator.evaluate( ss, apDn, entryDn, entry ) );

    }


    @Test
    public void testWithFilter() throws Exception
    {
        ExprNode filter = FilterParser.parse( schemaManager, "(&(cn=Ersin)(objectClass=person))" );
        filter.accept( visitor );

        SubtreeSpecificationModifier modifier = new SubtreeSpecificationModifier();
        modifier.setRefinement( filter );
        modifier.setMinBaseDistance( 1 );
        modifier.setMaxBaseDistance( 3 );
        modifier.setBase( dnFactory.create( "ou=users" ) );
        SubtreeSpecification ss = modifier.getSubtreeSpecification();
        Dn apDn = dnFactory.create( "ou=system" );
        Dn entryDn = dnFactory.create( "ou=users,ou=system" );

        Entry entry = new DefaultEntry( schemaManager, entryDn );
        entry.put( "objectClass", "person" );
        entry.put( "cn", "Ersin" );

        assertFalse( evaluator.evaluate( ss, apDn, entryDn, entry ) );

        entryDn = dnFactory.create( "cn=Ersin,ou=users,ou=system" );
        assertTrue( evaluator.evaluate( ss, apDn, entryDn, entry ) );

        // now change the filter so the entry is rejected
        entry = new DefaultEntry( schemaManager, entryDn );
        entry.put( "objectClass", "person" );
        entry.put( "cn", "Alex" );

        assertFalse( evaluator.evaluate( ss, apDn, entryDn, entry ) );

        entryDn = dnFactory.create( "cn=Alex,ou=users,ou=system" );
        assertFalse( evaluator.evaluate( ss, apDn, entryDn, entry ) );
    }
}
