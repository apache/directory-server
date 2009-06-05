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

package org.apache.directory.shared.ldap.aci;


import java.text.ParseException;

import org.apache.directory.shared.ldap.aci.ACIItemChecker;
import org.junit.Test;
import static org.junit.Assert.fail;


/**
 * Unit tests class for ACIItem checker (wrapper).
 * 
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 * @version $Rev: 437007 $
 */
public class ACIItemChekerTest
{

    /** the ACIItem checker wrapper */
    ACIItemChecker checker;


    // /** holds multithreaded success value */
    // boolean isSuccessMultithreaded = true;

    /**
     * Creates a ACIItemParserTest instance.
     */
    public ACIItemChekerTest()
    {
        super();
        checker = new ACIItemChecker();
    }


    /**
     * Tests the checker with an ACIItem of ItemFirst main component.
     */
    @Test
    public void testItemFirst() throws Exception
    {
        String spec = " {  identificationTag  \"id1\" , precedence 114  , authenticationLevel simple  , "
            + "itemOrUserFirst itemFirst  :{ protectedItems  { entry  , attributeType { 1.2.3    , ou }  , "
            + " attributeValue { ou=people  , cn=Ersin  }  , rangeOfValues (cn=ErsinEr) , "
            + "classes and : { item: xyz , or:{item:X,item:Y}   }}  , "
            + "itemPermissions { { userClasses {allUsers  , userGroup { \"1.2=y,z=t\"  , \"a=b,c=d\" } "
            + " , subtree { { base \"ou=people\" } } }   , grantsAndDenials  {  denyCompare  , grantModify } },"
            + "{ precedence 10, userClasses {allUsers  , userGroup { \"1.2=y,z=t\"  , \"a=b,c=d\" } "
            + " , subtree { { base \"ou=people\" } } }   , grantsAndDenials  {  denyCompare  , grantModify } } } }}";

        checker.parse( spec );
    }


    /**
     * Tests the checker with an ACIItem of UserFirst main component.
     */
    @Test
    public void testUserFirst() throws Exception
    {
        String spec = "{ identificationTag \"id2\"   , precedence 14, authenticationLevel none  , "
            + "itemOrUserFirst userFirst:  { userClasses {  allUsers  , name { \"ou=people,cn=ersin\" }, "
            + "subtree {{ base \"ou=system\" }, { base \"ou=ORGANIZATIONUNIT\","
            + "minimum  1, maximum   2 } } }  , "
            + "userPermissions { { protectedItems{ entry  , attributeType { cn  , ou }  , attributeValue {x=y,m=n,k=l} , "
            + "rangeOfValues (cn=ErsinEr) }  , grantsAndDenials { grantBrowse } } } }  }   ";

        checker.parse( spec );
    }


    @Test
    public void testAllowAddAllUsers() throws Exception
    {
        String spec = "{ identificationTag \"addAci\", " + "precedence 14, " + "authenticationLevel none, "
            + "itemOrUserFirst userFirst: { " + "userClasses { allUsers }, "
            + "userPermissions { { protectedItems {entry}, " + "grantsAndDenials { grantAdd } } } } }";

        checker.parse( spec );
    }


    @Test
    public void testCombo() throws Exception
    {
        String spec = "{ identificationTag \"addAci\", " + "precedence 14, " + "authenticationLevel none, "
            + "itemOrUserFirst userFirst: { " + "userClasses { allUsers, name { \"ou=blah\" } }, "
            + "userPermissions { { protectedItems {entry}, " + "grantsAndDenials { grantAdd } } } } }";

        checker.parse( spec );
    }


    @Test
    public void testOrderOfProtectedItemsDoesNotMatter() throws Exception
    {
        String spec = " {  identificationTag  \"id1\" , precedence 114  , authenticationLevel simple  , "
            + "itemOrUserFirst itemFirst  :{ protectedItems  { attributeType { 1.2.3    , ou }, entry , "
            + " rangeOfValues (cn=ErsinEr) , attributeValue { ou=people  , cn=Ersin  },"
            + "classes and : { item: xyz , or:{item:X,item:Y}   }}  , "
            + "itemPermissions { { userClasses {allUsers  , userGroup { \"1.2=y,z=t\"  , \"a=b,c=d\" } "
            + " , subtree { { base \"ou=people\" } } }   , grantsAndDenials  {  denyCompare  , grantModify } },"
            + "{ precedence 10, userClasses {allUsers  , userGroup { \"1.2=y,z=t\"  , \"a=b,c=d\" } "
            + " , subtree { { base \"ou=people\" } } }   , grantsAndDenials  {  denyCompare  , grantModify } } } }}";

        checker.parse( spec );
    }


    @Test
    public void testOrderOfUserClassesDoesNotMatter() throws Exception
    {
        String spec = "{ identificationTag \"id2\"   , precedence 14, authenticationLevel none  , "
            + "itemOrUserFirst userFirst:  { userClasses {  name { \"ou=people,cn=ersin\" }, allUsers, "
            + "subtree {{ base \"ou=system\" }, { base \"ou=ORGANIZATIONUNIT\","
            + "minimum  1, maximum   2 } } }  , "
            + "userPermissions { { protectedItems{ entry  , attributeType { cn  , ou }  , attributeValue {x=y,m=n,k=l} , "
            + "rangeOfValues (cn=ErsinEr) }  , grantsAndDenials { grantBrowse } } } }  }   ";

        checker.parse( spec );
    }


    @Test
    public void testItemPermissionComponentsOrderDoesNotMatter() throws Exception
    {
        String spec = " {  identificationTag  \"id1\" , precedence 114  , authenticationLevel simple  , "
            + "itemOrUserFirst itemFirst  :{ protectedItems  { attributeType { 1.2.3    , ou }, entry , "
            + " rangeOfValues (cn=ErsinEr) , attributeValue { ou=people  , cn=Ersin  },"
            + "classes and : { item: xyz , or:{item:X,item:Y}   }}  , "
            + "itemPermissions { { grantsAndDenials  {  denyCompare  , grantModify }, userClasses {allUsers  , userGroup { \"1.2=y,z=t\"  , \"a=b,c=d\" } "
            + " , subtree { { base \"ou=people\" } } }   },"
            + "{ precedence 10, userClasses {allUsers  , userGroup { \"1.2=y,z=t\"  , \"a=b,c=d\" } "
            + " , subtree { { base \"ou=people\" } } }   , grantsAndDenials  {  denyCompare  , grantModify } } } }}";

        checker.parse( spec );
    }


    @Test
    public void testUserPermissionComponentsOrderDoesNotMatter() throws Exception
    {
        String spec = "{ identificationTag \"id2\"   , precedence 14, authenticationLevel none  , "
            + "itemOrUserFirst userFirst:  { userClasses {  allUsers  , name { \"ou=people,cn=ersin\" }, "
            + "subtree {{ base \"ou=system\" }, { base \"ou=ORGANIZATIONUNIT\","
            + "minimum  1, maximum   2 } } }  , "
            + "userPermissions { { grantsAndDenials { grantBrowse }, protectedItems{ entry  , attributeType { cn  , ou }  , attributeValue {x=y,m=n,k=l} , "
            + "rangeOfValues (cn=ErsinEr) }  } } }  }   ";

        checker.parse( spec );
    }


    @Test
    public void testOrderOfMainACIComponentsDoesNotMatter() throws Exception
    {
        String spec = "{   itemOrUserFirst userFirst:  { userClasses {  allUsers  , name { \"ou=people,cn=ersin\" }, "
            + "subtree {{ base \"ou=system\" }, { base \"ou=ORGANIZATIONUNIT\","
            + "minimum  1, maximum   2 } } }  , "
            + "userPermissions { { protectedItems{ entry  , attributeType { cn  , ou }  , attributeValue {x=y,m=n,k=l} , "
            + "rangeOfValues (cn=ErsinEr) }  , grantsAndDenials { grantBrowse } } } }, "
            + " identificationTag \"id2\"   , authenticationLevel none, precedence 14 }   ";

        checker.parse( spec );
    }


    @Test
    public void testUserFirstComponentsOrderDoesNotMatter() throws Exception
    {
        String spec = "{ identificationTag \"id2\"   , precedence 14, authenticationLevel none  , "
            + "itemOrUserFirst userFirst:  { userPermissions { { protectedItems{ entry  , attributeType { cn  , ou }  , attributeValue {x=y,m=n,k=l} , "
            + "rangeOfValues (cn=ErsinEr) }  , grantsAndDenials { grantBrowse } } }, userClasses {  allUsers  , name { \"ou=people,cn=ersin\" }, "
            + "subtree {{ base \"ou=system\" }, { base \"ou=ORGANIZATIONUNIT\","
            + "minimum  1, maximum   2 } } } }  }   ";

        checker.parse( spec );
    }


    @Test
    public void testItemFirstComponentsOrderDoesNotMatter() throws Exception
    {
        String spec = " {  identificationTag  \"id1\" , precedence 114  , authenticationLevel simple  , "
            + "itemOrUserFirst itemFirst  :{ itemPermissions { { userClasses {allUsers  , userGroup { \"1.2=y,z=t\"  , \"a=b,c=d\" } "
            + " , subtree { { base \"ou=people\" } } }   , grantsAndDenials  {  denyCompare  , grantModify } },"
            + "{ precedence 10, userClasses {allUsers  , userGroup { \"1.2=y,z=t\"  , \"a=b,c=d\" } "
            + " , subtree { { base \"ou=people\" } } }   , grantsAndDenials  {  denyCompare  , grantModify } } },protectedItems  { entry  , attributeType { 1.2.3    , ou }  , "
            + " attributeValue { ou=people  , cn=Ersin  }  , rangeOfValues (cn=ErsinEr) , "
            + "classes and : { item: xyz , or:{item:X,item:Y}   }}  " + " }}";

        checker.parse( spec );
    }


    @Test
    public void testRestrictedValueComponentsOrderDoesNotMatter() throws Exception
    {
        String spec = "{ identificationTag \"id2\"   , precedence 14, authenticationLevel none  , "
            + "itemOrUserFirst userFirst:  { userClasses {  allUsers  , name { \"ou=people,cn=ersin\" }, "
            + "subtree {{ base \"ou=system\"}, { base \"ou=ORGANIZATIONUNIT\"," + "minimum  1, maximum   2 } } }  , "
            + "userPermissions { { protectedItems{ entry  , "
            + "maxValueCount { { type 10.11.12, maxCount 10 }, { maxCount 20, type 11.12.13  } } "
            + " }  , grantsAndDenials { grantBrowse } } } }  }   ";

        checker.parse( spec );
    }


    @Test
    public void testMaxValueCountComponentsOrderDoesNotMatter() throws Exception
    {
        String spec = "{ identificationTag \"id2\"   , precedence 14, authenticationLevel none  , "
            + "itemOrUserFirst userFirst:  { userClasses {  allUsers  , name { \"ou=people,cn=ersin\" }, "
            + "subtree {{ base \"ou=system\" }, { base \"ou=ORGANIZATIONUNIT\"," + "minimum  1, maximum   2 } } }  , "
            + "userPermissions { { protectedItems{ entry  , "
            + "restrictedBy { { type 10.11.12, valuesIn ou }, { valuesIn cn, type 11.12.13  } } "
            + " }  , grantsAndDenials { grantBrowse } } } }  }   ";

        checker.parse( spec );
    }


    @Test
    public void testSubtreeSpecificationComponentsOrderDoesNotMatter() throws Exception
    {
        String spec = "{ identificationTag \"id2\"   , precedence 14, authenticationLevel none  , "
            + "itemOrUserFirst userFirst:  { userPermissions { { protectedItems{ entry  , attributeType { cn  , ou }  , attributeValue {x=y,m=n,k=l} , "
            + "rangeOfValues (cn=ErsinEr) }  , grantsAndDenials { grantBrowse } } }, userClasses {  allUsers  , name { \"ou=people,cn=ersin\" }, "
            + "subtree {{ minimum 7, maximum 9, base \"ou=system\" }, { base \"ou=ORGANIZATIONUNIT\","
            + " maximum   2, minimum  1 } } }  }  }   ";

        checker.parse( spec );
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
        try
        {
            checker.parse( spec );
            fail("Expected ParseException, invalid protected item 'attributeValue { must_be_a_name_value_pair }'");
        }
        catch ( ParseException e )
        {
            // Expected
        }
        
        // no name-value-pair
        spec = "{ identificationTag \"id2\"   , precedence 14, authenticationLevel none  , "
            + "itemOrUserFirst userFirst:  { userPermissions { { protectedItems{ entry  , attributeType { cn  , ou }  , attributeValue { x=y,m=n,k=l,x } , "
            + "rangeOfValues (cn=ErsinEr) }  , grantsAndDenials { grantBrowse } } }, userClasses {  allUsers  , name { \"ou=people,cn=ersin\" }, "
            + "subtree {{ minimum 7, maximum 9, base \"ou=system\" }, { base \"ou=ORGANIZATIONUNIT\","
            + " maximum   2, minimum  1 } } }  }  }   ";
        try
        {
            checker.parse( spec );
            fail("Expected ParseException, invalid protected item 'attributeValue { must_be_a_name_value_pair }'");
        }
        catch ( ParseException e )
        {
            // Expected
        }
    }
    
    
    /**
     * Test case for DIRSERVER-891
     */
    @Test
    public void testIncomplete()
    {
        String spec;
        
        spec = "{ }";
        try
        {
            checker.parse( spec );
            fail("Expected ParseException, ACIItem is incomplete'");
        }
        catch ( ParseException e )
        {
            // Expected
        }
        
        spec = "{ identificationTag \"id2\" }";
        try
        {
            checker.parse( spec );
            fail("Expected ParseException, ACIItem is incomplete'");
        }
        catch ( ParseException e )
        {
            // Expected
        }
        
        spec = "{ identificationTag \"id2\", precedence 14 } ";
        try
        {
            checker.parse( spec );
            fail("Expected ParseException, ACIItem is incomplete'");
        }
        catch ( ParseException e )
        {
            // Expected
        }
        
        spec = "{ identificationTag \"id2\", precedence 14, authenticationLevel none } ";
        try
        {
            checker.parse( spec );
            fail("Expected ParseException, ACIItem is incomplete'");
        }
        catch ( ParseException e )
        {
            // Expected
        }
    }
}
