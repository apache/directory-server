/*
 *   @(#) $Id$
 *
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
package org.apache.ldap.server.authz.support;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import junit.framework.Assert;
import junit.framework.TestCase;

import org.apache.ldap.common.aci.ACITuple;
import org.apache.ldap.common.aci.AuthenticationLevel;
import org.apache.ldap.common.aci.UserClass;

/**
 * Tests {@link MostSpecificUserClassFilter}.
 *
 * @author The Apache Directory Project
 * @version $Rev$, $Date$
 */
public class MostSpecificUserClassFilterTest extends TestCase
{
    private static final Collection EMPTY_COLLECTION =
        Collections.unmodifiableCollection( new ArrayList() );
    private static final Set EMPTY_SET =
        Collections.unmodifiableSet( new HashSet() );
    
    private static final List TUPLES_A = new ArrayList();
    private static final List TUPLES_B = new ArrayList();
    private static final List TUPLES_C = new ArrayList();
    private static final List TUPLES_D = new ArrayList();
    private static final List TUPLES_E = new ArrayList();
    
    static
    {
        Collection name = new ArrayList();
        Collection thisEntry = new ArrayList();
        Collection userGroup = new ArrayList();
        Collection subtree = new ArrayList();
        Collection allUsers = new ArrayList();
        
        name.add( new UserClass.Name( EMPTY_SET ) );
        thisEntry.add( UserClass.THIS_ENTRY );
        userGroup.add( new UserClass.UserGroup( EMPTY_SET ) );
        subtree.add( new UserClass.Subtree( EMPTY_COLLECTION ) );
        allUsers.add( UserClass.ALL_USERS );

        ACITuple nameTuple = new ACITuple(
                name, AuthenticationLevel.NONE, EMPTY_COLLECTION,
                EMPTY_SET, true, 0 );
        ACITuple thisEntryTuple = new ACITuple(
                thisEntry, AuthenticationLevel.NONE, EMPTY_COLLECTION,
                EMPTY_SET, true, 0 );
        ACITuple userGroupTuple = new ACITuple(
                userGroup, AuthenticationLevel.NONE, EMPTY_COLLECTION,
                EMPTY_SET, true, 0 );
        ACITuple subtreeTuple = new ACITuple(
                subtree, AuthenticationLevel.NONE, EMPTY_COLLECTION,
                EMPTY_SET, true, 0 );
        ACITuple allUsersTuple = new ACITuple(
                allUsers, AuthenticationLevel.NONE, EMPTY_COLLECTION,
                EMPTY_SET, true, 0 );

        TUPLES_A.add( nameTuple );
        TUPLES_A.add( thisEntryTuple );
        TUPLES_A.add( userGroupTuple );
        TUPLES_A.add( subtreeTuple );
        TUPLES_A.add( allUsersTuple );

        TUPLES_B.add( thisEntryTuple );
        TUPLES_B.add( userGroupTuple );
        TUPLES_B.add( subtreeTuple );
        TUPLES_B.add( allUsersTuple );

        TUPLES_C.add( userGroupTuple );
        TUPLES_C.add( subtreeTuple );
        TUPLES_C.add( allUsersTuple );

        TUPLES_D.add( subtreeTuple );
        TUPLES_D.add( allUsersTuple );

        TUPLES_E.add( allUsersTuple );
        TUPLES_E.add( allUsersTuple );
    }
    
    public void testZeroOrOneTuple() throws Exception
    {
        MostSpecificUserClassFilter filter = new MostSpecificUserClassFilter();

        Assert.assertEquals(
                0, filter.filter(
                        EMPTY_COLLECTION, OperationScope.ATTRIBUTE_TYPE_AND_VALUE,
                        null, null, null, null, null, null, null, null, null, null ).size() );

        Collection tuples = new ArrayList();
        tuples.add( new ACITuple(
                EMPTY_COLLECTION, AuthenticationLevel.NONE, EMPTY_COLLECTION,
                EMPTY_SET, false, 0 ) );
        
        Assert.assertEquals(
                1, filter.filter(
                        tuples, OperationScope.ATTRIBUTE_TYPE_AND_VALUE,
                        null, null, null, null, null, null, null, null, null, null ).size() );
    }
    
    public void testNameAndThisEntry() throws Exception
    {
        MostSpecificUserClassFilter filter = new MostSpecificUserClassFilter();
        
        List tuples = new ArrayList( TUPLES_A );
        tuples = ( List ) filter.filter(
                tuples, OperationScope.ENTRY, null, null, null,
                null, null, null, null, null, null, null );
        
        Assert.assertEquals( 2, tuples.size() );
        Assert.assertSame( TUPLES_A.get( 0 ), tuples.get( 0 ) );
        Assert.assertSame( TUPLES_A.get( 1 ), tuples.get( 1 ) );
    }

    public void testThisEntry() throws Exception
    {
        MostSpecificUserClassFilter filter = new MostSpecificUserClassFilter();
        
        List tuples = new ArrayList( TUPLES_B );
        tuples = ( List ) filter.filter(
                tuples, OperationScope.ENTRY, null, null, null,
                null, null, null, null, null, null, null );
        
        Assert.assertEquals( 1, tuples.size() );
        Assert.assertSame( TUPLES_B.get( 0 ), tuples.get( 0 ) );
    }

    public void testUserGroup() throws Exception
    {
        MostSpecificUserClassFilter filter = new MostSpecificUserClassFilter();
        
        List tuples = new ArrayList( TUPLES_C );
        tuples = ( List ) filter.filter(
                tuples, OperationScope.ENTRY, null, null, null,
                null, null, null, null, null, null, null );
        
        Assert.assertEquals( 1, tuples.size() );
        Assert.assertSame( TUPLES_C.get( 0 ), tuples.get( 0 ) );
    }

    public void testSubtree() throws Exception
    {
        MostSpecificUserClassFilter filter = new MostSpecificUserClassFilter();
        
        List tuples = new ArrayList( TUPLES_D );
        tuples = ( List ) filter.filter(
                tuples, OperationScope.ENTRY, null, null, null,
                null, null, null, null, null, null, null );
        
        Assert.assertEquals( 1, tuples.size() );
        Assert.assertSame( TUPLES_D.get( 0 ), tuples.get( 0 ) );
    }
    
    public void testOthers() throws Exception
    {
        MostSpecificUserClassFilter filter = new MostSpecificUserClassFilter();
        
        List tuples = new ArrayList( TUPLES_E );
        tuples = ( List ) filter.filter(
                tuples, OperationScope.ENTRY, null, null, null,
                null, null, null, null, null, null, null );
        
        Assert.assertEquals( 2, tuples.size() );
        Assert.assertSame( TUPLES_E.get( 0 ), tuples.get( 0 ) );
        Assert.assertSame( TUPLES_E.get( 1 ), tuples.get( 1 ) );
    }
}
