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

import junit.framework.TestCase;

/**
 *
 * Test the Telephone Number normalizer class
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 */
public class TelephoneNumberNormalizerTest extends TestCase
{
   public void testTelephoneNumberNormalizerNull() throws NamingException
   {
       Normalizer normalizer = new TelephoneNumberNormalizer();
       assertEquals( "", normalizer.normalize( (String)null ) );
   }

   public void testTelephoneNumberNormalizerEmpty() throws NamingException
   {
       Normalizer normalizer = new TelephoneNumberNormalizer();
       assertEquals( "", normalizer.normalize( "" ) );
   }

   public void testTelephoneNumberNormalizerOneSpace() throws NamingException
   {
       Normalizer normalizer = new TelephoneNumberNormalizer();
       assertEquals( "", normalizer.normalize( " " ) );
   }

   public void testTelephoneNumberNormalizerTwoSpaces() throws NamingException
   {
       Normalizer normalizer = new TelephoneNumberNormalizer();
       assertEquals( "", normalizer.normalize( "  " ) );
   }

   public void testTelephoneNumberNormalizerNSpaces() throws NamingException
   {
       Normalizer normalizer = new TelephoneNumberNormalizer();
       assertEquals( "", normalizer.normalize( "      " ) );
   }

   public void testTelephoneNumberNormalizerOneHyphen() throws NamingException
   {
       Normalizer normalizer = new TelephoneNumberNormalizer();
       assertEquals( "", normalizer.normalize( "-" ) );
   }

   public void testTelephoneNumberNormalizerTwoHyphen() throws NamingException
   {
       Normalizer normalizer = new TelephoneNumberNormalizer();
       assertEquals( "", normalizer.normalize( "--" ) );
   }

   public void testTelephoneNumberNormalizerHyphensSpaces() throws NamingException
   {
       Normalizer normalizer = new TelephoneNumberNormalizer();
       assertEquals( "", normalizer.normalize( " -- - -- " ) );
   }

   public void testInsignifiantSpacesStringOneChar() throws NamingException
   {
       Normalizer normalizer = new TelephoneNumberNormalizer();
       assertEquals( "1", normalizer.normalize( "1" ) );
   }

   public void testInsignifiantSpacesStringTwoChars() throws NamingException
   {
       Normalizer normalizer = new TelephoneNumberNormalizer();
       assertEquals( "11", normalizer.normalize( "11" ) );
   }

   public void testInsignifiantSpacesStringNChars() throws NamingException
   {
       Normalizer normalizer = new TelephoneNumberNormalizer();
       assertEquals( "123456", normalizer.normalize( "123456" ) );
   }

   public void testInsignifiantTelephoneNumberCharsSpaces() throws NamingException
   {
       Normalizer normalizer = new TelephoneNumberNormalizer();
       assertEquals( "1", normalizer.normalize( " 1" ) );
       assertEquals( "1", normalizer.normalize( "1 " ) );
       assertEquals( "1", normalizer.normalize( " 1 " ) );
       assertEquals( "11", normalizer.normalize( "1 1" ) );
       assertEquals( "11", normalizer.normalize( " 1 1" ) );
       assertEquals( "11", normalizer.normalize( "1 1 " ) );
       assertEquals( "11", normalizer.normalize( "1  1" ) );
       assertEquals( "11", normalizer.normalize( " 1   1 " ) );
       assertEquals( "123456789", normalizer.normalize( "  123   456   789  " ) );
       assertEquals( "1", normalizer.normalize( "-1" ) );
       assertEquals( "1", normalizer.normalize( "1-" ) );
       assertEquals( "1", normalizer.normalize( "-1-" ) );
       assertEquals( "11", normalizer.normalize( "1-1" ) );
       assertEquals( "11", normalizer.normalize( "-1-1" ) );
       assertEquals( "11", normalizer.normalize( "1-1-" ) );
       assertEquals( "11", normalizer.normalize( "1--1" ) );
       assertEquals( "11", normalizer.normalize( "-1---1-" ) );
       assertEquals( "1(2)+3456789", normalizer.normalize( "---1(2)+3   456-  789 --" ) );
   }
}