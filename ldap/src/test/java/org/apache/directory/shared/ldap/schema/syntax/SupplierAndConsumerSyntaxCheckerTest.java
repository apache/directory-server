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
package org.apache.directory.shared.ldap.schema.syntax;

import org.apache.directory.shared.ldap.schema.syntaxes.SupplierAndConsumerSyntaxChecker;

import org.junit.Test;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.assertEquals;

/**
 * Test cases for SupplierAndConsumerSyntaxChecker.
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 * @version $Rev$
 */
public class SupplierAndConsumerSyntaxCheckerTest
{
    SupplierAndConsumerSyntaxChecker checker = new SupplierAndConsumerSyntaxChecker();


    @Test
    public void testNullString()
    {
        assertTrue( checker.isValidSyntax( null ) );
    }


    @Test
    public void testEmptyString()
    {
        assertTrue( checker.isValidSyntax( "" ) );
    }

    @Test
    public void testOid()
    {
        assertEquals( "1.3.6.1.4.1.1466.115.121.1.48", checker.getSyntaxOid() );
    }

    @Test
    public void testCorrectCase()
    {
        assertTrue( checker.isValidSyntax( "FALSE" ) );
        assertTrue( checker.isValidSyntax( new byte[]{0x01, (byte)0xFF} ) );
    }
}
