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
package org.apache.directory.shared.ldap.filter;


import org.apache.directory.shared.ldap.filter.BranchNormalizedVisitor;
import org.apache.directory.shared.ldap.filter.ExprNode;
import org.junit.Test;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;


/**
 * Tests the BranchNormalizedVisitor.
 * 
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 * @version $Rev$
 */
public class BranchNormalizedVisitorTest
{
    @Test
    public void testBranchNormalizedVisitor0() throws Exception
    {
        String filter = "(ou=Human Resources)";

        ExprNode ori = FilterParser.parse( filter );

        ExprNode altered = FilterParser.parse( filter );

        BranchNormalizedVisitor visitor = new BranchNormalizedVisitor();

        visitor.visit( altered );

        assertEquals( ori.toString(), altered.toString() );
    }


    @Test
    public void testBranchNormalizedVisitor1() throws Exception
    {
        String filter = "(&(ou=Human Resources)(uid=akarasulu))";

        ExprNode ori = FilterParser.parse( filter );

        ExprNode altered = FilterParser.parse( filter );

        BranchNormalizedVisitor visitor = new BranchNormalizedVisitor();

        visitor.visit( altered );

        assertEquals( ori.toString(), altered.toString() );
    }


    @Test
    public void testBranchNormalizedVisitor2() throws Exception
    {
        String filter = "(&(uid=akarasulu)(ou=Human Resources)";

        filter += "(|(uid=akarasulu)(ou=Human Resources))) ";

        ExprNode ori = FilterParser.parse( filter );

        ExprNode altered = FilterParser.parse( filter );

        BranchNormalizedVisitor visitor = new BranchNormalizedVisitor();

        visitor.visit( altered );

        assertFalse( ori.toString().equals( altered.toString() ) );
    }


    @Test
    public void testBranchNormalizedVisitor3() throws Exception
    {
        String filter = "(&(ou=Human Resources)(uid=akarasulu)";

        filter += "(|(ou=Human Resources)(uid=akarasulu)))";

        ExprNode ori = FilterParser.parse( filter );

        ExprNode altered = FilterParser.parse( filter );

        BranchNormalizedVisitor visitor = new BranchNormalizedVisitor();

        visitor.visit( altered );

        assertTrue( ori.toString().equals( altered.toString() ) );
    }


    @Test
    public void testBranchNormalizedComplex() throws Exception
    {
        String filter1 = "(&(a=A)(|(b=B)(c=C)))";

        String filter2 = "(&(a=A)(|(c=C)(b=B)))";

        String normalizedFilter1 = BranchNormalizedVisitor.getNormalizedFilter( filter1 );

        String normalizedFilter2 = BranchNormalizedVisitor.getNormalizedFilter( filter2 );

        assertEquals( normalizedFilter1, normalizedFilter2 );
    }

   public void testBranchNormalizedVisitor4() throws Exception
   {
       ExprNode ori = FilterParser.parse( "(&(!(sn=Bob))(ou=Human Resources)(uid=akarasulu))" );

       ExprNode altered = FilterParser.parse( "(&(ou=Human Resources)(uid=akarasulu)(!(sn=Bob)))" );

       BranchNormalizedVisitor visitor = new BranchNormalizedVisitor();

       visitor.visit( altered );

       assertTrue( ori.toString().equals( altered.toString() ) );
       
   }
     
    
}
