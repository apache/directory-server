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
package org.apache.directory.shared.ldap.schema;


import javax.naming.NamingException;

import org.apache.directory.shared.ldap.schema.normalizers.NumericNormalizer;
import org.junit.Test;
import static org.junit.Assert.assertEquals;


/**
 * Test the numeric normalizer class
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 */
public class NumericNormalizerTest
{
    @Test
    public void testNumericNormalizerNull() throws NamingException
    {
        Normalizer normalizer = new NumericNormalizer();
        assertEquals( "", normalizer.normalize( ( String ) null ) );
    }


    @Test
    public void testNumericNormalizerEmpty() throws NamingException
    {
        Normalizer normalizer = new NumericNormalizer();
        assertEquals( "", normalizer.normalize( "" ) );
    }


    @Test
    public void testNumericNormalizerOneSpace() throws NamingException
    {
        Normalizer normalizer = new NumericNormalizer();
        assertEquals( "", normalizer.normalize( " " ) );
    }


    @Test
    public void testNumericNormalizerTwoSpaces() throws NamingException
    {
        Normalizer normalizer = new NumericNormalizer();
        assertEquals( "", normalizer.normalize( "  " ) );
    }


    @Test
    public void testNumericNormalizerNSpaces() throws NamingException
    {
        Normalizer normalizer = new NumericNormalizer();
        assertEquals( "", normalizer.normalize( "      " ) );
    }


    @Test
    public void testInsignifiantSpacesStringOneChar() throws NamingException
    {
        Normalizer normalizer = new NumericNormalizer();
        assertEquals( "1", normalizer.normalize( "1" ) );
    }


    @Test
    public void testInsignifiantSpacesStringTwoChars() throws NamingException
    {
        Normalizer normalizer = new NumericNormalizer();
        assertEquals( "11", normalizer.normalize( "11" ) );
    }


    @Test
    public void testInsignifiantSpacesStringNChars() throws NamingException
    {
        Normalizer normalizer = new NumericNormalizer();
        assertEquals( "123456", normalizer.normalize( "123456" ) );
    }


    @Test
    public void testInsignifiantNumericCharsSpaces() throws NamingException
    {
        Normalizer normalizer = new NumericNormalizer();
        assertEquals( "1", normalizer.normalize( " 1" ) );
        assertEquals( "1", normalizer.normalize( "1 " ) );
        assertEquals( "1", normalizer.normalize( " 1 " ) );
        assertEquals( "11", normalizer.normalize( "1 1" ) );
        assertEquals( "11", normalizer.normalize( " 1 1" ) );
        assertEquals( "11", normalizer.normalize( "1 1 " ) );
        assertEquals( "11", normalizer.normalize( "1  1" ) );
        assertEquals( "11", normalizer.normalize( " 1   1 " ) );
        assertEquals( "123456789", normalizer.normalize( "  123   456   789  " ) );
    }
}