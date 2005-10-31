/*
 *   Copyright 2004 The Apache Software Foundation
 *
 *   Licensed under the Apache License, Version 2.0 (the "License");
 *   you may not use this file except in compliance with the License.
 *   You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *   Unless required by applicable law or agreed to in writing, software
 *   distributed under the License is distributed on an "AS IS" BASIS,
 *   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *   See the License for the specific language governing permissions and
 *   limitations under the License.
 *
 */
package org.apache.ldap.server.subtree;


import junit.framework.TestCase;
import org.apache.ldap.server.schema.GlobalRegistries;
import org.apache.ldap.server.schema.OidRegistry;
import org.apache.ldap.server.schema.bootstrap.*;
import org.apache.ldap.common.subtree.SubtreeSpecificationModifier;
import org.apache.ldap.common.subtree.SubtreeSpecification;
import org.apache.ldap.common.name.LdapName;
import org.apache.ldap.common.filter.FilterParserImpl;
import org.apache.ldap.common.filter.FilterParser;
import org.apache.ldap.common.filter.ExprNode;

import javax.naming.NamingException;
import javax.naming.Name;
import javax.naming.directory.Attribute;
import javax.naming.directory.BasicAttribute;
import java.util.Set;
import java.util.HashSet;


/**
 * Unit test cases for the SubtreeEvaluator.
 *
 * @author <a href="mailto:directory-dev@incubator.apache.org">Apache Directory Project</a>
 * @version $Rev$
 */
public class SubtreeEvaluatorTest extends TestCase
{
    private GlobalRegistries registries;
    private SubtreeEvaluator evaluator;


    private void init() throws NamingException
    {
        BootstrapRegistries bsRegistries = new BootstrapRegistries();
        registries = new GlobalRegistries( bsRegistries );
        BootstrapSchemaLoader loader = new BootstrapSchemaLoader();
        Set schemas = new HashSet();
        schemas.add( new SystemSchema() );
        schemas.add( new ApacheSchema() );
        schemas.add( new CoreSchema() );
        schemas.add( new CosineSchema() );
        schemas.add( new InetorgpersonSchema() );
        schemas.add( new JavaSchema() );
        loader.load( schemas, bsRegistries );
    }


    protected void setUp() throws Exception
    {
        init();
        OidRegistry registry = registries.getOidRegistry();
        evaluator = new SubtreeEvaluator( registry );
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
        Name apDn = new LdapName( "ou=system" );
        Name entryDn = new LdapName( "ou=users,ou=system" );
        Attribute objectClasses = new BasicAttribute( "objectClass" );

        assertTrue( evaluator.evaluate( ss, apDn, entryDn, objectClasses ) );

        entryDn = new LdapName( "ou=system" );
        assertTrue( evaluator.evaluate( ss, apDn, entryDn, objectClasses ) );

        entryDn = new LdapName( "ou=abc" );
        assertFalse( evaluator.evaluate( ss, apDn, entryDn, objectClasses ) );
    }


    public void testWithBase() throws Exception
    {
        SubtreeSpecificationModifier modifier = new SubtreeSpecificationModifier();
        modifier.setBase( new LdapName( "ou=users" ) );
        SubtreeSpecification ss = modifier.getSubtreeSpecification();
        Name apDn = new LdapName( "ou=system" );
        Name entryDn = new LdapName( "ou=users,ou=system" );
        Attribute objectClasses = new BasicAttribute( "objectClass" );

        assertTrue( evaluator.evaluate( ss, apDn, entryDn, objectClasses ) );

        entryDn = new LdapName( "uid=akarasulu,ou=users,ou=system" );
        assertTrue( evaluator.evaluate( ss, apDn, entryDn, objectClasses ) );

        entryDn = new LdapName( "ou=system" );
        assertFalse( evaluator.evaluate( ss, apDn, entryDn, objectClasses ) );
    }


    public void testWithMinMax() throws Exception
    {
        SubtreeSpecificationModifier modifier = new SubtreeSpecificationModifier();
        modifier.setMinBaseDistance( 1 );
        modifier.setMaxBaseDistance( 3 );
        modifier.setBase( new LdapName( "ou=users" ) );
        SubtreeSpecification ss = modifier.getSubtreeSpecification();
        Name apDn = new LdapName( "ou=system" );
        Name entryDn = new LdapName( "ou=users,ou=system" );
        Attribute objectClasses = new BasicAttribute( "objectClass" );

        assertFalse( evaluator.evaluate( ss, apDn, entryDn, objectClasses ) );

        entryDn = new LdapName( "uid=akarasulu,ou=users,ou=system" );
        assertTrue( evaluator.evaluate( ss, apDn, entryDn, objectClasses ) );

        entryDn = new LdapName( "ou=system" );
        assertFalse( evaluator.evaluate( ss, apDn, entryDn, objectClasses ) );

        entryDn = new LdapName( "ou=twolevels,uid=akarasulu,ou=users,ou=system" );
        assertTrue( evaluator.evaluate( ss, apDn, entryDn, objectClasses ) );

        entryDn = new LdapName( "ou=threelevels,ou=twolevels,uid=akarasulu,ou=users,ou=system" );
        assertTrue( evaluator.evaluate( ss, apDn, entryDn, objectClasses ) );

        entryDn = new LdapName( "ou=fourlevels,ou=threelevels,ou=twolevels,uid=akarasulu,ou=users,ou=system" );
        assertFalse( evaluator.evaluate( ss, apDn, entryDn, objectClasses ) );
    }


    public void testWithMinMaxAndChopAfter() throws Exception
    {
        SubtreeSpecificationModifier modifier = new SubtreeSpecificationModifier();
        Set chopAfter = new HashSet();
        chopAfter.add( new LdapName( "uid=Tori Amos" ) );
        chopAfter.add( new LdapName( "ou=twolevels,uid=akarasulu" ) );
        modifier.setChopAfterExclusions( chopAfter );
        modifier.setMinBaseDistance( 1 );
        modifier.setMaxBaseDistance( 3 );
        modifier.setBase( new LdapName( "ou=users" ) );
        SubtreeSpecification ss = modifier.getSubtreeSpecification();
        Name apDn = new LdapName( "ou=system" );
        Name entryDn = new LdapName( "ou=users,ou=system" );
        Attribute objectClasses = new BasicAttribute( "objectClass" );

        assertFalse( evaluator.evaluate( ss, apDn, entryDn, objectClasses ) );

        entryDn = new LdapName( "uid=akarasulu,ou=users,ou=system" );
        assertTrue( evaluator.evaluate( ss, apDn, entryDn, objectClasses ) );

        entryDn = new LdapName( "ou=system" );
        assertFalse( evaluator.evaluate( ss, apDn, entryDn, objectClasses ) );

        entryDn = new LdapName( "ou=twolevels,uid=akarasulu,ou=users,ou=system" );
        assertTrue( evaluator.evaluate( ss, apDn, entryDn, objectClasses ) );

        entryDn = new LdapName( "ou=threelevels,ou=twolevels,uid=akarasulu,ou=users,ou=system" );
        assertFalse( evaluator.evaluate( ss, apDn, entryDn, objectClasses ) );

        entryDn = new LdapName( "ou=fourlevels,ou=threelevels,ou=twolevels,uid=akarasulu,ou=users,ou=system" );
        assertFalse( evaluator.evaluate( ss, apDn, entryDn, objectClasses ) );
    }


    public void testWithMinMaxAndChopBefore() throws Exception
    {
        SubtreeSpecificationModifier modifier = new SubtreeSpecificationModifier();
        Set chopBefore = new HashSet();
        chopBefore.add( new LdapName( "uid=Tori Amos" ) );
        chopBefore.add( new LdapName( "ou=threelevels,ou=twolevels,uid=akarasulu" ) );
        modifier.setChopBeforeExclusions( chopBefore );
        modifier.setMinBaseDistance( 1 );
        modifier.setMaxBaseDistance( 3 );
        modifier.setBase( new LdapName( "ou=users" ) );
        SubtreeSpecification ss = modifier.getSubtreeSpecification();
        Name apDn = new LdapName( "ou=system" );
        Name entryDn = new LdapName( "ou=users,ou=system" );
        Attribute objectClasses = new BasicAttribute( "objectClass" );

        assertFalse( evaluator.evaluate( ss, apDn, entryDn, objectClasses ) );

        entryDn = new LdapName( "uid=akarasulu,ou=users,ou=system" );
        assertTrue( evaluator.evaluate( ss, apDn, entryDn, objectClasses ) );

        entryDn = new LdapName( "ou=system" );
        assertFalse( evaluator.evaluate( ss, apDn, entryDn, objectClasses ) );

        entryDn = new LdapName( "ou=twolevels,uid=akarasulu,ou=users,ou=system" );
        assertTrue( evaluator.evaluate( ss, apDn, entryDn, objectClasses ) );

        entryDn = new LdapName( "ou=threelevels,ou=twolevels,uid=akarasulu,ou=users,ou=system" );
        assertFalse( evaluator.evaluate( ss, apDn, entryDn, objectClasses ) );

        entryDn = new LdapName( "ou=fourlevels,ou=threelevels,ou=twolevels,uid=akarasulu,ou=users,ou=system" );
        assertFalse( evaluator.evaluate( ss, apDn, entryDn, objectClasses ) );
    }


    public void testWithMinMaxAndSimpleRefinement() throws Exception
    {
        FilterParser parser = new FilterParserImpl();
        ExprNode refinement = parser.parse( "( objectClass = person )" );

        SubtreeSpecificationModifier modifier = new SubtreeSpecificationModifier();
        modifier.setRefinement( refinement );
        modifier.setMinBaseDistance( 1 );
        modifier.setMaxBaseDistance( 3 );
        modifier.setBase( new LdapName( "ou=users" ) );
        SubtreeSpecification ss = modifier.getSubtreeSpecification();
        Name apDn = new LdapName( "ou=system" );
        Name entryDn = new LdapName( "ou=users,ou=system" );
        Attribute objectClasses = new BasicAttribute( "objectClass", "person" );

        assertFalse( evaluator.evaluate( ss, apDn, entryDn, objectClasses ) );

        entryDn = new LdapName( "uid=akarasulu,ou=users,ou=system" );
        assertTrue( evaluator.evaluate( ss, apDn, entryDn, objectClasses ) );

        entryDn = new LdapName( "ou=system" );
        assertFalse( evaluator.evaluate( ss, apDn, entryDn, objectClasses ) );

        entryDn = new LdapName( "ou=twolevels,uid=akarasulu,ou=users,ou=system" );
        assertTrue( evaluator.evaluate( ss, apDn, entryDn, objectClasses ) );

        entryDn = new LdapName( "ou=threelevels,ou=twolevels,uid=akarasulu,ou=users,ou=system" );
        assertTrue( evaluator.evaluate( ss, apDn, entryDn, objectClasses ) );

        entryDn = new LdapName( "ou=fourlevels,ou=threelevels,ou=twolevels,uid=akarasulu,ou=users,ou=system" );
        assertFalse( evaluator.evaluate( ss, apDn, entryDn, objectClasses ) );

        // now change the refinement so the entry is rejected
        objectClasses = new BasicAttribute( "objectClass", "organizationalUnit" );

        assertFalse( evaluator.evaluate( ss, apDn, entryDn, objectClasses ) );

        entryDn = new LdapName( "uid=akarasulu,ou=users,ou=system" );
        assertFalse( evaluator.evaluate( ss, apDn, entryDn, objectClasses ) );

        entryDn = new LdapName( "ou=system" );
        assertFalse( evaluator.evaluate( ss, apDn, entryDn, objectClasses ) );

        entryDn = new LdapName( "ou=twolevels,uid=akarasulu,ou=users,ou=system" );
        assertFalse( evaluator.evaluate( ss, apDn, entryDn, objectClasses ) );

        entryDn = new LdapName( "ou=threelevels,ou=twolevels,uid=akarasulu,ou=users,ou=system" );
        assertFalse( evaluator.evaluate( ss, apDn, entryDn, objectClasses ) );

        entryDn = new LdapName( "ou=fourlevels,ou=threelevels,ou=twolevels,uid=akarasulu,ou=users,ou=system" );
        assertFalse( evaluator.evaluate( ss, apDn, entryDn, objectClasses ) );

    }
}
