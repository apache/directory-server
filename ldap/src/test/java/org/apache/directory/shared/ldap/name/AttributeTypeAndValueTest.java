/*
 *   Copyright 2005 The Apache Software Foundation
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
package org.apache.directory.shared.ldap.name;


import javax.naming.InvalidNameException;

import org.apache.directory.shared.ldap.name.AttributeTypeAndValue;

import junit.framework.Assert;
import junit.framework.TestCase;


/**
 * Test the class AttributeTypeAndValue
 * 
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 */
public class AttributeTypeAndValueTest extends TestCase
{
    // ~ Methods
    // ------------------------------------------------------------------------------------

    /**
     * Setup the test
     */
    protected void setUp()
    {
    }


    /**
     * Test a null AttributeTypeAndValue
     */
    public void testAttributeTypeAndValueNull() throws InvalidNameException
    {
        Assert.assertEquals( "", new AttributeTypeAndValue().toString() );
    }


    /**
     * test an empty AttributeTypeAndValue
     */
    public void testLdapRDNEmpty() throws InvalidNameException
    {
        try
        {
            new AttributeTypeAndValue( "", "" );
            Assert.fail( "Should not occurs ... " );
        }
        catch ( InvalidNameException ine )
        {
            Assert.assertTrue( true );
        }
    }


    /**
     * test a simple AttributeTypeAndValue : a = b
     */
    public void testLdapRDNSimple() throws InvalidNameException
    {
        Assert.assertEquals( "a=b", new AttributeTypeAndValue( "a", "b" ).toString() );
    }


    /**
     * Compares two equals atavs
     */
    public void testCompareToEquals() throws InvalidNameException
    {
        AttributeTypeAndValue atav1 = new AttributeTypeAndValue( "a", "b" );
        AttributeTypeAndValue atav2 = new AttributeTypeAndValue( "a", "b" );

        Assert.assertEquals( 0, atav1.compareTo( atav2 ) );
    }


    /**
     * Compares two equals atavs but with a type in different case
     */
    public void testCompareToEqualsCase() throws InvalidNameException
    {
        AttributeTypeAndValue atav1 = new AttributeTypeAndValue( "a", "b" );
        AttributeTypeAndValue atav2 = new AttributeTypeAndValue( "A", "b" );

        Assert.assertEquals( 0, atav1.compareTo( atav2 ) );
    }


    /**
     * Compare two atavs : the first one is superior because its type is
     * superior
     */
    public void testCompareAtav1TypeSuperior() throws InvalidNameException
    {
        AttributeTypeAndValue atav1 = new AttributeTypeAndValue( "b", "b" );
        AttributeTypeAndValue atav2 = new AttributeTypeAndValue( "a", "b" );

        Assert.assertEquals( 1, atav1.compareTo( atav2 ) );
    }


    /**
     * Compare two atavs : the second one is superior because its type is
     * superior
     */
    public void testCompareAtav2TypeSuperior() throws InvalidNameException
    {
        AttributeTypeAndValue atav1 = new AttributeTypeAndValue( "a", "b" );
        AttributeTypeAndValue atav2 = new AttributeTypeAndValue( "b", "b" );

        Assert.assertEquals( -1, atav1.compareTo( atav2 ) );
    }


    /**
     * Compare two atavs : the first one is superior because its type is
     * superior
     */
    public void testCompareAtav1ValueSuperior() throws InvalidNameException
    {
        AttributeTypeAndValue atav1 = new AttributeTypeAndValue( "a", "b" );
        AttributeTypeAndValue atav2 = new AttributeTypeAndValue( "a", "a" );

        Assert.assertEquals( 1, atav1.compareTo( atav2 ) );
    }


    /**
     * Compare two atavs : the second one is superior because its type is
     * superior
     */
    public void testCompareAtav2ValueSuperior() throws InvalidNameException
    {
        AttributeTypeAndValue atav1 = new AttributeTypeAndValue( "a", "a" );
        AttributeTypeAndValue atav2 = new AttributeTypeAndValue( "a", "b" );

        Assert.assertEquals( -1, atav1.compareTo( atav2 ) );
    }


    public void testNormalize() throws InvalidNameException
    {
        AttributeTypeAndValue atav = new AttributeTypeAndValue( " A ", "a" );

        Assert.assertEquals( "a=a", atav.normalize() );
    }
}
