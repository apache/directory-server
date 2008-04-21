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
package org.apache.directory.server.xdbm.search.impl;


import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.apache.directory.server.xdbm.Store;
import org.apache.directory.server.xdbm.ForwardIndexEntry;
import org.apache.directory.server.xdbm.tools.StoreUtils;
import org.apache.directory.server.schema.registries.*;
import org.apache.directory.server.schema.bootstrap.*;
import org.apache.directory.server.schema.SerializableComparator;
import org.apache.directory.server.core.partition.impl.btree.jdbm.JdbmStore;
import org.apache.directory.server.core.partition.impl.btree.jdbm.JdbmIndex;
import org.apache.directory.shared.ldap.constants.SchemaConstants;
import org.apache.directory.shared.ldap.filter.LessEqNode;
import org.apache.directory.shared.ldap.schema.*;
import org.apache.directory.shared.ldap.message.AttributesImpl;
import org.apache.directory.shared.ldap.name.LdapDN;
import org.apache.commons.io.FileUtils;
import org.junit.Before;
import org.junit.After;
import org.junit.Test;import static org.junit.Assert.assertTrue;import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import javax.naming.directory.Attributes;
import javax.naming.NamingException;
import java.io.File;
import java.util.Set;
import java.util.HashSet;
import java.util.Comparator;

import jdbm.helper.StringComparator;


/**
 * Tests the LessEqEvaluator and LessEqCursor classes for correct operation.
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 * @version $$Rev$$
 */
public class LessEqTest
{
    public static final Logger LOG = LoggerFactory.getLogger( OneLevelScopeTest.class );


    File wkdir;
    Store<Attributes> store;
    Registries registries = null;
    AttributeTypeRegistry attributeRegistry;


    public LessEqTest() throws Exception
    {
        // setup the standard registries
        BootstrapSchemaLoader loader = new BootstrapSchemaLoader();
        OidRegistry oidRegistry = new DefaultOidRegistry();
        registries = new DefaultRegistries( "bootstrap", loader, oidRegistry );
        SerializableComparator.setRegistry( registries.getComparatorRegistry() );

        // load essential bootstrap schemas
        Set<Schema> bootstrapSchemas = new HashSet<Schema>();
        bootstrapSchemas.add( new ApachemetaSchema() );
        bootstrapSchemas.add( new ApacheSchema() );
        bootstrapSchemas.add( new CoreSchema() );
        bootstrapSchemas.add( new SystemSchema() );
        bootstrapSchemas.add( new CollectiveSchema() );
        loader.loadWithDependencies( bootstrapSchemas, registries );
        attributeRegistry = registries.getAttributeTypeRegistry();
    }


    @Before
    public void createStore() throws Exception
    {
        destryStore();

        // setup the working directory for the store
        wkdir = File.createTempFile( getClass().getSimpleName(), "db" );
        wkdir.delete();
        wkdir = new File( wkdir.getParentFile(), getClass().getSimpleName() );
        wkdir.mkdirs();

        // initialize the store
        store = new JdbmStore<Attributes>();
        store.setName( "example" );
        store.setCacheSize( 10 );
        store.setWorkingDirectory( wkdir );
        store.setSyncOnWrite( false );

        store.addIndex( new JdbmIndex( SchemaConstants.OU_AT_OID ) );
        store.addIndex( new JdbmIndex( SchemaConstants.CN_AT_OID ) );
        store.addIndex( new JdbmIndex( SchemaConstants.POSTALCODE_AT_OID ) );

        StoreUtils.loadExampleData( store, registries );
        LOG.debug( "Created new store" );
    }


    @After
    public void destryStore() throws Exception
    {
        if ( store != null )
        {
            store.destroy();
        }

        store = null;
        if ( wkdir != null )
        {
            FileUtils.deleteDirectory( wkdir );
        }

        wkdir = null;
    }


    @Test
    public void testEvaluatorIndexed() throws Exception
    {
        LessEqNode node = new LessEqNode( SchemaConstants.POSTALCODE_AT_OID, "3" );
        LessEqEvaluator evaluator = new LessEqEvaluator( node, store, registries );
        ForwardIndexEntry<String,Attributes> indexEntry = new ForwardIndexEntry<String,Attributes>();
        assertEquals( node, evaluator.getExpression() );
        assertEquals( SchemaConstants.POSTALCODE_AT_OID, evaluator.getAttributeType().getOid() );
        assertNotNull( evaluator.getNormalizer() );
        assertNotNull( evaluator.getComparator() );

        indexEntry.setId( 1L );
        assertTrue( evaluator.evaluate( indexEntry ) );

        indexEntry = new ForwardIndexEntry<String,Attributes>();
        indexEntry.setId( 4L );
        assertTrue( evaluator.evaluate( indexEntry ) );

        indexEntry = new ForwardIndexEntry<String,Attributes>();
        indexEntry.setId( 5L );
        assertTrue( evaluator.evaluate( indexEntry ) );

        indexEntry = new ForwardIndexEntry<String,Attributes>();
        indexEntry.setId( 6L );
        assertFalse( evaluator.evaluate( indexEntry ) );

        indexEntry = new ForwardIndexEntry<String,Attributes>();
        indexEntry.setId( 7L );
        assertFalse( evaluator.evaluate( indexEntry ) );

        indexEntry = new ForwardIndexEntry<String,Attributes>();
        indexEntry.setId( 8L );
        assertFalse( evaluator.evaluate( indexEntry ) );

        indexEntry = new ForwardIndexEntry<String,Attributes>();
        indexEntry.setId( 9L );
        assertFalse( evaluator.evaluate( indexEntry ) );

        indexEntry = new ForwardIndexEntry<String,Attributes>();
        indexEntry.setId( 10L );
        assertFalse( evaluator.evaluate( indexEntry ) );
    }


    @Test
    public void testEvaluatorWithDescendantValue() throws Exception
    {
        LessEqNode node = new LessEqNode( SchemaConstants.STREET_AT_OID, "2" );
        LessEqEvaluator evaluator = new LessEqEvaluator( node, store, registries );
        ForwardIndexEntry<String,Attributes> indexEntry = new ForwardIndexEntry<String,Attributes>();
        assertEquals( node, evaluator.getExpression() );
        assertEquals( SchemaConstants.STREET_AT_OID, evaluator.getAttributeType().getOid() );
        assertNotNull( evaluator.getNormalizer() );
        assertNotNull( evaluator.getComparator() );

        LdapDN dn = new LdapDN( "cn=jane doe,o=good times co." );
        dn.normalize( registries.getAttributeTypeRegistry().getNormalizerMapping() );
        AttributesImpl attrs = new AttributesImpl( "objectClass", "person", true );
        attrs.put( "c-street", "1" );
        attrs.put( "cn", "jane doe" );
        attrs.put( "sn", "doe" );
        store.add( dn, attrs );

        indexEntry.setId( 12L );
        assertTrue( evaluator.evaluate( indexEntry ) );
    }


    @Test
    public void testEvaluatorWithoutDescendants() throws Exception
    {
        LessEqNode node = new LessEqNode( SchemaConstants.C_POSTALCODE_AT_OID, "2" );
        LessEqEvaluator evaluator = new LessEqEvaluator( node, store, registries );
        ForwardIndexEntry<String,Attributes> indexEntry = new ForwardIndexEntry<String,Attributes>();
        assertEquals( node, evaluator.getExpression() );
        assertEquals( SchemaConstants.C_POSTALCODE_AT_OID, evaluator.getAttributeType().getOid() );
        assertNotNull( evaluator.getNormalizer() );
        assertNotNull( evaluator.getComparator() );

        indexEntry.setId( 1L );
        assertFalse( evaluator.evaluate( indexEntry ) );
    }


    @Test
    public void testEvaluatorNotIndexed() throws Exception
    {
        LessEqNode node = new LessEqNode( SchemaConstants.POSTOFFICEBOX_AT_OID, "3" );
        LessEqEvaluator evaluator = new LessEqEvaluator( node, store, registries );
        ForwardIndexEntry<String,Attributes> indexEntry = new ForwardIndexEntry<String,Attributes>();
        assertEquals( node, evaluator.getExpression() );
        assertEquals( SchemaConstants.POSTOFFICEBOX_AT_OID, evaluator.getAttributeType().getOid() );
        assertNotNull( evaluator.getNormalizer() );
        assertNotNull( evaluator.getComparator() );

        indexEntry.setId( 1L );
        assertTrue( evaluator.evaluate( indexEntry ) );

        indexEntry = new ForwardIndexEntry<String,Attributes>();
        indexEntry.setId( 4L );
        assertTrue( evaluator.evaluate( indexEntry ) );

        indexEntry = new ForwardIndexEntry<String,Attributes>();
        indexEntry.setId( 5L );
        assertTrue( evaluator.evaluate( indexEntry ) );

        indexEntry = new ForwardIndexEntry<String,Attributes>();
        indexEntry.setId( 6L );
        assertFalse( evaluator.evaluate( indexEntry ) );

        indexEntry = new ForwardIndexEntry<String,Attributes>();
        indexEntry.setId( 7L );
        assertFalse( evaluator.evaluate( indexEntry ) );

        indexEntry = new ForwardIndexEntry<String,Attributes>();
        indexEntry.setId( 8L );
        assertFalse( evaluator.evaluate( indexEntry ) );

        indexEntry = new ForwardIndexEntry<String,Attributes>();
        indexEntry.setId( 9L );
        assertFalse( evaluator.evaluate( indexEntry ) );

        indexEntry = new ForwardIndexEntry<String,Attributes>();
        indexEntry.setId( 10L );
        assertFalse( evaluator.evaluate( indexEntry ) );
    }


    @Test ( expected = IllegalStateException.class )
    public void testEvaluatorAttributeNoMatchingRule() throws Exception
    {
        AttributeType at = new AttributeType()
        {

            public boolean isSingleValue()
            {
                return false;
            }


            public boolean isCanUserModify()
            {
                return false;
            }


            public boolean isCollective()
            {
                return false;
            }


            public UsageEnum getUsage()
            {
                return null;
            }


            public AttributeType getSuperior() throws NamingException
            {
                return null;
            }


            public Syntax getSyntax() throws NamingException
            {
                return null;
            }


            public int getLength()
            {
                return 0;
            }


            public MatchingRule getEquality() throws NamingException
            {
                return null;
            }


            public MatchingRule getOrdering() throws NamingException
            {
                return null;
            }


            public MatchingRule getSubstr() throws NamingException
            {
                return null;
            }


            public boolean isAncestorOf( AttributeType descendant ) throws NamingException
            {
                return false;
            }


            public boolean isDescentantOf( AttributeType ancestor ) throws NamingException
            {
                return false;
            }


            public boolean isObsolete()
            {
                return false;
            }


            public String getOid()
            {
                return SchemaConstants.ATTRIBUTE_TYPES_AT_OID + ".2000";
            }


            public String[] getNames()
            {
                return new String[] { "bogus" };
            }


            public String getName()
            {
                return "bogus";
            }


            public String getDescription()
            {
                return "";
            }


            public String getSchema()
            {
                return "other";
            }


            public void setSchema( String schemaName )
            {
            }
        };
        registries.getAttributeTypeRegistry().register( at );
        LessEqNode node = new LessEqNode( at.getOid(), "3" );
        new LessEqEvaluator( node, store, registries );
        registries.getAttributeTypeRegistry().unregister( at.getOid() );
    }


    @Test
    public void testEvaluatorAttributeOrderingMatchingRule() throws Exception
    {
        AttributeType at = new AttributeType()
        {

            public boolean isSingleValue()
            {
                return false;
            }


            public boolean isCanUserModify()
            {
                return false;
            }


            public boolean isCollective()
            {
                return false;
            }


            public UsageEnum getUsage()
            {
                return null;
            }


            public AttributeType getSuperior() throws NamingException
            {
                return null;
            }


            public Syntax getSyntax() throws NamingException
            {
                return null;
            }


            public int getLength()
            {
                return 0;
            }


            public MatchingRule getEquality() throws NamingException
            {
                return null;
            }


            public MatchingRule getOrdering() throws NamingException
            {
                return new MatchingRule()
                {

                    public Syntax getSyntax() throws NamingException
                    {
                        return null;
                    }


                    public Comparator getComparator() throws NamingException
                    {
                        return new StringComparator();
                    }


                    public Normalizer getNormalizer() throws NamingException
                    {
                        return new NoOpNormalizer();
                    }


                    public boolean isObsolete()
                    {
                        return false;
                    }


                    public String getOid()
                    {
                        return null;
                    }


                    public String[] getNames()
                    {
                        return new String[0];
                    }


                    public String getName()
                    {
                        return null;
                    }


                    public String getDescription()
                    {
                        return null;
                    }


                    public String getSchema()
                    {
                        return null;
                    }


                    public void setSchema( String schemaName )
                    {
                    }
                };
            }


            public MatchingRule getSubstr() throws NamingException
            {
                return null;
            }


            public boolean isAncestorOf( AttributeType descendant ) throws NamingException
            {
                return false;
            }


            public boolean isDescentantOf( AttributeType ancestor ) throws NamingException
            {
                return false;
            }


            public boolean isObsolete()
            {
                return false;
            }


            public String getOid()
            {
                return SchemaConstants.ATTRIBUTE_TYPES_AT_OID + ".2000";
            }


            public String[] getNames()
            {
                return new String[] { "bogus" };
            }


            public String getName()
            {
                return "bogus";
            }


            public String getDescription()
            {
                return "";
            }


            public String getSchema()
            {
                return "other";
            }


            public void setSchema( String schemaName )
            {
            }
        };
        registries.getAttributeTypeRegistry().register( at );
        LessEqNode node = new LessEqNode( at.getOid(), "3" );
        new LessEqEvaluator( node, store, registries );
        registries.getAttributeTypeRegistry().unregister( at.getOid() );
    }
}
