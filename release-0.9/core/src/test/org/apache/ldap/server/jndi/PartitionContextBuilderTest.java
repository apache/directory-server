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
package org.apache.ldap.server.jndi;


import junit.framework.TestCase;
import org.apache.ldap.common.message.LockableAttributeImpl;
import org.apache.ldap.common.message.LockableAttributesImpl;
import org.apache.ldap.common.util.ArrayUtils;
import org.apache.ldap.server.ContextPartitionConfig;

import javax.naming.NamingException;
import java.util.Hashtable;


/**
 * Testcase which tests the correct operation of the PartitionContextBuilder.
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 * @version $Rev$
 */
public class PartitionContextBuilderTest extends TestCase
{
    /**
     * Tests {@link PartitionConfigBuilder#getContextPartitionConfigs(Hashtable)}
     * using an empty Hashtable.
     */
    public void testEmptyEnvironment() throws NamingException
    {
        Hashtable env = new Hashtable();
        ContextPartitionConfig[] configs = null;

        configs = PartitionConfigBuilder.getContextPartitionConfigs( env );
        assertNotNull( configs );
        assertEquals( 0, configs.length );
    }


    /**
     * Tests {@link PartitionConfigBuilder#getContextPartitionConfigs(Hashtable)}
     * using a Hashtable with only partition names.
     */
    public void testPartialConfig() throws NamingException
    {
        Hashtable env = new Hashtable();
        ContextPartitionConfig[] configs = null;

        // setup everything and build config bean
        env.put( EnvKeys.PARTITIONS, "apache test" );
        configs = PartitionConfigBuilder.getContextPartitionConfigs( env );

        // start testing return values
        assertNotNull( configs );
        assertEquals( 2, configs.length );

        // test the apache config bean
        assertEquals( "apache", configs[0].getId() );
        assertNull( configs[0].getSuffix() );
        assertNotNull( configs[0].getAttributes() );
        assertEquals( 0, configs[0].getAttributes().size() );
        assertTrue( ArrayUtils.isEquals( ArrayUtils.EMPTY_STRING_ARRAY,
                configs[0].getIndices() ) );

        // test the 'test' config bean
        assertEquals( "test", configs[1].getId() );
        assertNull( configs[1].getSuffix() );
        assertNotNull( configs[1].getAttributes() );
        assertEquals( 0, configs[1].getAttributes().size() );
        assertTrue( ArrayUtils.isEquals( ArrayUtils.EMPTY_STRING_ARRAY,
                configs[1].getIndices() ) );
    }


    /**
     * Tests {@link PartitionConfigBuilder#getContextPartitionConfigs(Hashtable)}
     * using a Hashtable with only partition names but the list property has
     * extra spaces in between and at the ends.
     */
    public void testPartialConfigWithExtraWhitespace() throws NamingException
    {
        Hashtable env = new Hashtable();
        ContextPartitionConfig[] configs = null;

        // setup everything and build config bean
        env.put( EnvKeys.PARTITIONS, "  apache        test " );
        configs = PartitionConfigBuilder.getContextPartitionConfigs( env );

        // start testing return values
        assertNotNull( configs );
        assertEquals( 2, configs.length );

        // test the apache config bean
        assertEquals( "apache", configs[0].getId() );
        assertNull( configs[0].getSuffix() );
        assertNotNull( configs[0].getAttributes() );
        assertEquals( 0, configs[0].getAttributes().size() );
        assertTrue( ArrayUtils.isEquals( ArrayUtils.EMPTY_STRING_ARRAY,
                configs[0].getIndices() ) );

        // test the 'test' config bean
        assertEquals( "test", configs[1].getId() );
        assertNull( configs[1].getSuffix() );
        assertNotNull( configs[1].getAttributes() );
        assertEquals( 0, configs[1].getAttributes().size() );
        assertTrue( ArrayUtils.isEquals( ArrayUtils.EMPTY_STRING_ARRAY,
                configs[1].getIndices() ) );
    }


    /**
     * Tests {@link PartitionConfigBuilder#getContextPartitionConfigs(Hashtable)}
     * using a Hashtable with partitions that have a suffix.  Correctness with
     * whitespace varience is tested.
     */
    public void testSuffixKeys() throws NamingException
    {
        Hashtable env = new Hashtable();
        ContextPartitionConfig[] configs = null;

        // setup everything and build config bean
        env.put( EnvKeys.PARTITIONS, "apache test" );
        env.put( EnvKeys.SUFFIX + "apache", " dc= apache,    dc=org" );
        env.put( EnvKeys.SUFFIX + "test", "   ou   =  test " );
        configs = PartitionConfigBuilder.getContextPartitionConfigs( env );

        // start testing return values
        assertNotNull( configs );
        assertEquals( 2, configs.length );

        // test the apache config bean
        assertEquals( "apache", configs[0].getId() );
        assertEquals( "dc=apache,dc=org", configs[0].getSuffix() );
        assertNotNull( configs[0].getAttributes() );
        assertEquals( 0, configs[0].getAttributes().size() );
        assertTrue( ArrayUtils.isEquals( ArrayUtils.EMPTY_STRING_ARRAY,
                configs[0].getIndices() ) );

        // test the 'test' config bean
        assertEquals( "test", configs[1].getId() );
        assertEquals( "ou=test", configs[1].getSuffix() );
        assertNotNull( configs[1].getAttributes() );
        assertEquals( 0, configs[1].getAttributes().size() );
        assertTrue( ArrayUtils.isEquals( ArrayUtils.EMPTY_STRING_ARRAY,
                configs[1].getIndices() ) );
    }


    /**
     * Tests {@link PartitionConfigBuilder#getContextPartitionConfigs(Hashtable)}
     * using a Hashtable with partitions that have malformed suffix
     * distinguished names.  We test for failure.
     */
    public void testSuffixKeysWithMalformedDN()
    {
        Hashtable env = new Hashtable();

        // setup everything and build config bean
        env.put( EnvKeys.PARTITIONS, "apache test" );
        env.put( EnvKeys.SUFFIX + "apache", " dcapachedcorg" );

        try
        {
            PartitionConfigBuilder.getContextPartitionConfigs( env );
            fail( "should never get here due to an exception" );
        }
        catch( NamingException e )
        {
        }
    }


    /**
     * Tests {@link PartitionConfigBuilder#getContextPartitionConfigs(Hashtable)}
     * using a Hashtable with partitions that have suffixes and indices set.
     */
    public void testIndexKeys() throws NamingException
    {
        Hashtable env = new Hashtable();
        ContextPartitionConfig[] configs = null;

        // setup everything and build config bean
        env.put( EnvKeys.PARTITIONS, "apache test" );
        env.put( EnvKeys.SUFFIX  + "apache", "dc=apache,dc=org" );
        env.put( EnvKeys.SUFFIX  + "test", "ou=test" );
        env.put( EnvKeys.INDICES + "apache", "ou objectClass      uid" );
        env.put( EnvKeys.INDICES + "test", "ou objectClass  " );
        configs = PartitionConfigBuilder.getContextPartitionConfigs( env );

        // start testing return values
        assertNotNull( configs );
        assertEquals( 2, configs.length );

        // test the apache config bean
        assertEquals( "apache", configs[0].getId() );
        assertEquals( "dc=apache,dc=org", configs[0].getSuffix() );
        assertNotNull( configs[0].getAttributes() );
        assertEquals( 0, configs[0].getAttributes().size() );
        assertEquals( 3, configs[0].getIndices().length );
        assertTrue( ArrayUtils.isEquals( new String[]{ "ou", "objectClass", "uid" },
                configs[0].getIndices() ) );

        // test the 'test' config bean
        assertEquals( "test", configs[1].getId() );
        assertEquals( "ou=test", configs[1].getSuffix() );
        assertNotNull( configs[1].getAttributes() );
        assertEquals( 0, configs[1].getAttributes().size() );
        assertEquals( 2, configs[1].getIndices().length );
        assertTrue( ArrayUtils.isEquals( new String[]{ "ou", "objectClass" },
                configs[1].getIndices() ) );
    }



    /**
     * Tests {@link PartitionConfigBuilder#getContextPartitionConfigs(Hashtable)}
     * using a Hashtable with partitions that have suffixes, indices and
     * attributes set.
     */
    public void testAttributeKeys() throws NamingException
    {
        Hashtable env = new Hashtable();
        ContextPartitionConfig[] configs = null;

        // setup everything and build config bean
        env.put( EnvKeys.PARTITIONS, "apache test" );
        env.put( EnvKeys.SUFFIX  + "apache", "dc=apache,dc=org" );
        env.put( EnvKeys.SUFFIX  + "test", "ou=test" );
        env.put( EnvKeys.INDICES + "apache", "ou objectClass      uid" );
        env.put( EnvKeys.INDICES + "test", "ou objectClass  " );
        env.put( EnvKeys.ATTRIBUTES + "apache" + ".dc", "apache" );
        env.put( EnvKeys.ATTRIBUTES + "apache" + ".objectClass", "top domain extensibleObject" );
        env.put( EnvKeys.ATTRIBUTES + "test" + ".ou", "test" );
        env.put( EnvKeys.ATTRIBUTES + "test" + ".objectClass", "top extensibleObject organizationalUnit" );
        configs = PartitionConfigBuilder.getContextPartitionConfigs( env );

        // start testing return values
        assertNotNull( configs );
        assertEquals( 2, configs.length );

        // test the apache config bean
        assertEquals( "apache", configs[0].getId() );
        assertEquals( "dc=apache,dc=org", configs[0].getSuffix() );
        assertNotNull( configs[0].getAttributes() );
        assertEquals( 2, configs[0].getAttributes().size() );
        assertEquals( 3, configs[0].getIndices().length );
        assertTrue( ArrayUtils.isEquals( new String[]{ "ou", "objectClass", "uid" },
                configs[0].getIndices() ) );
        LockableAttributesImpl attrs = new LockableAttributesImpl();
        LockableAttributeImpl attr = new LockableAttributeImpl( "dc" );
        attrs.put( attr );
        attr.add( "apache" );
        attr = new LockableAttributeImpl( "objectClass" );
        attrs.put( attr );
        attr.add( "top" );
        attr.add( "domain" );
        attr.add( "extensibleObject" );
        assertTrue( attrs.equals( configs[0].getAttributes() ) );

        // test the 'test' config bean
        assertEquals( "test", configs[1].getId() );
        assertEquals( "ou=test", configs[1].getSuffix() );
        assertNotNull( configs[1].getAttributes() );
        assertEquals( 2, configs[1].getAttributes().size() );
        assertEquals( 2, configs[1].getIndices().length );
        assertTrue( ArrayUtils.isEquals( new String[]{ "ou", "objectClass" },
                configs[1].getIndices() ) );
        attrs = new LockableAttributesImpl();
        attr = new LockableAttributeImpl( "ou" );
        attrs.put( attr );
        attr.add( "test" );
        attr = new LockableAttributeImpl( "objectClass" );
        attrs.put( attr );
        attr.add( "top" );
        attr.add( "extensibleObject" );
        attr.add( "organizationalUnit" );
        assertTrue( attrs.equals( configs[1].getAttributes() ) );
    }


    /**
     * Tests {@link PartitionConfigBuilder#getContextPartitionConfigs(Hashtable)}
     * using a Hashtable with partitions that have suffixes, indices and
     * attributes set however values have some space variance.
     */
    public void testAttributeValuesWithWhitespace() throws NamingException
    {
        Hashtable env = new Hashtable();
        ContextPartitionConfig[] configs = null;

        // setup everything and build config bean
        env.put( EnvKeys.PARTITIONS, "apache test" );
        env.put( EnvKeys.SUFFIX  + "apache", "dc=apache,dc=org" );
        env.put( EnvKeys.SUFFIX  + "test", "ou=test" );
        env.put( EnvKeys.INDICES + "apache", "ou objectClass      uid" );
        env.put( EnvKeys.INDICES + "test", "ou objectClass  " );
        env.put( EnvKeys.ATTRIBUTES + "apache" + ".dc", "apache" );
        env.put( EnvKeys.ATTRIBUTES + "apache" + ".objectClass",
                "     top    domain    extensibleObject " );
        env.put( EnvKeys.ATTRIBUTES + "test" + ".ou", "test" );
        env.put( EnvKeys.ATTRIBUTES + "test" + ".objectClass",
                "top extensibleObject    organizationalUnit" );
        configs = PartitionConfigBuilder.getContextPartitionConfigs( env );

        // start testing return values
        assertNotNull( configs );
        assertEquals( 2, configs.length );

        // test the apache config bean
        assertEquals( "apache", configs[0].getId() );
        assertEquals( "dc=apache,dc=org", configs[0].getSuffix() );
        assertNotNull( configs[0].getAttributes() );
        assertEquals( 2, configs[0].getAttributes().size() );
        assertEquals( 3, configs[0].getIndices().length );
        assertTrue( ArrayUtils.isEquals( new String[]{ "ou", "objectClass", "uid" },
                configs[0].getIndices() ) );
        LockableAttributesImpl attrs = new LockableAttributesImpl();
        LockableAttributeImpl attr = new LockableAttributeImpl( "dc" );
        attrs.put( attr );
        attr.add( "apache" );
        attr = new LockableAttributeImpl( "objectClass" );
        attrs.put( attr );
        attr.add( "top" );
        attr.add( "domain" );
        attr.add( "extensibleObject" );
        assertTrue( attrs.equals( configs[0].getAttributes() ) );

        // test the 'test' config bean
        assertEquals( "test", configs[1].getId() );
        assertEquals( "ou=test", configs[1].getSuffix() );
        assertNotNull( configs[1].getAttributes() );
        assertEquals( 2, configs[1].getAttributes().size() );
        assertEquals( 2, configs[1].getIndices().length );
        assertTrue( ArrayUtils.isEquals( new String[]{ "ou", "objectClass" },
                configs[1].getIndices() ) );
        attrs = new LockableAttributesImpl();
        attr = new LockableAttributeImpl( "ou" );
        attrs.put( attr );
        attr.add( "test" );
        attr = new LockableAttributeImpl( "objectClass" );
        attrs.put( attr );
        attr.add( "top" );
        attr.add( "extensibleObject" );
        attr.add( "organizationalUnit" );
        assertTrue( attrs.equals( configs[1].getAttributes() ) );
    }
}
