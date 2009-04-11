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

import org.junit.Test;
import static org.junit.Assert.fail;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertEquals;

/**
 * Unit tests class for ACIItem parser (wrapper).
 * 
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 * @version $Rev$
 */
public class ACIItemParserTest
{

    /** the ACIItem parser wrapper */
    ACIItemParser parser;


    /**
     * Creates a ACIItemParserTest instance.
     */
    public ACIItemParserTest()
    {
        super();
        parser = new ACIItemParser( null );
    }


    /**
     * Creates a ACIItemParserTest instance.
     */
    public ACIItemParserTest( String s )
    {
        parser = new ACIItemParser( null );
    }


    private void checkItemToString( String spec, ACIItem item ) throws Exception
    {
        // try to parse the result of item.toString() again
        parser.parse( item.toString() );
    }


    /**
     * Tests the parser with a rangeOfValues with a nested filter.
     */
    @Test
    public void testRangeOfValues() throws Exception
    {
        String spec = " {  identificationTag  \"id1\" , precedence 114  , authenticationLevel simple  , "
            + "itemOrUserFirst itemFirst  :{ protectedItems  "
            + "{ rangeOfValues (&(&(|(|(cn=ccc)(!(cn=ddd))(&(cn=aaa)(cn=bbb)))))) " + "}  , itemPermissions {  } } }";

        ACIItem item = parser.parse( spec );
        checkItemToString( spec, item );
        
        
        spec = " { identificationTag \"id8\", precedence 0, authenticationLevel simple "
            + ", itemOrUserFirst userFirst: { userClasses { allUsers }, userPermissions { "
            + " { protectedItems { rangeOfValues (&(cn=test)(sn=test)) }, grantsAndDenials { grantAdd } }, "
            + "{ protectedItems { rangeOfValues (|(!(cn=aaa))(sn=bbb)) }, grantsAndDenials { grantAdd } } "
            + " } } }";
        
        item = parser.parse( spec );
        checkItemToString( spec, item );
    }


    /**
     * Tests the parser with an ACIItem of ItemFirst main component.
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

        ACIItem item = parser.parse( spec );
        checkItemToString( spec, item );
    }


    /**
     * Tests the parser with an ACIItem of UserFirst main component.
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

        ACIItem item = parser.parse( spec );
        checkItemToString( spec, item );
    }


    @Test 
    public void testAllowAddAllUsers() throws Exception
    {
        String spec = "{ identificationTag \"addAci\", " + "precedence 14, " + "authenticationLevel none, "
            + "itemOrUserFirst userFirst: { " + "userClasses { allUsers }, "
            + "userPermissions { { protectedItems {entry}, " + "grantsAndDenials { grantAdd } } } } }";

        ACIItem item = parser.parse( spec );
        checkItemToString( spec, item );
    }


    @Test 
    public void testCombo() throws Exception
    {
        String spec = "{ identificationTag \"addAci\", " + "precedence 14, " + "authenticationLevel none, "
            + "itemOrUserFirst userFirst: { " + "userClasses { allUsers, name { \"ou=blah\" } }, "
            + "userPermissions { { protectedItems {entry}, " + "grantsAndDenials { grantAdd } } } } }";

        ACIItem item = parser.parse( spec );
        checkItemToString( spec, item );
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

        ACIItem item = parser.parse( spec );
        checkItemToString( spec, item );
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

        ACIItem item = parser.parse( spec );
        checkItemToString( spec, item );
    }


    @Test 
    public void testOrderOfProtectedItemsDoesNotMatterButDuplicatesMatter() throws Exception
    {
        String spec = " {  identificationTag  \"id1\" , precedence 114  , authenticationLevel simple  , "
            + "itemOrUserFirst itemFirst  :{ protectedItems  { attributeType { 1.2.3    , ou }, entry, entry , "
            + " rangeOfValues (cn=ErsinEr) , attributeValue { ou=people  , cn=Ersin  },"
            + "classes and : { item: xyz , or:{item:X,item:Y}   }}  , "
            + "itemPermissions { { userClasses {allUsers  , userGroup { \"1.2=y,z=t\"  , \"a=b,c=d\" } "
            + " , subtree { { base \"ou=people\" } } }   , grantsAndDenials  {  denyCompare  , grantModify } },"
            + "{ precedence 10, userClasses {allUsers  , userGroup { \"1.2=y,z=t\"  , \"a=b,c=d\" } "
            + " , subtree { { base \"ou=people\" } } }   , grantsAndDenials  {  denyCompare  , grantModify } } } }}";

        try
        {
            parser.parse( spec );
            fail( "testItemFirstOrderOfProtectedItemsDoesNotMatterButDuplicatesMatter() should not have run this line." );
        }
        catch ( ParseException e )
        {
            assertNotNull( e );
        }
    }


    @Test 
    public void testOrderOfUserClassesDoesNotMatterButDuplicatesMatter() throws Exception
    {
        String spec = "{ identificationTag \"id2\"   , precedence 14, authenticationLevel none  , "
            + "itemOrUserFirst userFirst:  { userClasses {  name { \"ou=people,cn=ersin\" }, allUsers, allUsers, "
            + "subtree {{ base \"ou=system\" }, { base \"ou=ORGANIZATIONUNIT\","
            + "minimum  1, maximum   2 } } }  , "
            + "userPermissions { { protectedItems{ entry  , attributeType { cn  , ou }  , attributeValue {x=y,m=n,k=l} , "
            + "rangeOfValues (cn=ErsinEr) }  , grantsAndDenials { grantBrowse } } } }  }   ";

        try
        {
            parser.parse( spec );
            fail( "testUserFirstOrderOfUserClassesDoesNotMatterButDuplicatesMatter() should not have run this line." );
        }
        catch ( ParseException e )
        {
            assertNotNull( e );
        }
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

        ACIItem item = parser.parse( spec );
        checkItemToString( spec, item );
    }


    @Test 
    public void testItemPermissionComponentsOrderDoesNotMatterButDuplicatesMatter() throws Exception
    {
        String spec = " {  identificationTag  \"id1\" , precedence 114  , authenticationLevel simple  , "
            + "itemOrUserFirst itemFirst  :{ protectedItems  { attributeType { 1.2.3    , ou }, entry , "
            + " rangeOfValues (cn=ErsinEr) , attributeValue { ou=people  , cn=Ersin  },"
            + "classes and : { item: xyz , or:{item:X,item:Y}   }}  , "
            + "itemPermissions { { userClasses {allUsers  , userGroup { \"1.2=y,z=t\"  , \"a=b,c=d\" } "
            + " , subtree { { base \"ou=people\" } } }, grantsAndDenials  {  denyCompare  , grantModify }, userClasses {allUsers  , userGroup { \"1.2=y,z=t\"  , \"a=b,c=d\" } "
            + " , subtree { { base \"ou=people\" } } }   },"
            + "{ precedence 10, userClasses {allUsers  , userGroup { \"1.2=y,z=t\"  , \"a=b,c=d\" } "
            + " , subtree { { base \"ou=people\" } } }   , grantsAndDenials  {  denyCompare  , grantModify } } } }}";

        try
        {
            parser.parse( spec );
            fail( "testItemPermissionComponentsOrderDoesNotMatterButDuplicatesMatter() should not have run this line." );
        }
        catch ( ParseException e )
        {
            assertNotNull( e );
        }
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

        ACIItem item = parser.parse( spec );
        checkItemToString( spec, item );
    }


    @Test 
    public void testUserPermissionComponentsOrderDoesNotMatterButDuplicatesMatter() throws Exception
    {
        String spec = "{ identificationTag \"id2\"   , precedence 14, authenticationLevel none  , "
            + "itemOrUserFirst userFirst:  { userClasses {  allUsers  , name { \"ou=people,cn=ersin\" }, "
            + "subtree {{ base \"ou=system\" }, { base \"ou=ORGANIZATIONUNIT\","
            + "minimum  1, maximum   2 } } }  , "
            + "userPermissions { { grantsAndDenials { grantBrowse }, grantsAndDenials { grantBrowse }, protectedItems{ entry  , attributeType { cn  , ou }  , attributeValue {x=y,m=n,k=l} , "
            + "rangeOfValues (cn=ErsinEr) }  } } }  }   ";

        try
        {
            parser.parse( spec );
            fail( "testUserPermissionComponentsOrderDoesNotMatterButDuplicatesMatter() should not have run this line." );
        }
        catch ( ParseException e )
        {
            assertNotNull( e );
        }
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

        ACIItem item = parser.parse( spec );
        checkItemToString( spec, item );
    }


    @Test 
    public void testOrderOfMainACIComponentsDoesNotMatterButDuplicatesMatter() throws Exception
    {
        String spec = "{   itemOrUserFirst userFirst:  { userClasses {  allUsers  , name { \"ou=people,cn=ersin\" }, "
            + "subtree {{ base \"ou=system\" }, { base \"ou=ORGANIZATIONUNIT\","
            + "minimum  1, maximum   2 } } }  , "
            + "userPermissions { { protectedItems{ entry  , attributeType { cn  , ou }  , attributeValue {x=y,m=n,k=l} , "
            + "rangeOfValues (cn=ErsinEr) }  , grantsAndDenials { grantBrowse } } } }, "
            + " identificationTag \"id2\"   , authenticationLevel none, authenticationLevel simple, precedence 14 }   ";

        try
        {
            parser.parse( spec );
            fail( "testOrderOfMainACIComponentsDoesNotMatterButDuplicatesMatter() should not have run this line." );
        }
        catch ( ParseException e )
        {
            assertNotNull( e );
        }
    }


    @Test 
    public void testOrderOfMainACIComponentsDoesNotMatterButMissingsMatter() throws Exception
    {
        String spec = "{   itemOrUserFirst userFirst:  { userClasses {  allUsers  , name { \"ou=people,cn=ersin\" }, "
            + "subtree {{ base \"ou=system\" }, { base \"ou=ORGANIZATIONUNIT\","
            + "minimum  1, maximum   2 } } }  , "
            + "userPermissions { { protectedItems{ entry  , attributeType { cn  , ou }  , attributeValue {x=y,m=n,k=l} , "
            + "rangeOfValues (cn=ErsinEr) }  , grantsAndDenials { grantBrowse } } } }, "
            + " identificationTag \"id2\"   , precedence 14 }   ";

        try
        {
            parser.parse( spec );
            fail( "testOrderOfMainACIComponentsDoesNotMatterButMissingsMatter() should not have run this line." );
        }
        catch ( ParseException e )
        {
            assertNotNull( e );
        }
    }


    @Test 
    public void testUserFirstComponentsOrderDoesNotMatter() throws Exception
    {
        String spec = "{ identificationTag \"id2\"   , precedence 14, authenticationLevel none  , "
            + "itemOrUserFirst userFirst:  { userPermissions { { protectedItems{ entry  , attributeType { cn  , ou }  , attributeValue {x=y,m=n,k=l} , "
            + "rangeOfValues (cn=ErsinEr) }  , grantsAndDenials { grantBrowse } } }, userClasses {  allUsers  , name { \"ou=people,cn=ersin\" }, "
            + "subtree {{ base \"ou=system\" }, { base \"ou=ORGANIZATIONUNIT\","
            + "minimum  1, maximum   2 } } } }  }   ";

        ACIItem item = parser.parse( spec );
        checkItemToString( spec, item );
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

        ACIItem item = parser.parse( spec );
        checkItemToString( spec, item );
    }


    @Test 
    public void testGrantAndDenialBitsOrderDoesNotMatterButDuplicatesMatter() throws Exception
    {
        String spec = "{ identificationTag \"id2\"   , precedence 14, authenticationLevel none  , "
            + "itemOrUserFirst userFirst:  { userClasses {  allUsers }  , "
            + "userPermissions { { protectedItems{ entry  }  , grantsAndDenials { grantBrowse, grantInvoke, denyAdd, grantBrowse } } } }  }";

        try
        {
            parser.parse( spec );
            fail( "testGrantAndDenialBitsOrderDoesNotMatterButDuplicatesMatter() should not have run this line." );
        }
        catch ( ParseException e )
        {
            assertNotNull( e );
        }
    }


    @Test 
    public void testMaxValueCountComponentsOrderDoesNotMatter() throws Exception
    {
        String spec = "{ identificationTag \"id2\"   , precedence 14, authenticationLevel none  , "
            + "itemOrUserFirst userFirst:  { userClasses {  allUsers  , name { \"ou=people,cn=ersin\" }, "
            + "subtree {{ base \"ou=system\"}, { base \"ou=ORGANIZATIONUNIT\"," + "minimum  1, maximum   2 } } }  , "
            + "userPermissions { { protectedItems{ entry  , "
            + "maxValueCount { { type 10.11.12, maxCount 10 }, { maxCount 20, type 11.12.13  } } "
            + " }  , grantsAndDenials { grantBrowse } } } }  }   ";

        ACIItem item = parser.parse( spec );
        checkItemToString( spec, item );
    }


    @Test 
    public void testRestrictedValueComponentsOrderDoesNotMatter() throws Exception
    {
        String spec = "{ identificationTag \"id2\"   , precedence 14, authenticationLevel none  , "
            + "itemOrUserFirst userFirst:  { userClasses {  allUsers  , name { \"ou=people,cn=ersin\" }, "
            + "subtree {{ base \"ou=system\" }, { base \"ou=ORGANIZATIONUNIT\"," + "minimum  1, maximum   2 } } }  , "
            + "userPermissions { { protectedItems{ entry  , "
            + "restrictedBy { { type 10.11.12, valuesIn ou }, { valuesIn cn, type 11.12.13  } } "
            + " }  , grantsAndDenials { grantBrowse } } } }  }   ";

        ACIItem item = parser.parse( spec );
        checkItemToString( spec, item );
    }


    @Test 
    public void testMaxImmSubComponentsOrderDoesNotMatter() throws Exception
    {
        String spec = "{ identificationTag \"id2\"   , precedence 14, authenticationLevel none  , "
            + "itemOrUserFirst userFirst:  { userClasses {  allUsers  , name { \"ou=people,cn=ersin\" }, "
            + "subtree {{ base \"ou=system\" }, { base \"ou=ORGANIZATIONUNIT\"," + "minimum  1, maximum   2 } } }  , "
            + "userPermissions { { protectedItems{ entry  , maxImmSub 5 "
            + " }  , grantsAndDenials { grantBrowse } } } }  }   ";
        
        ACIItem item = parser.parse( spec );
        checkItemToString( spec, item );
    }
    
    
    @Test 
    public void testSubtreeSpecificationComponentsOrderDoesNotMatter() throws Exception
    {
        String spec = "{ identificationTag \"id2\"   , precedence 14, authenticationLevel none  , "
            + "itemOrUserFirst userFirst:  { userPermissions { { protectedItems{ entry  , attributeType { cn  , ou }  , attributeValue {x=y,m=n,k=l} , "
            + "rangeOfValues (cn=ErsinEr) }  , grantsAndDenials { grantBrowse } } }, userClasses {  allUsers  , name { \"ou=people,cn=ersin\" }, "
            + "subtree {{ minimum 7, maximum 9, base \"ou=system\" }, { base \"ou=ORGANIZATIONUNIT\","
            + " maximum   2, minimum  1 } } }  }  }   ";

        ACIItem item = parser.parse( spec );
        checkItemToString( spec, item );
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
            parser.parse( spec );
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
            parser.parse( spec );
            fail("Expected ParseException, invalid protected item 'attributeValue { x=y,m=n,k=l,x }'");
        }
        catch ( ParseException e )
        {
            // Expected
        }
    }
    
    @Test 
    public void testUserClassParentOfEntry() throws Exception
    {
        String spec = "{ identificationTag \"id\"   , precedence 10, authenticationLevel none  , "
            + "itemOrUserFirst userFirst:  { userClasses {  parentOfEntry  , name { \"cn=ersin,ou=people\" }, "
            + "subtree {{ base \"ou=system\" }, { base \"ou=ORGANIZATIONUNIT\","
            + "minimum  1, maximum   2 } } }  , "
            + "userPermissions { { protectedItems{ entry  , attributeType { cn  , ou }  , attributeValue {x=y,m=n,k=l} , "
            + "rangeOfValues (cn=ErsinEr) }  , grantsAndDenials { grantBrowse } } } }  }   ";

        ACIItem item = parser.parse( spec );
        checkItemToString( spec, item );
    }


    /**
     * Test case for DIRSTUDIO-440
     */
    @Test 
    public void testPrecedenceOfUserFirst() throws Exception
    {
        String spec = "{ identificationTag \"test\", precedence 14, authenticationLevel simple, "
            + "itemOrUserFirst userFirst: { userClasses { allUsers }, userPermissions { { "
            + "precedence 1, protectedItems { attributeType { userPassword } }, grantsAndDenials "
            + "{ denyRead, denyReturnDN, denyBrowse } }, { precedence 2, protectedItems "
            + "{ entry, allUserAttributeTypesAndValues }, grantsAndDenials "
            + "{ grantReturnDN, grantRead, grantBrowse } } } } }";

        ACIItem item = parser.parse( spec );
        checkItemToString( spec, item );

        UserFirstACIItem userFirstItem = ( UserFirstACIItem ) item;
        int aciPrecedence = userFirstItem.getPrecedence();
        assertEquals( 14, aciPrecedence );
        for ( UserPermission permission : userFirstItem.getUserPermission() )
        {
            int precedence = permission.getPrecedence();
            if ( precedence == 1 )
            {
                assertEquals( 1, precedence );
            }
            else if ( precedence == 2 )
            {
                assertEquals( 2, precedence );
            }
            else
            {
                fail( "Got precedence " + precedence + ", excpected precedence 1 or 2." );
            }
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
            parser.parse( spec );
            fail("Expected ParseException, ACIItem is incomplete'");
        }
        catch ( ParseException e )
        {
            // Expected
        }
        
        spec = "{ identificationTag \"id2\" }";
        try
        {
            parser.parse( spec );
            fail("Expected ParseException, ACIItem is incomplete'");
        }
        catch ( ParseException e )
        {
            // Expected
        }
        
        spec = "{ identificationTag \"id2\", precedence 14 } ";
        try
        {
            parser.parse( spec );
            fail("Expected ParseException, ACIItem is incomplete'");
        }
        catch ( ParseException e )
        {
            // Expected
        }
        
        spec = "{ identificationTag \"id2\", precedence 14, authenticationLevel none } ";
        try
        {
            parser.parse( spec );
            fail("Expected ParseException, ACIItem is incomplete'");
        }
        catch ( ParseException e )
        {
            // Expected
        }
    }
}
