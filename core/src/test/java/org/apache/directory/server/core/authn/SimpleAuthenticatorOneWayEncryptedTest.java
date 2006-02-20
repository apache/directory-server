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

package org.apache.directory.server.core.authn;


import java.security.NoSuchAlgorithmException;

import org.apache.directory.server.core.authn.SimpleAuthenticator;

import junit.framework.TestCase;


/**
 * Test case for helper methods within SimpleAuthenticator.
 * 
 * @author Apache Directory Project (dev@directory.apache.org)
 */
public class SimpleAuthenticatorOneWayEncryptedTest extends TestCase
{
    private SimpleAuthenticator auth = null;


    protected void setUp() throws Exception
    {
        super.setUp();
        this.auth = new SimpleAuthenticator();
    }


    public void testGetAlgorithmForHashedPassword()
    {
        String digestetValue = "{SHA}LhkDrSoM6qr0fW6hzlfOJQW61tc=";
        assertEquals( "SHA", auth.getAlgorithmForHashedPassword( digestetValue ) );
        assertEquals( "SHA", auth.getAlgorithmForHashedPassword( digestetValue.getBytes() ) );

        String noAlgorithm = "Secret1!";
        assertEquals( null, auth.getAlgorithmForHashedPassword( noAlgorithm ) );
        assertEquals( null, auth.getAlgorithmForHashedPassword( noAlgorithm.getBytes() ) );

        String unknownAlgorithm = "{XYZ}LhkDrSoM6qr0fW6hzlfOJQW61tc=";
        assertEquals( null, auth.getAlgorithmForHashedPassword( unknownAlgorithm ) );
        assertEquals( null, auth.getAlgorithmForHashedPassword( unknownAlgorithm.getBytes() ) );
    }


    public void testCreateDigestedPassword() throws NoSuchAlgorithmException
    {
        String pwd = "Secret1!";
        String expected = "{SHA}znbJr3+tymFoQD4+Njh4ITtI7Cc=";
        String digested = auth.createDigestedPassword( "SHA", pwd );

        assertEquals( expected, digested );
    }
}