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


import org.apache.directory.shared.ldap.schema.syntaxes.DITContentRuleDescriptionSyntaxChecker;

import org.junit.Test;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;


/**
 * Test cases for DITContentRuleDescriptionSyntaxChecker.
 * 
 * There are also many test cases in SchemaParserDITContentRuleDescriptionTest.
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 * @version $Rev$
 */
public class DITContentRuleDescriptionSyntaxCheckerTest
{
    private DITContentRuleDescriptionSyntaxChecker checker = new DITContentRuleDescriptionSyntaxChecker();


    @Test
    public void testValid()
    {
        assertTrue( checker.isValidSyntax( "( 2.5.6.4 )" ) );
        assertTrue( checker.isValidSyntax( "( 2.5.6.4 NAME 'organization' )" ) );
        assertTrue( checker.isValidSyntax( "( 2.5.6.4 NAME 'organization' DESC 'content rule for organization' )" ) );
        assertTrue( checker
            .isValidSyntax( "( 2.5.6.4 NAME 'organization' DESC 'content rule for organization' OBSOLETE )" ) );
        assertTrue( checker
            .isValidSyntax( "( 2.5.6.4 NAME 'organization' DESC 'content rule for organization' OBSOLETE AUX ( pilotOrganization $  2.5.6.5 ) )" ) );
        assertTrue( checker
            .isValidSyntax( "( 2.5.6.4 NAME 'organization' DESC 'content rule for organization' OBSOLETE AUX ( pilotOrganization $  2.5.6.5 ) MUST ( objectClass $ o ) )" ) );
        assertTrue( checker
            .isValidSyntax( "( 2.5.6.4 NAME 'organization' DESC 'content rule for organization' OBSOLETE AUX ( pilotOrganization $  2.5.6.5 ) MUST ( objectClass $ o ) MAY ( l $ st )  )" ) );
        assertTrue( checker
            .isValidSyntax( "( 2.5.6.4 NAME 'organization' DESC 'content rule for organization' OBSOLETE AUX ( pilotOrganization $  2.5.6.5 ) MUST ( objectClass $ o ) MAY ( l $ st ) NOT ( 1.2.3.4.5.6.7.8.9.0 $ ou ) )" ) );

        assertTrue( checker.isValidSyntax( "(2.5.6.4)" ) );
        assertTrue( checker.isValidSyntax( "(2.5.6.4 NAME organization)" ) );
        assertTrue( checker
            .isValidSyntax( "(   2.5.6.4     NAME   'organization'   DESC   'content rule for organization' OBSOLETE AUX ( pilotOrganization $  2.5.6.5 ) MUST ( objectClass $ o )     MAY    (    l   $   st   ) NOT (1.2.3.4.5.6.7.8.9.0 $ ou))" ) );

        // lowercase NAME, DESC, AUX
        assertTrue( checker
            .isValidSyntax( "( 2.5.6.4 name 'organization' desc 'content rule for organization' aux ( pilotOrganization $  2.5.6.5 ) )" ) );
    }


    @Test
    public void testInvalid()
    {
        // null 
        assertFalse( checker.isValidSyntax( null ) );

        // empty 
        assertFalse( checker.isValidSyntax( "" ) );

        // missing/invalid OID
        assertFalse( checker.isValidSyntax( "()" ) );
        assertFalse( checker.isValidSyntax( "(  )" ) );
        assertFalse( checker.isValidSyntax( "( . )" ) );
        assertFalse( checker.isValidSyntax( "( 1 )" ) );
        assertFalse( checker.isValidSyntax( "( 1. )" ) );
        assertFalse( checker.isValidSyntax( "( 1.2. )" ) );
        assertFalse( checker.isValidSyntax( "( 1.A )" ) );
        assertFalse( checker.isValidSyntax( "( A.B )" ) );

        // missing right parenthesis
        assertFalse( checker.isValidSyntax( "( 2.5.6.4 NAME 'organization'" ) );
    }

}
