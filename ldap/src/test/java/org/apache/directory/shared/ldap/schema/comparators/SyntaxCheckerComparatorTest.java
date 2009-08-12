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
package org.apache.directory.shared.ldap.schema.comparators;

import org.junit.Before;
import org.junit.Test;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotSame;


/**
 * Test the SyntaxChecker comparator
 * 
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 * @version $Rev$
 */
public class SyntaxCheckerComparatorTest
{
    private SyntaxCheckerComparator comparator;
    
    @Before
    public void init()
    {
        comparator = new SyntaxCheckerComparator();
    }
    
    
    @Test
    public void testNullSyntaxCheckers()
    {
        assertEquals( 0, comparator.compare( null, null ) );
        
        String c2 = "( 1.3.6.1.4.1.18060.0.4.1.0.10000 DESC 'bogus desc' FQCN org.apache.directory.shared.ldap.schema.syntax.AcceptAllSyntaxChecker )";
        
        assertEquals( -1, comparator.compare( null, c2 ) );

        assertEquals( -1, comparator.compare( c2, null ) );
    }


    @Test
    public void testEqualsSyntaxCheckers()
    {
        String c1 = "( 1.3.6.1.4.1.18060.0.4.1.0.10000 DESC 'bogus desc' FQCN org.apache.directory.shared.ldap.schema.syntax.AcceptAllSyntaxChecker )";
        String c2 = "( 1.3.6.1.4.1.18060.0.4.1.0.10000 desc 'bogus desc' FQCN org.apache.directory.shared.ldap.schema.syntax.AcceptAllSyntaxChecker )";
        assertEquals( 0, comparator.compare( c1, c2 ) );

        String c3 = "( 1.3.6.1.4.1.18060.0.4.1.0.10000 DESC 'bogus desc' FQCN org.apache.directory.shared.ldap.schema.syntax.AcceptAllSyntaxChecker X-SCHEMA 'system' )";
        String c4 = "( 1.3.6.1.4.1.18060.0.4.1.0.10000 DESC 'bogus desc' FQCN org.apache.directory.shared.ldap.schema.syntax.AcceptAllSyntaxChecker )";
        assertEquals( 0, comparator.compare( c3, c4 ) );
    }


    @Test
    public void testDifferentSyntaxCheckers()
    {
        String c1 = "( 1.3.6.1.4.1.18060.0.4.1.0.10000 DESC 'bogus desc' FQCN org.apache.directory.shared.ldap.schema.syntax.AcceptAllSyntaxChecker )";
        String c2 = "( 1.3.6.1.4.1.18060.0.4.1.0.10001 DESC 'bogus desc' FQCN org.apache.directory.shared.ldap.schema.syntax.AcceptAllSyntaxChecker )";
        assertNotSame( 0, comparator.compare( c1, c2 ) );

        String c3 = "( 1.3.6.1.4.1.18060.0.4.1.0.10000 DESC 'bogus desc' FQCN org.apache.directory.shared.ldap.schema.syntax.AcceptAllSyntaxChecker X-SCHEMA 'system' )";
        String c4 = "( 1.3.6.1.4.1.18060.0.4.1.0.10001 DESC 'bogus desc' FQCN org.apache.directory.shared.ldap.schema.syntax.AcceptAllSyntaxChecker )";
        assertNotSame( 0, comparator.compare( c3, c4 ) );
    }


    @Test
    public void testInvalidSyntaxCheckers()
    {
        String c1 = "( 1.3.6.1.4.1.18060.0.4.1.0.10000 DESC 'bogus desc' FQCN org.apache.directory.shared.ldap.schema.syntax.AcceptAllSyntaxChecker )";
        String c2 = "( 1.1 Bad data )";
        assertNotSame( 0, comparator.compare( c1, c2 ) );

        String c3 = "( 1.1 bad data )";
        String c4 = "( 1.3.6.1.4.1.18060.0.4.1.0.10000 DESC 'bogus desc' FQCN org.apache.directory.shared.ldap.schema.syntax.AcceptAllSyntaxChecker )";
        assertNotSame( 0, comparator.compare( c3, c4 ) );
    }
}
