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

import org.apache.directory.shared.ldap.schema.syntaxes.CsnSyntaxChecker;

import org.junit.Test;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * Test cases for CsnSyntaxChecker.
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 * @version $Rev$
 */
public class CsnSyntaxCheckerTest
{
    CsnSyntaxChecker checker = new CsnSyntaxChecker();


    @Test
    public void testNullCsn()
    {
        assertFalse( checker.isValidSyntax( null ) );
    }


    @Test
    public void testEmptyCsn()
    {
        assertFalse( checker.isValidSyntax( "" ) );
    }
    
    
    /**
     * Test that a replicaId not being an integer between 000 and fff
     * is seen as invalid
     */
    @Test
    public void testBadReplicaId()
    {
        assertFalse( checker.isValidSyntax( "20090602120000.100000Z#000000##000000" ) );
        assertFalse( checker.isValidSyntax( "20090602120000.100000Z#000000#00P#000000" ) );
        assertFalse( checker.isValidSyntax( "20090602120000.100000Z#000000#-1#000000" ) );
        assertFalse( checker.isValidSyntax( "20090602120000.100000Z#000000#-01#000000" ) );
        assertFalse( checker.isValidSyntax( "20090602120000.100000Z#000000#0#000000" ) );
        assertFalse( checker.isValidSyntax( "20090602120000.100000Z#000000#0 0#000000" ) );
        assertFalse( checker.isValidSyntax( "20090602120000.100000Z#000000#   #000000" ) );
    }
    
    
    /**
     * Test that a replicaId is a valid number between 000 and fff
     */
    @Test
    public void testValidReplicaId()
    {
        assertTrue( checker.isValidSyntax( "20090602120000.100000Z#000000#000#000000" ) );
        assertTrue( checker.isValidSyntax( "20090602120000.100000Z#000000#00f#000000" ) );
        assertTrue( checker.isValidSyntax( "20090602120000.100000Z#000000#fff#000000" ) );
        assertTrue( checker.isValidSyntax( "20090602120000.100000Z#000000#123#000000" ) );
    }
}
