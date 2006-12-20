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

import junit.framework.TestCase;

/**
 * Test cases for ACIItemSyntaxChecker.
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 * @version $Rev$
 */
public class ACIItemSyntaxCheckerTest extends TestCase
{
    ACIItemSyntaxChecker checker = new ACIItemSyntaxChecker();


    public void testNullString()
    {
        assertFalse( checker.isValidSyntax( null ) );
    }


    public void testEmptyString()
    {
        assertFalse( checker.isValidSyntax( "" ) );
    }

    public void testOid()
    {
        assertEquals( "1.3.6.1.4.1.1466.115.121.1.1", checker.getSyntaxOid() );
    }

    public void testCorrectCase()
    {
    }

    /**
     * Tests the checker with an ACIItem of ItemFirst main component.
     */
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


    public void testAllowAddAllUsers()
    {
        String spec = "{ identificationTag \"addAci\", " + "precedence 14, " + "authenticationLevel none, "
            + "itemOrUserFirst userFirst: { " + "userClasses { allUsers }, "
            + "userPermissions { { protectedItems {entry}, " + "grantsAndDenials { grantAdd } } } } }";

        assertTrue( checker.isValidSyntax( spec ) );
    }


    public void testCombo()
    {
        String spec = "{ identificationTag \"addAci\", " + "precedence 14, " + "authenticationLevel none, "
            + "itemOrUserFirst userFirst: { " + "userClasses { allUsers, name { \"ou=blah\" } }, "
            + "userPermissions { { protectedItems {entry}, " + "grantsAndDenials { grantAdd } } } } }";

        assertTrue( checker.isValidSyntax( spec ) );
    }


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


    public void testUserFirstComponentsOrderDoesNotMatter()
    {
        String spec = "{ identificationTag \"id2\"   , precedence 14, authenticationLevel none  , "
            + "itemOrUserFirst userFirst:  { userPermissions { { protectedItems{ entry  , attributeType { cn  , ou }  , attributeValue {x=y,m=n,k=l} , "
            + "rangeOfValues (cn=ErsinEr) }  , grantsAndDenials { grantBrowse } } }, userClasses {  allUsers  , name { \"ou=people,cn=ersin\" }, "
            + "subtree {{ base \"ou=system\" }, { base \"ou=ORGANIZATIONUNIT\","
            + "minimum  1, maximum   2 } } } }  }   ";

        assertTrue( checker.isValidSyntax( spec ) );
    }


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


    public void testSubtreeSpecificationComponentsOrderDoesNotMatter()
    {
        String spec = "{ identificationTag \"id2\"   , precedence 14, authenticationLevel none  , "
            + "itemOrUserFirst userFirst:  { userPermissions { { protectedItems{ entry  , attributeType { cn  , ou }  , attributeValue {x=y,m=n,k=l} , "
            + "rangeOfValues (cn=ErsinEr) }  , grantsAndDenials { grantBrowse } } }, userClasses {  allUsers  , name { \"ou=people,cn=ersin\" }, "
            + "subtree {{ minimum 7, maximum 9, base \"ou=system\" }, { base \"ou=ORGANIZATIONUNIT\","
            + " maximum   2, minimum  1 } } }  }  }   ";

        assertTrue( checker.isValidSyntax( spec ) );
    }
}
