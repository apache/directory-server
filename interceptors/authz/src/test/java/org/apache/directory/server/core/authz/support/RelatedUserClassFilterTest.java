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
package org.apache.directory.server.core.authz.support;


import static org.junit.Assert.assertEquals;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import org.apache.directory.server.core.api.subtree.SubtreeEvaluator;
import org.apache.directory.shared.ldap.aci.ACITuple;
import org.apache.directory.shared.ldap.aci.MicroOperation;
import org.apache.directory.shared.ldap.aci.ProtectedItem;
import org.apache.directory.shared.ldap.aci.UserClass;
import org.apache.directory.shared.ldap.model.constants.AuthenticationLevel;
import org.apache.directory.shared.ldap.model.exception.LdapInvalidDnException;
import org.apache.directory.shared.ldap.model.name.Dn;
import org.apache.directory.shared.ldap.model.schema.SchemaManager;
import org.apache.directory.shared.ldap.schemamanager.impl.DefaultSchemaManager;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;

import com.mycila.junit.concurrent.Concurrency;
import com.mycila.junit.concurrent.ConcurrentJunitRunner;


/**
 * Tests {@link RelatedUserClassFilter}.
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 */
@RunWith(ConcurrentJunitRunner.class)
@Concurrency()
public class RelatedUserClassFilterTest
{
    private static final Collection<ACITuple> EMPTY_ACI_TUPLE_COLLECTION = Collections.unmodifiableCollection( new ArrayList<ACITuple>() );
    private static final Collection<ProtectedItem> EMPTY_PROTECTED_ITEM_COLLECTION = Collections.unmodifiableCollection( new ArrayList<ProtectedItem>() );

    private static final Set<MicroOperation> EMPTY_MICRO_OPERATION_SET = Collections.unmodifiableSet( new HashSet<MicroOperation>() );

    private static Dn GROUP_NAME;
    private static Dn USER_NAME;
    private static final Set<Dn> USER_NAMES = new HashSet<Dn>();
    private static final Set<Dn> GROUP_NAMES = new HashSet<Dn>();

    private static SubtreeEvaluator SUBTREE_EVALUATOR;

    private static RelatedUserClassFilter filter;
    private static SchemaManager schemaManager;

    @BeforeClass
    public static void init() throws Exception
    {
        schemaManager = new DefaultSchemaManager();
        SUBTREE_EVALUATOR = new SubtreeEvaluator( new DefaultSchemaManager( null ) );
        filter = new RelatedUserClassFilter( SUBTREE_EVALUATOR );
        
        try
        {
            GROUP_NAME = new Dn( schemaManager, "ou=test,ou=groups,ou=system" );
            USER_NAME = new Dn( schemaManager, "ou=test, ou=users, ou=system" );
        }
        catch ( LdapInvalidDnException e )
        {
            throw new Error();
        }

        USER_NAMES.add( USER_NAME );
        GROUP_NAMES.add( GROUP_NAME );
    }


    @Test
    public void testZeroTuple() throws Exception
    {
        AciContext aciContext = new AciContext( null, null );
        aciContext.setAciTuples( EMPTY_ACI_TUPLE_COLLECTION );

        assertEquals( 0, filter.filter( aciContext, OperationScope.ATTRIBUTE_TYPE_AND_VALUE, null ).size() );
    }


    @Test
    public void testAllUsers() throws Exception
    {
        Collection<ACITuple> tuples = getTuples( UserClass.ALL_USERS );
        AciContext aciContext = new AciContext( null, null );
        aciContext.setAciTuples( tuples );
        aciContext.setAuthenticationLevel( AuthenticationLevel.NONE );

        assertEquals( 1, filter.filter( aciContext, OperationScope.ENTRY, null ).size() );
    }


    @Test
    public void testThisEntry() throws Exception
    {
        Collection<ACITuple> tuples = getTuples( UserClass.THIS_ENTRY );

        AciContext aciContext = new AciContext( null, null );
        aciContext.setAciTuples( tuples );
        aciContext.setUserDn( USER_NAME );
        aciContext.setAuthenticationLevel( AuthenticationLevel.NONE );
        aciContext.setEntryDn( USER_NAME );

        assertEquals( 1, filter.filter( aciContext, OperationScope.ENTRY, null ).size() );

        aciContext = new AciContext( null, null );
        aciContext.setAciTuples( tuples );
        aciContext.setUserDn( USER_NAME );
        aciContext.setAuthenticationLevel( AuthenticationLevel.NONE );
        aciContext.setEntryDn( new Dn( schemaManager, "ou=unrelated" ) );

        assertEquals( 0, filter.filter( aciContext, OperationScope.ENTRY, null ).size() );
    }
    
    
    @Test
    public void testParentOfEntry() throws Exception
    {
        Collection<ACITuple> tuples = getTuples( UserClass.PARENT_OF_ENTRY );

        AciContext aciContext = new AciContext( null, null );
        aciContext.setAciTuples( tuples );
        aciContext.setUserDn( USER_NAME );
        aciContext.setAuthenticationLevel( AuthenticationLevel.NONE );
        aciContext.setEntryDn( new Dn( schemaManager, "ou=phoneBook, ou=test, ou=users, ou=system" ) );

        assertEquals( 1, filter.filter( aciContext, OperationScope.ENTRY, null ).size() );

        aciContext = new AciContext( null, null );
        aciContext.setAciTuples( tuples );
        aciContext.setUserDn( USER_NAME );
        aciContext.setAuthenticationLevel( AuthenticationLevel.NONE );
        aciContext.setEntryDn( new Dn( schemaManager, "ou=unrelated" ) );

        assertEquals( 0, filter.filter( aciContext, OperationScope.ENTRY, null ).size() );
    }


    @Test
    public void testName() throws Exception
    {
        Collection<ACITuple> tuples = getTuples( new UserClass.Name( USER_NAMES ) );

        AciContext aciContext = new AciContext( null, null );
        aciContext.setAciTuples( tuples );
        aciContext.setUserDn( USER_NAME );
        aciContext.setAuthenticationLevel( AuthenticationLevel.NONE );

        assertEquals( 1, filter.filter( aciContext, OperationScope.ENTRY, null ).size() );

        aciContext = new AciContext( null, null );
        aciContext.setAciTuples( tuples );
        aciContext.setUserDn( new Dn( schemaManager, "ou=unrelateduser, ou=users" ) );
        aciContext.setAuthenticationLevel( AuthenticationLevel.NONE );
        aciContext.setEntryDn( USER_NAME );

        assertEquals( 0, filter.filter( aciContext, OperationScope.ENTRY, null ).size() );
    }


    @Test
    public void testUserGroup() throws Exception
    {
        Collection<ACITuple> tuples = getTuples( new UserClass.UserGroup( GROUP_NAMES ) );

        AciContext aciContext = new AciContext( null, null );
        aciContext.setAciTuples( tuples );
        aciContext.setUserGroupNames( GROUP_NAMES );
        aciContext.setUserDn( USER_NAME );
        aciContext.setAuthenticationLevel( AuthenticationLevel.NONE );

        assertEquals( 1, filter.filter( aciContext, OperationScope.ENTRY, null ).size() );

        Set<Dn> wrongGroupNames = new HashSet<Dn>();
        wrongGroupNames.add( new Dn( schemaManager, "ou=unrelatedgroup" ) );

        aciContext = new AciContext( null, null );
        aciContext.setAciTuples( tuples );
        aciContext.setUserDn( USER_NAME );
        aciContext.setAuthenticationLevel( AuthenticationLevel.NONE );
        aciContext.setUserGroupNames( wrongGroupNames );
        aciContext.setEntryDn( USER_NAME );

        assertEquals( 0, filter.filter( aciContext, OperationScope.ENTRY, null ).size() );
    }


    @Test
    public void testSubtree() throws Exception
    {
        // TODO Don't know how to test yet.
    }


    @Test
    public void testAuthenticationLevel() throws Exception
    {
        Collection<ACITuple> tuples = getTuples( AuthenticationLevel.SIMPLE, true );

        AciContext aciContext = new AciContext( null, null );
        aciContext.setAciTuples( tuples );
        aciContext.setAuthenticationLevel( AuthenticationLevel.STRONG );

        assertEquals( 1, filter.filter( aciContext, OperationScope.ENTRY, null ).size() );

        aciContext = new AciContext( null, null );
        aciContext.setAciTuples( tuples );
        aciContext.setAuthenticationLevel( AuthenticationLevel.SIMPLE );

        assertEquals( 1, filter.filter( aciContext, OperationScope.ENTRY, null ).size() );

        aciContext = new AciContext( null, null );
        aciContext.setAciTuples( tuples );
        aciContext.setAuthenticationLevel( AuthenticationLevel.NONE );

        assertEquals( 0, filter.filter( aciContext, OperationScope.ENTRY, null ).size() );

        tuples = getTuples( AuthenticationLevel.SIMPLE, false );

        aciContext = new AciContext( null, null );
        aciContext.setAciTuples( tuples );
        aciContext.setAuthenticationLevel( AuthenticationLevel.NONE );

        assertEquals( 1, filter.filter( aciContext, OperationScope.ENTRY, null ).size() );

        aciContext = new AciContext( null, null );
        aciContext.setAciTuples( tuples );
        aciContext.setAuthenticationLevel( AuthenticationLevel.STRONG );

        assertEquals( 0, filter.filter( aciContext, OperationScope.ENTRY, null ).size() );

        tuples = getTuples( AuthenticationLevel.SIMPLE, false );

        aciContext = new AciContext( null, null );
        aciContext.setAciTuples( tuples );
        aciContext.setAuthenticationLevel( AuthenticationLevel.SIMPLE );

        assertEquals( 0, filter.filter( aciContext, OperationScope.ENTRY, null ).size() );
    }


    private static Collection<ACITuple> getTuples( UserClass userClass )
    {
        Collection<UserClass> classes = new ArrayList<UserClass>();
        classes.add( userClass );

        Collection<ACITuple> tuples = new ArrayList<ACITuple>();
        tuples.add( new ACITuple( classes, AuthenticationLevel.NONE, EMPTY_PROTECTED_ITEM_COLLECTION, 
            EMPTY_MICRO_OPERATION_SET, true, 0 ) );

        return tuples;
    }


    private static Collection<ACITuple> getTuples( AuthenticationLevel level, boolean grant )
    {
        Collection<UserClass> classes = new ArrayList<UserClass>();
        
        if ( grant )
        {
            classes.add( UserClass.ALL_USERS );
        }
        else
        {
            Set<Dn> names = new HashSet<Dn>();
            
            try
            {
                names.add( new Dn( schemaManager, "cn=dummy" ) );
            }
            catch ( LdapInvalidDnException e )
            {
                throw new Error();
            }

            classes.add( new UserClass.Name( names ) );
        }

        Collection<ACITuple> tuples = new ArrayList<ACITuple>();
        tuples.add( new ACITuple( classes, level, EMPTY_PROTECTED_ITEM_COLLECTION, EMPTY_MICRO_OPERATION_SET, grant, 0 ) );

        return tuples;
    }
}
