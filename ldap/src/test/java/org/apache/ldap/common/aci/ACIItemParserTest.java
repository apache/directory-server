/*
 *   Copyright 2005 The Apache Software Foundation
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


package org.apache.ldap.common.aci;


import java.text.ParseException;

import junit.framework.TestCase;


/**
 * Unit tests class for ACIItem parser (wrapper).
 * 
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 * @version $Rev$
 */
public class ACIItemParserTest extends TestCase
{
    
    /** the ACIItem parser wrapper */
    ACIItemParser parser;
    
    // /** holds multithreaded success value */
    // boolean isSuccessMultithreaded = true;


    /**
     * Creates a ACIItemParserTest instance.
     */
    public ACIItemParserTest()
    {
        super();
        parser = new ACIItemParser();
    }

    
    /**
     * Creates a ACIItemParserTest instance.
     */
    public ACIItemParserTest( String s )
    {
        super( s );
        parser = new ACIItemParser();
    }


    /**
     * Tests the parser with an ACIItem of ItemFirst main component.
     */    
    public void testItemFirst() throws Exception
    {
        String spec = " {  identificationTag  \"id1\" , precedence 114  , authenticationLevel simple  , " +
                "itemOrUserFirst itemFirst  :{ protectedItems  { entry  , attributeType { 1.2.3    , ou }  , " +
                " attributeValue { ou=people  , cn=Ersin  }  , rangeOfValues (cn=ErsinEr) , " +
                "classes and : { item: xyz , or:{item:X,item:Y}   }}  , " +
                "itemPermissions { { userClasses {allUsers  , userGroup { \"1.2=y,z=t\"  , \"a=b,c=d\" } " +
                " , subtree { { base \"ou=people\" } } }   , grantsAndDenials  {  denyCompare  , grantModify } }," +
                "{ precedence 10, userClasses {allUsers  , userGroup { \"1.2=y,z=t\"  , \"a=b,c=d\" } " +
                " , subtree { { base \"ou=people\" } } }   , grantsAndDenials  {  denyCompare  , grantModify } } } }}";
        
        parser.parse( spec );
    }


    /**
     * Tests the parser with an ACIItem of UserFirst main component.
     */    
    public void testUserFirst() throws Exception
    {
        String spec = "{ identificationTag \"id2\"   , precedence 14, authenticationLevel none  , " +
                "itemOrUserFirst userFirst:  { userClasses {  allUsers  , name { \"ou=people,cn=ersin\" }, " +
                "subtree {{ base \"ou=system\" }, { base \"ou=ORGANIZATIONUNIT\"," +
                "minimum  1, maximum   2 } } }  , " +
                "userPermissions { { protectedItems{ entry  , attributeType { cn  , ou }  , attributeValue {x=y,m=n,k=l} , " +
                "rangeOfValues (cn=ErsinEr) }  , grantsAndDenials { grantBrowse } } } }  }   ";
        
        parser.parse( spec );
    }


    public void testAllowAddAllUsers() throws Exception
    {
        String spec = "{ identificationTag \"addAci\", " +
                "precedence 14, " +
                "authenticationLevel none, " +
                "itemOrUserFirst userFirst: { " +
                "userClasses { allUsers }, " +
                "userPermissions { { protectedItems {entry}, " +
                "grantsAndDenials { grantAdd } } } } }";
        
        parser.parse( spec );
    }


    public void testCombo() throws Exception
    {
        String spec = "{ identificationTag \"addAci\", " +
                "precedence 14, " +
                "authenticationLevel none, " +
                "itemOrUserFirst userFirst: { " +
                "userClasses { allUsers, name { \"ou=blah\" } }, " +
                "userPermissions { { protectedItems {entry}, " +
                "grantsAndDenials { grantAdd } } } } }";
        
        parser.parse( spec );
    }
    
    
    public void testOrderOfProtectedItemsDoesNotMatter() throws Exception
    {
        String spec = " {  identificationTag  \"id1\" , precedence 114  , authenticationLevel simple  , " +
                "itemOrUserFirst itemFirst  :{ protectedItems  { attributeType { 1.2.3    , ou }, entry , " +
                " rangeOfValues (cn=ErsinEr) , attributeValue { ou=people  , cn=Ersin  }," +
                "classes and : { item: xyz , or:{item:X,item:Y}   }}  , " +
                "itemPermissions { { userClasses {allUsers  , userGroup { \"1.2=y,z=t\"  , \"a=b,c=d\" } " +
                " , subtree { { base \"ou=people\" } } }   , grantsAndDenials  {  denyCompare  , grantModify } }," +
                "{ precedence 10, userClasses {allUsers  , userGroup { \"1.2=y,z=t\"  , \"a=b,c=d\" } " +
                " , subtree { { base \"ou=people\" } } }   , grantsAndDenials  {  denyCompare  , grantModify } } } }}";
        
        parser.parse(spec);
    }
    
    public void testOrderOfUserClassesDoesNotMatter() throws Exception
    {
        String spec = "{ identificationTag \"id2\"   , precedence 14, authenticationLevel none  , " +
                "itemOrUserFirst userFirst:  { userClasses {  name { \"ou=people,cn=ersin\" }, allUsers, " +
                "subtree {{ base \"ou=system\" }, { base \"ou=ORGANIZATIONUNIT\"," +
                "minimum  1, maximum   2 } } }  , " +
                "userPermissions { { protectedItems{ entry  , attributeType { cn  , ou }  , attributeValue {x=y,m=n,k=l} , " +
                "rangeOfValues (cn=ErsinEr) }  , grantsAndDenials { grantBrowse } } } }  }   ";
        
        parser.parse(spec);
    }
    
    public void testOrderOfProtectedItemsDoesNotMatterButDuplicatesMatter() throws Exception
    {
        String spec = " {  identificationTag  \"id1\" , precedence 114  , authenticationLevel simple  , " +
                "itemOrUserFirst itemFirst  :{ protectedItems  { attributeType { 1.2.3    , ou }, entry, entry , " +
                " rangeOfValues (cn=ErsinEr) , attributeValue { ou=people  , cn=Ersin  }," +
                "classes and : { item: xyz , or:{item:X,item:Y}   }}  , " +
                "itemPermissions { { userClasses {allUsers  , userGroup { \"1.2=y,z=t\"  , \"a=b,c=d\" } " +
                " , subtree { { base \"ou=people\" } } }   , grantsAndDenials  {  denyCompare  , grantModify } }," +
                "{ precedence 10, userClasses {allUsers  , userGroup { \"1.2=y,z=t\"  , \"a=b,c=d\" } " +
                " , subtree { { base \"ou=people\" } } }   , grantsAndDenials  {  denyCompare  , grantModify } } } }}";

        try
        {
            parser.parse(spec);
            fail( "testItemFirstOrderOfProtectedItemsDoesNotMatterButDuplicatesMatter() should not have run this line." );
        }
        catch ( ParseException e )
        {
            assertNotNull( e );
        }
    }
    
    public void testOrderOfUserClassesDoesNotMatterButDuplicatesMatter() throws Exception
    {
        String spec = "{ identificationTag \"id2\"   , precedence 14, authenticationLevel none  , " +
                "itemOrUserFirst userFirst:  { userClasses {  name { \"ou=people,cn=ersin\" }, allUsers, allUsers, " +
                "subtree {{ base \"ou=system\" }, { base \"ou=ORGANIZATIONUNIT\"," +
                "minimum  1, maximum   2 } } }  , " +
                "userPermissions { { protectedItems{ entry  , attributeType { cn  , ou }  , attributeValue {x=y,m=n,k=l} , " +
                "rangeOfValues (cn=ErsinEr) }  , grantsAndDenials { grantBrowse } } } }  }   ";
        
        try
        {
            parser.parse(spec);
            fail( "testUserFirstOrderOfUserClassesDoesNotMatterButDuplicatesMatter() should not have run this line." );
        }
        catch ( ParseException e )
        {
            assertNotNull( e );
        }
    }
        
    public void testItemPermissionComponentsOrderDoesNotMatter() throws Exception
    {
        String spec = " {  identificationTag  \"id1\" , precedence 114  , authenticationLevel simple  , " +
                "itemOrUserFirst itemFirst  :{ protectedItems  { attributeType { 1.2.3    , ou }, entry , " +
                " rangeOfValues (cn=ErsinEr) , attributeValue { ou=people  , cn=Ersin  }," +
                "classes and : { item: xyz , or:{item:X,item:Y}   }}  , " +
                "itemPermissions { { grantsAndDenials  {  denyCompare  , grantModify }, userClasses {allUsers  , userGroup { \"1.2=y,z=t\"  , \"a=b,c=d\" } " +
                " , subtree { { base \"ou=people\" } } }   }," +
                "{ precedence 10, userClasses {allUsers  , userGroup { \"1.2=y,z=t\"  , \"a=b,c=d\" } " +
                " , subtree { { base \"ou=people\" } } }   , grantsAndDenials  {  denyCompare  , grantModify } } } }}";

        parser.parse( spec );
    }
    
    public void testItemPermissionComponentsOrderDoesNotMatterButDuplicatesMatter() throws Exception
    {
        String spec = " {  identificationTag  \"id1\" , precedence 114  , authenticationLevel simple  , " +
                "itemOrUserFirst itemFirst  :{ protectedItems  { attributeType { 1.2.3    , ou }, entry , " +
                " rangeOfValues (cn=ErsinEr) , attributeValue { ou=people  , cn=Ersin  }," +
                "classes and : { item: xyz , or:{item:X,item:Y}   }}  , " +
                "itemPermissions { { userClasses {allUsers  , userGroup { \"1.2=y,z=t\"  , \"a=b,c=d\" } " +
                " , subtree { { base \"ou=people\" } } }, grantsAndDenials  {  denyCompare  , grantModify }, userClasses {allUsers  , userGroup { \"1.2=y,z=t\"  , \"a=b,c=d\" } " +
                " , subtree { { base \"ou=people\" } } }   }," +
                "{ precedence 10, userClasses {allUsers  , userGroup { \"1.2=y,z=t\"  , \"a=b,c=d\" } " +
                " , subtree { { base \"ou=people\" } } }   , grantsAndDenials  {  denyCompare  , grantModify } } } }}";

        try
        {
            parser.parse(spec);
            fail( "testItemPermissionComponentsOrderDoesNotMatterButDuplicatesMatter() should not have run this line." );
        }
        catch ( ParseException e )
        {
            assertNotNull( e );
        }
    }
    
    public void testUserPermissionComponentsOrderDoesNotMatter() throws Exception
    {
        String spec = "{ identificationTag \"id2\"   , precedence 14, authenticationLevel none  , " +
                "itemOrUserFirst userFirst:  { userClasses {  allUsers  , name { \"ou=people,cn=ersin\" }, " +
                "subtree {{ base \"ou=system\" }, { base \"ou=ORGANIZATIONUNIT\"," +
                "minimum  1, maximum   2 } } }  , " +
                "userPermissions { { grantsAndDenials { grantBrowse }, protectedItems{ entry  , attributeType { cn  , ou }  , attributeValue {x=y,m=n,k=l} , " +
                "rangeOfValues (cn=ErsinEr) }  } } }  }   ";
        
        parser.parse( spec );
    }
    
    public void testUserPermissionComponentsOrderDoesNotMatterButDuplicatesMatter() throws Exception
    {
        String spec = "{ identificationTag \"id2\"   , precedence 14, authenticationLevel none  , " +
                "itemOrUserFirst userFirst:  { userClasses {  allUsers  , name { \"ou=people,cn=ersin\" }, " +
                "subtree {{ base \"ou=system\" }, { base \"ou=ORGANIZATIONUNIT\"," +
                "minimum  1, maximum   2 } } }  , " +
                "userPermissions { { grantsAndDenials { grantBrowse }, grantsAndDenials { grantBrowse }, protectedItems{ entry  , attributeType { cn  , ou }  , attributeValue {x=y,m=n,k=l} , " +
                "rangeOfValues (cn=ErsinEr) }  } } }  }   ";
        
        try
        {
            parser.parse(spec);
            fail( "testUserPermissionComponentsOrderDoesNotMatterButDuplicatesMatter() should not have run this line." );
        }
        catch ( ParseException e )
        {
            assertNotNull( e );
        }
    }
    
    public void testOrderOfMainACIComponentsDoesNotMatter() throws Exception
    {
        String spec = "{   itemOrUserFirst userFirst:  { userClasses {  allUsers  , name { \"ou=people,cn=ersin\" }, " +
                "subtree {{ base \"ou=system\" }, { base \"ou=ORGANIZATIONUNIT\"," +
                "minimum  1, maximum   2 } } }  , " +
                "userPermissions { { protectedItems{ entry  , attributeType { cn  , ou }  , attributeValue {x=y,m=n,k=l} , " +
                "rangeOfValues (cn=ErsinEr) }  , grantsAndDenials { grantBrowse } } } }, " +
                " identificationTag \"id2\"   , authenticationLevel none, precedence 14 }   ";

        parser.parse( spec );
    }
    
    public void testOrderOfMainACIComponentsDoesNotMatterButDuplicatesMatter() throws Exception
    {
        String spec = "{   itemOrUserFirst userFirst:  { userClasses {  allUsers  , name { \"ou=people,cn=ersin\" }, " +
                "subtree {{ base \"ou=system\" }, { base \"ou=ORGANIZATIONUNIT\"," +
                "minimum  1, maximum   2 } } }  , " +
                "userPermissions { { protectedItems{ entry  , attributeType { cn  , ou }  , attributeValue {x=y,m=n,k=l} , " +
                "rangeOfValues (cn=ErsinEr) }  , grantsAndDenials { grantBrowse } } } }, " +
                " identificationTag \"id2\"   , authenticationLevel none, authenticationLevel simple, precedence 14 }   ";

        try
        {
            parser.parse(spec);
            fail( "testOrderOfMainACIComponentsDoesNotMatterButDuplicatesMatter() should not have run this line." );
        }
        catch ( ParseException e )
        {
            assertNotNull( e );
        }
    }
    
    public void testOrderOfMainACIComponentsDoesNotMatterButMissingsMatter() throws Exception
    {
        String spec = "{   itemOrUserFirst userFirst:  { userClasses {  allUsers  , name { \"ou=people,cn=ersin\" }, " +
                "subtree {{ base \"ou=system\" }, { base \"ou=ORGANIZATIONUNIT\"," +
                "minimum  1, maximum   2 } } }  , " +
                "userPermissions { { protectedItems{ entry  , attributeType { cn  , ou }  , attributeValue {x=y,m=n,k=l} , " +
                "rangeOfValues (cn=ErsinEr) }  , grantsAndDenials { grantBrowse } } } }, " +
                " identificationTag \"id2\"   , precedence 14 }   ";

        try
        {
            parser.parse(spec);
            fail( "testOrderOfMainACIComponentsDoesNotMatterButMissingsMatter() should not have run this line." );
        }
        catch ( ParseException e )
        {
            assertNotNull( e );
        }
    }
     
    public void testUserFirstComponentsOrderDoesNotMatter() throws Exception
    {
        String spec = "{ identificationTag \"id2\"   , precedence 14, authenticationLevel none  , " +
                "itemOrUserFirst userFirst:  { userPermissions { { protectedItems{ entry  , attributeType { cn  , ou }  , attributeValue {x=y,m=n,k=l} , " +
                "rangeOfValues (cn=ErsinEr) }  , grantsAndDenials { grantBrowse } } }, userClasses {  allUsers  , name { \"ou=people,cn=ersin\" }, " +
                "subtree {{ base \"ou=system\" }, { base \"ou=ORGANIZATIONUNIT\"," +
                "minimum  1, maximum   2 } } } }  }   ";
        
        parser.parse( spec );
    }
    
    public void testItemFirstComponentsOrderDoesNotMatter() throws Exception
    {
        String spec = " {  identificationTag  \"id1\" , precedence 114  , authenticationLevel simple  , " +
                "itemOrUserFirst itemFirst  :{ itemPermissions { { userClasses {allUsers  , userGroup { \"1.2=y,z=t\"  , \"a=b,c=d\" } " +
                " , subtree { { base \"ou=people\" } } }   , grantsAndDenials  {  denyCompare  , grantModify } }," +
                "{ precedence 10, userClasses {allUsers  , userGroup { \"1.2=y,z=t\"  , \"a=b,c=d\" } " +
                " , subtree { { base \"ou=people\" } } }   , grantsAndDenials  {  denyCompare  , grantModify } } },protectedItems  { entry  , attributeType { 1.2.3    , ou }  , " +
                " attributeValue { ou=people  , cn=Ersin  }  , rangeOfValues (cn=ErsinEr) , " +
                "classes and : { item: xyz , or:{item:X,item:Y}   }}  " +
                " }}";
        
        parser.parse( spec );
    }
    
    public void testGrantAndDenialBitsOrderDoesNotMatterButDuplicatesMatter() throws Exception
    {
        String spec = "{ identificationTag \"id2\"   , precedence 14, authenticationLevel none  , " +
                "itemOrUserFirst userFirst:  { userClasses {  allUsers }  , " +
                "userPermissions { { protectedItems{ entry  }  , grantsAndDenials { grantBrowse, grantInvoke, denyAdd, grantBrowse } } } }  }";
        
        try
        {
            parser.parse(spec);
            fail( "testGrantAndDenialBitsOrderDoesNotMatterButDuplicatesMatter() should not have run this line." );
        }
        catch ( ParseException e )
        {
            assertNotNull( e );
        }
    }
    
    public void testRestrictedValueComponentsOrderDoesNotMatter() throws Exception
    {
        String spec = "{ identificationTag \"id2\"   , precedence 14, authenticationLevel none  , " +
                "itemOrUserFirst userFirst:  { userClasses {  allUsers  , name { \"ou=people,cn=ersin\" }, " +
                "subtree {{ base \"ou=system\"}, { base \"ou=ORGANIZATIONUNIT\"," +
                "minimum  1, maximum   2 } } }  , " +
                "userPermissions { { protectedItems{ entry  , " + 
                "maxValueCount { { type 10.11.12, maxCount 10 }, { maxCount 20, type 11.12.13  } } " +
                " }  , grantsAndDenials { grantBrowse } } } }  }   ";
        
        parser.parse( spec );
    }
    
    public void testMaxValueCountComponentsOrderDoesNotMatter() throws Exception
    {
        String spec = "{ identificationTag \"id2\"   , precedence 14, authenticationLevel none  , " +
                "itemOrUserFirst userFirst:  { userClasses {  allUsers  , name { \"ou=people,cn=ersin\" }, " +
                "subtree {{ base \"ou=system\" }, { base \"ou=ORGANIZATIONUNIT\"," +
                "minimum  1, maximum   2 } } }  , " +
                "userPermissions { { protectedItems{ entry  , " + 
                "restrictedBy { { type 10.11.12, valuesIn ou }, { valuesIn cn, type 11.12.13  } } " +
                " }  , grantsAndDenials { grantBrowse } } } }  }   ";
        
        parser.parse( spec );
    }
    
    public void testSubtreeSpecificationComponentsOrderDoesNotMatter() throws Exception
    {
        String spec = "{ identificationTag \"id2\"   , precedence 14, authenticationLevel none  , " +
                "itemOrUserFirst userFirst:  { userPermissions { { protectedItems{ entry  , attributeType { cn  , ou }  , attributeValue {x=y,m=n,k=l} , " +
                "rangeOfValues (cn=ErsinEr) }  , grantsAndDenials { grantBrowse } } }, userClasses {  allUsers  , name { \"ou=people,cn=ersin\" }, " +
                "subtree {{ minimum 7, maximum 9, base \"ou=system\" }, { base \"ou=ORGANIZATIONUNIT\"," +
                " maximum   2, minimum  1 } } }  }  }   ";
        
        parser.parse( spec );
    }
}
