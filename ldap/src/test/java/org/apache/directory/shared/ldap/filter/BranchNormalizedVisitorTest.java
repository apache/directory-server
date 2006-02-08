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
package org.apache.directory.shared.ldap.filter;


import org.apache.directory.shared.ldap.filter.BranchNormalizedVisitor;
import org.apache.directory.shared.ldap.filter.ExprNode;
import org.apache.directory.shared.ldap.filter.FilterParserImpl;

import junit.framework.TestCase;


/**
 * Tests the BranchNormalizedVisitor.
 * 
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 * @version $Rev$
 */
public class BranchNormalizedVisitorTest extends TestCase
{
    public void testBranchNormalizedVisitor0() throws Exception
    {
        FilterParserImpl parser = new FilterParserImpl();

        String filter = "( ou = Human Resources )";

        ExprNode ori = parser.parse( filter );

        ExprNode altered = parser.parse( filter );

        BranchNormalizedVisitor visitor = new BranchNormalizedVisitor();

        visitor.visit( altered );

        StringBuffer oriBuf = new StringBuffer();

        ori.printToBuffer( oriBuf );

        StringBuffer alteredBuf = new StringBuffer();

        altered.printToBuffer( alteredBuf );

        assertEquals( oriBuf.toString(), alteredBuf.toString() );
    }


    public void testBranchNormalizedVisitor1() throws Exception
    {
        FilterParserImpl parser = new FilterParserImpl();

        String filter = "( & ( ou = Human Resources ) ( uid = akarasulu ) )";

        ExprNode ori = parser.parse( filter );

        ExprNode altered = parser.parse( filter );

        BranchNormalizedVisitor visitor = new BranchNormalizedVisitor();

        visitor.visit( altered );

        StringBuffer oriBuf = new StringBuffer();

        ori.printToBuffer( oriBuf );

        StringBuffer alteredBuf = new StringBuffer();

        altered.printToBuffer( alteredBuf );

        assertEquals( oriBuf.toString(), alteredBuf.toString() );
    }


    public void testBranchNormalizedVisitor2() throws Exception
    {
        FilterParserImpl parser = new FilterParserImpl();

        String filter = "( & ( uid = akarasulu ) ( ou = Human Resources ) ";

        filter += "(| ( uid = akarasulu ) ( ou = Human Resources ) ) ) ";

        ExprNode ori = parser.parse( filter );

        ExprNode altered = parser.parse( filter );

        BranchNormalizedVisitor visitor = new BranchNormalizedVisitor();

        visitor.visit( altered );

        StringBuffer oriBuf = new StringBuffer();

        ori.printToBuffer( oriBuf );

        StringBuffer alteredBuf = new StringBuffer();

        altered.printToBuffer( alteredBuf );

        assertFalse( oriBuf.toString().equals( alteredBuf.toString() ) );
    }


    public void testBranchNormalizedVisitor3() throws Exception
    {
        FilterParserImpl parser = new FilterParserImpl();

        String filter = "( & ( ou = Human Resources ) ( uid = akarasulu ) ";

        filter += "(| ( ou = Human Resources ) ( uid = akarasulu ) ) ) ";

        ExprNode ori = parser.parse( filter );

        ExprNode altered = parser.parse( filter );

        BranchNormalizedVisitor visitor = new BranchNormalizedVisitor();

        visitor.visit( altered );

        StringBuffer oriBuf = new StringBuffer();

        ori.printToBuffer( oriBuf );

        StringBuffer alteredBuf = new StringBuffer();

        altered.printToBuffer( alteredBuf );

        assertTrue( oriBuf.toString().equals( alteredBuf.toString() ) );
    }


    public void testBranchNormalizedComplex() throws Exception
    {
        String filter1 = "( & ( a = A ) ( | ( b = B ) ( c = C ) ) )";

        String filter2 = "( & ( a = A ) ( | ( c = C ) ( b = B ) ) )";

        String normalizedFilter1 = BranchNormalizedVisitor.getNormalizedFilter( filter1 );

        String normalizedFilter2 = BranchNormalizedVisitor.getNormalizedFilter( filter2 );

        assertEquals( normalizedFilter1, normalizedFilter2 );
    }
}
