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


import java.util.HashSet;
import java.util.Set;

import javax.naming.Name;
import javax.naming.NamingException;
import javax.naming.directory.Attribute;
import javax.naming.directory.Attributes;
import javax.naming.directory.BasicAttribute;
import javax.naming.directory.BasicAttributes;

import junit.framework.TestCase;

import org.apache.directory.server.schema.bootstrap.ApacheSchema;
import org.apache.directory.server.schema.bootstrap.BootstrapSchemaLoader;
import org.apache.directory.server.schema.bootstrap.CoreSchema;
import org.apache.directory.server.schema.bootstrap.Schema;
import org.apache.directory.server.schema.bootstrap.SystemSchema;
import org.apache.directory.server.schema.registries.DefaultRegistries;
import org.apache.directory.server.schema.registries.Registries;
import org.apache.directory.shared.ldap.filter.ExprNode;
import org.apache.directory.shared.ldap.filter.FilterParser;
import org.apache.directory.shared.ldap.filter.FilterParserImpl;
import org.apache.directory.shared.ldap.name.LdapDN;
import org.apache.directory.shared.ldap.subtree.SubtreeSpecification;
import org.apache.directory.shared.ldap.subtree.SubtreeSpecificationModifier;


/**
 * Unit test cases for the SubtreeEvaluator.
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 * @version $Rev$
 */
public class SubtreeEvaluatorTest extends TestCase
{
    private Registries registries;
    private SubtreeEvaluator evaluator;


    private void init() throws NamingException
    {
        BootstrapSchemaLoader loader = new BootstrapSchemaLoader();
        DefaultRegistries bsRegistries = new DefaultRegistries( "bootstrap", loader );
        registries = bsRegistries;
        Set<Schema> schemas = new HashSet<Schema>();
        schemas.add( new SystemSchema() );
        schemas.add( new ApacheSchema() );
        schemas.add( new CoreSchema() );
        loader.loadWithDependencies( schemas, bsRegistries );
    }


    protected void setUp() throws Exception
    {
        init();
        evaluator = new SubtreeEvaluator( registries.getOidRegistry(), registries.getAttributeTypeRegistry() );
    }


    protected void tearDown() throws Exception
    {
        evaluator = null;
        registries = null;
    }


    public void testDefaults() throws Exception
    {
        SubtreeSpecificationModifier modifier = new SubtreeSpecificationModifier();
        SubtreeSpecification ss = modifier.getSubtreeSpecification();
        Name apDn = new LdapDN( "ou=system" );
        Name entryDn = new LdapDN( "ou=users,ou=system" );
        Attribute objectClasses = new BasicAttribute( "objectClass" );
        Attributes entry = new BasicAttributes();
        entry.put( objectClasses );

        assertTrue( evaluator.evaluate( ss, apDn, entryDn, entry ) );

        entryDn = new LdapDN( "ou=system" );
        assertTrue( evaluator.evaluate( ss, apDn, entryDn, entry ) );

        entryDn = new LdapDN( "ou=abc" );
        assertFalse( evaluator.evaluate( ss, apDn, entryDn, entry ) );
    }


    public void testWithBase() throws Exception
    {
        SubtreeSpecificationModifier modifier = new SubtreeSpecificationModifier();
        modifier.setBase( new LdapDN( "ou=users" ) );
        SubtreeSpecification ss = modifier.getSubtreeSpecification();
        Name apDn = new LdapDN( "ou=system" );
        Name entryDn = new LdapDN( "ou=users,ou=system" );
        Attribute objectClasses = new BasicAttribute( "objectClass" );
        Attributes entry = new BasicAttributes();
        entry.put( objectClasses );

        assertTrue( evaluator.evaluate( ss, apDn, entryDn, entry ) );

        entryDn = new LdapDN( "uid=akarasulu,ou=users,ou=system" );
        assertTrue( evaluator.evaluate( ss, apDn, entryDn, entry ) );

        entryDn = new LdapDN( "ou=system" );
        assertFalse( evaluator.evaluate( ss, apDn, entryDn, entry ) );
    }


    public void testWithMinMax() throws Exception
    {
        SubtreeSpecificationModifier modifier = new SubtreeSpecificationModifier();
        modifier.setMinBaseDistance( 1 );
        modifier.setMaxBaseDistance( 3 );
        modifier.setBase( new LdapDN( "ou=users" ) );
        SubtreeSpecification ss = modifier.getSubtreeSpecification();
        Name apDn = new LdapDN( "ou=system" );
        Name entryDn = new LdapDN( "ou=users,ou=system" );
        Attribute objectClasses = new BasicAttribute( "objectClass" );
        Attributes entry = new BasicAttributes();
        entry.put( objectClasses );

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
        Name apDn = new LdapDN( "ou=system" );
        Name entryDn = new LdapDN( "ou=users,ou=system" );
        Attribute objectClasses = new BasicAttribute( "objectClass" );
        Attributes entry = new BasicAttributes();
        entry.put( objectClasses );

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
        Name apDn = new LdapDN( "ou=system" );
        Name entryDn = new LdapDN( "ou=users,ou=system" );
        Attribute objectClasses = new BasicAttribute( "objectClass" );
        Attributes entry = new BasicAttributes();
        entry.put( objectClasses );

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


    public void testWithMinMaxAndSimpleRefinement() throws Exception
    {
        FilterParser parser = new FilterParserImpl();
        ExprNode refinement = parser.parse( "( objectClass = person )" );

        SubtreeSpecificationModifier modifier = new SubtreeSpecificationModifier();
        modifier.setRefinement( refinement );
        modifier.setMinBaseDistance( 1 );
        modifier.setMaxBaseDistance( 3 );
        modifier.setBase( new LdapDN( "ou=users" ) );
        SubtreeSpecification ss = modifier.getSubtreeSpecification();
        Name apDn = new LdapDN( "ou=system" );
        Name entryDn = new LdapDN( "ou=users,ou=system" );
        Attribute objectClasses = new BasicAttribute( "objectClass", "person" );
        Attributes entry = new BasicAttributes();
        entry.put( objectClasses );

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
        objectClasses = new BasicAttribute( "objectClass", "organizationalUnit" );
        entry = new BasicAttributes();
        entry.put( objectClasses );

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
    
    
    public void testWithFilter() throws Exception
    {
        FilterParser parser = new FilterParserImpl();
        ExprNode filter = parser.parse( "(&(cn=Ersin)(objectClass=person))" );

        SubtreeSpecificationModifier modifier = new SubtreeSpecificationModifier();
        modifier.setRefinement( filter );
        modifier.setMinBaseDistance( 1 );
        modifier.setMaxBaseDistance( 3 );
        modifier.setBase( new LdapDN( "ou=users" ) );
        SubtreeSpecification ss = modifier.getSubtreeSpecification();
        Name apDn = new LdapDN( "ou=system" );
        Name entryDn = new LdapDN( "ou=users,ou=system" );
        Attribute objectClasses = new BasicAttribute( "objectClass", "person" );
        Attribute cn = new BasicAttribute( "cn", "Ersin" );
        Attributes entry = new BasicAttributes();
        entry.put( objectClasses );
        entry.put( cn );

        assertFalse( evaluator.evaluate( ss, apDn, entryDn, entry ) );

        entryDn = new LdapDN( "cn=Ersin,ou=users,ou=system" );
        assertTrue( evaluator.evaluate( ss, apDn, entryDn, entry ) );

        // now change the filter so the entry is rejected
        objectClasses = new BasicAttribute( "objectClass", "person" );
        cn = new BasicAttribute( "cn", "Alex" );
        entry = new BasicAttributes();
        entry.put( objectClasses );
        entry.put( cn );

        assertFalse( evaluator.evaluate( ss, apDn, entryDn, entry ) );

        entryDn = new LdapDN( "cn=Alex,ou=users,ou=system" );
        assertFalse( evaluator.evaluate( ss, apDn, entryDn, entry ) );
    }
}
