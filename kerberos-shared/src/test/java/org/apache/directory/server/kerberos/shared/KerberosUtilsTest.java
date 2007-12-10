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
package org.apache.directory.server.kerberos.shared;

import java.util.List;

import javax.security.auth.kerberos.KerberosPrincipal;

import org.apache.directory.server.kerberos.shared.KerberosUtils;

import junit.framework.TestCase;

/**
 * Test the KerberosUtils class
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 */
public class KerberosUtilsTest extends TestCase
{
    public void setUp()
    {
        // First setup a default realm
        System.setProperty( "java.security.krb5.realm", "APACHE.ORG" );
        System.setProperty( "java.security.krb5.kdc", "localhost" );
    }

    
    public void testKerberosNameSimple() throws Exception
    {
        KerberosPrincipal kp = new KerberosPrincipal( "abc" );
        List<String> names = KerberosUtils.getNames( kp );
     
        assertEquals( 1, names.size() );
        assertEquals( "abc", names.get( 0 ) );
    }

    
    /**
    public void testKerberosNameEscaped() throws Exception
    {
        KerberosPrincipal kp = new KerberosPrincipal( "abc\\//d\\@f/g\\\\hi" );
        List<String> names = KerberosUtils.getNames( kp );
     
        assertEquals( 3, names.size() );
        assertEquals( "abc\\/", names.get( 0 ) );
        assertEquals( "d\\@g", names.get( 1 ) );
        assertEquals( "g\\\\hi", names.get( 2 ) );
    }
    */


    public void testKerberosNameSimpleWithRealm() throws Exception
    {
        KerberosPrincipal kp = new KerberosPrincipal( "abc@APACHE.ORG" );
        List<String> names = KerberosUtils.getNames( kp );
     
        assertEquals( 1, names.size() );
        assertEquals( "abc", names.get( 0 ) );
    }
    
    public void testKerberosNameThree() throws Exception
    {
        KerberosPrincipal kp = new KerberosPrincipal( "abc/def/ghi" );
        List<String> names = KerberosUtils.getNames( kp );
     
        assertEquals( 3, names.size() );
        assertEquals( "abc", names.get( 0 ) );
        assertEquals( "def", names.get( 1 ) );
        assertEquals( "ghi", names.get( 2 ) );
    }

    public void testKerberosNameThreeWithRealm() throws Exception
    {
        KerberosPrincipal kp = new KerberosPrincipal( "abc/def/ghi@APACHE.ORG" );
        List<String> names = KerberosUtils.getNames( kp );
     
        assertEquals( 3, names.size() );
        assertEquals( "abc", names.get( 0 ) );
        assertEquals( "def", names.get( 1 ) );
        assertEquals( "ghi", names.get( 2 ) );
    }


    
    /*
    public void testKerberosEndingSlash()
    {
        try
        {
            KerberosPrincipal kp = new KerberosPrincipal( "abc/def/ghi/" );
            KerberosUtils.getNames( kp );
            
            // Should not reach this point
            fail();
        }
        catch ( ParseException pe )
        {
            assertTrue( true );
        }
    }
    */
    
    
    /*
    public void testKerberosEndingSlashWithRealm()
    {
        try
        {
            KerberosPrincipal kp = new KerberosPrincipal( "abc/def/ghi/@APACHE.ORG" );
            KerberosUtils.getNames( kp );
            
            // Should not reach this point
            fail();
        }
        catch ( ParseException pe )
        {
            assertTrue( true );
        }
    }
    */
}
