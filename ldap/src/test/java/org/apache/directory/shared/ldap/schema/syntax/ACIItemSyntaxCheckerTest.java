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

import org.apache.directory.shared.ldap.schema.syntaxes.ACIItemSyntaxChecker;

import org.junit.Test;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.assertEquals;


/**
 * Test cases for ACIItemSyntaxChecker.
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 * @version $Rev$
 */
public class ACIItemSyntaxCheckerTest
{
    ACIItemSyntaxChecker checker = new ACIItemSyntaxChecker();


    @Test
    public void testNullString()
    {
        assertFalse( checker.isValidSyntax( null ) );
    }


    @Test
    public void testEmptyString()
    {
        assertFalse( checker.isValidSyntax( "" ) );
    }

    @Test
    public void testOid()
    {
        assertEquals( "1.3.6.1.4.1.1466.115.121.1.1", checker.getSyntaxOid() );
    }

    @Test
    public void testCorrectCase()
    {
    }

    /**
     * Tests the checker with an ACIItem of ItemFirst main component.
     */
    @Test
    public void testItemFirst()
    {
        String spec = " {  identificationTag  \"id1\" , precedence 114  , authenticationLevel simple  , "
            + "itemOrUserFirst itemFirst  :{ protectedItems  { entry  , attributeType { 1.2.3    , ou }  , "
            + " attributeValue { ou=people  , cn=Ersin  }  , rangeOfValues (cn=ErsinEr) , "
            + "classes and : { item: xyz , or:{item:X,item:Y}   }}  , "
            + "itemPermissions { { userClasses {allUsers  , userGroup { \"1.2=y,z=t\"  , \"a=b,c=d\" } "
            + " , subtree { { base \"ou=people\" } } }   , grantsAndDenials  {  denyCompare  , grantModify } },"
            + "{ precedence 10, userClasses {allUsers  , userGroup { \"1.2=y,z=t\"  , \"a=b,c=d\" } "
            + " , subtree { { base \"ou=people\" } } }   , grantsAndDenials  {  denyCompare  , grantModify } } } }}";

        assertTrue( checker.isValidSyntax( spec ) );
    }


    /**
     * Tests the checker with an ACIItem of UserFirst main component.
     */
    @Test
    public void testUserFirst()
    {
        String spec = "{ identificationTag \"id2\"   , precedence 14, authenticationLevel none  , "
            + "itemOrUserFirst userFirst:  { userClasses {  allUsers  , name { \"ou=people,cn=ersin\" }, "
            + "subtree {{ base \"ou=system\" }, { base \"ou=ORGANIZATIONUNIT\","
            + "minimum  1, maximum   2 } } }  , "
            + "userPermissions { { protectedItems{ entry  , attributeType { cn  , ou }  , attributeValue {x=y,m=n,k=l} , "
            + "rangeOfValues (cn=ErsinEr) }  , grantsAndDenials { grantBrowse } } } }  }   ";

        assertTrue( checker.isValidSyntax( spec ) );
    }


    @Test
    public void testAllowAddAllUsers()
    {
        String spec = "{ identificationTag \"addAci\", " + "precedence 14, " + "authenticationLevel none, "
            + "itemOrUserFirst userFirst: { " + "userClasses { allUsers }, "
            + "userPermissions { { protectedItems {entry}, " + "grantsAndDenials { grantAdd } } } } }";

        assertTrue( checker.isValidSyntax( spec ) );
    }


    @Test
    public void testCombo()
    {
        String spec = "{ identificationTag \"addAci\", " + "precedence 14, " + "authenticationLevel none, "
            + "itemOrUserFirst userFirst: { " + "userClasses { allUsers, name { \"ou=blah\" } }, "
            + "userPermissions { { protectedItems {entry}, " + "grantsAndDenials { grantAdd } } } } }";

        assertTrue( checker.isValidSyntax( spec ) );
    }


    @Test
    public void testOrderOfProtectedItemsDoesNotMatter()
    {
        String spec = " {  identificationTag  \"id1\" , precedence 114  , authenticationLevel simple  , "
            + "itemOrUserFirst itemFirst  :{ protectedItems  { attributeType { 1.2.3    , ou }, entry , "
            + " rangeOfValues (cn=ErsinEr) , attributeValue { ou=people  , cn=Ersin  },"
            + "classes and : { item: xyz , or:{item:X,item:Y}   }}  , "
            + "itemPermissions { { userClasses {allUsers  , userGroup { \"1.2=y,z=t\"  , \"a=b,c=d\" } "
            + " , subtree { { base \"ou=people\" } } }   , grantsAndDenials  {  denyCompare  , grantModify } },"
            + "{ precedence 10, userClasses {allUsers  , userGroup { \"1.2=y,z=t\"  , \"a=b,c=d\" } "
            + " , subtree { { base \"ou=people\" } } }   , grantsAndDenials  {  denyCompare  , grantModify } } } }}";

        assertTrue( checker.isValidSyntax( spec ) );
    }


    @Test
    public void testOrderOfUserClassesDoesNotMatter()
    {
        String spec = "{ identificationTag \"id2\"   , precedence 14, authenticationLevel none  , "
            + "itemOrUserFirst userFirst:  { userClasses {  name { \"ou=people,cn=ersin\" }, allUsers, "
            + "subtree {{ base \"ou=system\" }, { base \"ou=ORGANIZATIONUNIT\","
            + "minimum  1, maximum   2 } } }  , "
            + "userPermissions { { protectedItems{ entry  , attributeType { cn  , ou }  , attributeValue {x=y,m=n,k=l} , "
            + "rangeOfValues (cn=ErsinEr) }  , grantsAndDenials { grantBrowse } } } }  }   ";

        assertTrue( checker.isValidSyntax( spec ) );
    }


    @Test
    public void testItemPermissionComponentsOrderDoesNotMatter()
    {
        String spec = " {  identificationTag  \"id1\" , precedence 114  , authenticationLevel simple  , "
            + "itemOrUserFirst itemFirst  :{ protectedItems  { attributeType { 1.2.3    , ou }, entry , "
            + " rangeOfValues (cn=ErsinEr) , attributeValue { ou=people  , cn=Ersin  },"
            + "classes and : { item: xyz , or:{item:X,item:Y}   }}  , "
            + "itemPermissions { { grantsAndDenials  {  denyCompare  , grantModify }, userClasses {allUsers  , userGroup { \"1.2=y,z=t\"  , \"a=b,c=d\" } "
            + " , subtree { { base \"ou=people\" } } }   },"
            + "{ precedence 10, userClasses {allUsers  , userGroup { \"1.2=y,z=t\"  , \"a=b,c=d\" } "
            + " , subtree { { base \"ou=people\" } } }   , grantsAndDenials  {  denyCompare  , grantModify } } } }}";

        assertTrue( checker.isValidSyntax( spec ) );
    }


    @Test
    public void testUserPermissionComponentsOrderDoesNotMatter()
    {
        String spec = "{ identificationTag \"id2\"   , precedence 14, authenticationLevel none  , "
            + "itemOrUserFirst userFirst:  { userClasses {  allUsers  , name { \"ou=people,cn=ersin\" }, "
            + "subtree {{ base \"ou=system\" }, { base \"ou=ORGANIZATIONUNIT\","
            + "minimum  1, maximum   2 } } }  , "
            + "userPermissions { { grantsAndDenials { grantBrowse }, protectedItems{ entry  , attributeType { cn  , ou }  , attributeValue {x=y,m=n,k=l} , "
            + "rangeOfValues (cn=ErsinEr) }  } } }  }   ";

        assertTrue( checker.isValidSyntax( spec ) );
    }


    @Test
    public void testOrderOfMainACIComponentsDoesNotMatter()
    {
        String spec = "{   itemOrUserFirst userFirst:  { userClasses {  allUsers  , name { \"ou=people,cn=ersin\" }, "
            + "subtree {{ base \"ou=system\" }, { base \"ou=ORGANIZATIONUNIT\","
            + "minimum  1, maximum   2 } } }  , "
            + "userPermissions { { protectedItems{ entry  , attributeType { cn  , ou }  , attributeValue {x=y,m=n,k=l} , "
            + "rangeOfValues (cn=ErsinEr) }  , grantsAndDenials { grantBrowse } } } }, "
            + " identificationTag \"id2\"   , authenticationLevel none, precedence 14 }   ";

        assertTrue( checker.isValidSyntax( spec ) );
    }


    @Test
    public void testUserFirstComponentsOrderDoesNotMatter()
    {
        String spec = "{ identificationTag \"id2\"   , precedence 14, authenticationLevel none  , "
            + "itemOrUserFirst userFirst:  { userPermissions { { protectedItems{ entry  , attributeType { cn  , ou }  , attributeValue {x=y,m=n,k=l} , "
            + "rangeOfValues (cn=ErsinEr) }  , grantsAndDenials { grantBrowse } } }, userClasses {  allUsers  , name { \"ou=people,cn=ersin\" }, "
            + "subtree {{ base \"ou=system\" }, { base \"ou=ORGANIZATIONUNIT\","
            + "minimum  1, maximum   2 } } } }  }   ";

        assertTrue( checker.isValidSyntax( spec ) );
    }


    @Test
    public void testItemFirstComponentsOrderDoesNotMatter()
    {
        String spec = " {  identificationTag  \"id1\" , precedence 114  , authenticationLevel simple  , "
            + "itemOrUserFirst itemFirst  :{ itemPermissions { { userClasses {allUsers  , userGroup { \"1.2=y,z=t\"  , \"a=b,c=d\" } "
            + " , subtree { { base \"ou=people\" } } }   , grantsAndDenials  {  denyCompare  , grantModify } },"
            + "{ precedence 10, userClasses {allUsers  , userGroup { \"1.2=y,z=t\"  , \"a=b,c=d\" } "
            + " , subtree { { base \"ou=people\" } } }   , grantsAndDenials  {  denyCompare  , grantModify } } },protectedItems  { entry  , attributeType { 1.2.3    , ou }  , "
            + " attributeValue { ou=people  , cn=Ersin  }  , rangeOfValues (cn=ErsinEr) , "
            + "classes and : { item: xyz , or:{item:X,item:Y}   }}  " + " }}";

        assertTrue( checker.isValidSyntax( spec ) );
    }


    @Test
    public void testRestrictedValueComponentsOrderDoesNotMatter()
    {
        String spec = "{ identificationTag \"id2\"   , precedence 14, authenticationLevel none  , "
            + "itemOrUserFirst userFirst:  { userClasses {  allUsers  , name { \"ou=people,cn=ersin\" }, "
            + "subtree {{ base \"ou=system\"}, { base \"ou=ORGANIZATIONUNIT\"," + "minimum  1, maximum   2 } } }  , "
            + "userPermissions { { protectedItems{ entry  , "
            + "maxValueCount { { type 10.11.12, maxCount 10 }, { maxCount 20, type 11.12.13  } } "
            + " }  , grantsAndDenials { grantBrowse } } } }  }   ";

        assertTrue( checker.isValidSyntax( spec ) );
    }


    @Test
    public void testMaxValueCountComponentsOrderDoesNotMatter()
    {
        String spec = "{ identificationTag \"id2\"   , precedence 14, authenticationLevel none  , "
            + "itemOrUserFirst userFirst:  { userClasses {  allUsers  , name { \"ou=people,cn=ersin\" }, "
            + "subtree {{ base \"ou=system\" }, { base \"ou=ORGANIZATIONUNIT\"," + "minimum  1, maximum   2 } } }  , "
            + "userPermissions { { protectedItems{ entry  , "
            + "restrictedBy { { type 10.11.12, valuesIn ou }, { valuesIn cn, type 11.12.13  } } "
            + " }  , grantsAndDenials { grantBrowse } } } }  }   ";

        assertTrue( checker.isValidSyntax( spec ) );
    }


    @Test
    public void testSubtreeSpecificationComponentsOrderDoesNotMatter()
    {
        String spec = "{ identificationTag \"id2\"   , precedence 14, authenticationLevel none  , "
            + "itemOrUserFirst userFirst:  { userPermissions { { protectedItems{ entry  , attributeType { cn  , ou }  , attributeValue {x=y,m=n,k=l} , "
            + "rangeOfValues (cn=ErsinEr) }  , grantsAndDenials { grantBrowse } } }, userClasses {  allUsers  , name { \"ou=people,cn=ersin\" }, "
            + "subtree {{ minimum 7, maximum 9, base \"ou=system\" }, { base \"ou=ORGANIZATIONUNIT\","
            + " maximum   2, minimum  1 } } }  }  }   ";

        assertTrue( checker.isValidSyntax( spec ) );
    }
    
    /**
     * Test case for DIRSERVER-891
     */
    @Test
    public void testInvalidAttributeValue()
    {
        String spec;
        
        // no name-value-pair
        spec = "{ identificationTag \"id2\"   , precedence 14, authenticationLevel none  , "
            + "itemOrUserFirst userFirst:  { userPermissions { { protectedItems{ entry  , attributeType { cn  , ou }  , attributeValue { must_be_a_name_value_pair } , "
            + "rangeOfValues (cn=ErsinEr) }  , grantsAndDenials { grantBrowse } } }, userClasses {  allUsers  , name { \"ou=people,cn=ersin\" }, "
            + "subtree {{ minimum 7, maximum 9, base \"ou=system\" }, { base \"ou=ORGANIZATIONUNIT\","
            + " maximum   2, minimum  1 } } }  }  }   ";
        assertFalse( checker.isValidSyntax( spec ) );
        
        // no name-value-pair
        spec = "{ identificationTag \"id2\"   , precedence 14, authenticationLevel none  , "
            + "itemOrUserFirst userFirst:  { userPermissions { { protectedItems{ entry  , attributeType { cn  , ou }  , attributeValue { x=y,m=n,k=l,x } , "
            + "rangeOfValues (cn=ErsinEr) }  , grantsAndDenials { grantBrowse } } }, userClasses {  allUsers  , name { \"ou=people,cn=ersin\" }, "
            + "subtree {{ minimum 7, maximum 9, base \"ou=system\" }, { base \"ou=ORGANIZATIONUNIT\","
            + " maximum   2, minimum  1 } } }  }  }   ";
        assertFalse( checker.isValidSyntax( spec ) );
    }
    
    /**
     * Test case for DIRSERVER-891
     */
    @Test
    public void testIncomplete()
    {
        String spec;
        
        spec = "{ }";
        assertFalse( checker.isValidSyntax( spec ) );
        
        spec = "{ identificationTag \"id2\" }";
        assertFalse( checker.isValidSyntax( spec ) );
        
        spec = "{ identificationTag \"id2\", precedence 14 } ";
        assertFalse( checker.isValidSyntax( spec ) );
        
        spec = "{ identificationTag \"id2\", precedence 14, authenticationLevel none } ";
        assertFalse( checker.isValidSyntax( spec ) );
    }
    
}
