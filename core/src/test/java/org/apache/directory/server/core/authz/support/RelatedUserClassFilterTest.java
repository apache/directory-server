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


import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import javax.naming.NamingException;

import junit.framework.Assert;
import junit.framework.TestCase;

import org.apache.directory.server.core.authz.support.OperationScope;
import org.apache.directory.server.core.authz.support.RelatedUserClassFilter;
import org.apache.directory.server.core.subtree.SubtreeEvaluator;
import org.apache.directory.shared.ldap.aci.ACITuple;
import org.apache.directory.shared.ldap.aci.AuthenticationLevel;
import org.apache.directory.shared.ldap.aci.UserClass;
import org.apache.directory.shared.ldap.name.LdapDN;


/**
 * Tests {@link RelatedUserClassFilter}.
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 * @version $Rev$, $Date$
 */
public class RelatedUserClassFilterTest extends TestCase
{
    private static final Collection EMPTY_COLLECTION = Collections.unmodifiableCollection( new ArrayList() );
    private static final Set EMPTY_SET = Collections.unmodifiableSet( new HashSet() );

    private static final LdapDN GROUP_NAME;
    private static final LdapDN USER_NAME;
    private static final Set USER_NAMES = new HashSet();
    private static final Set GROUP_NAMES = new HashSet();

    private static final SubtreeEvaluator SUBTREE_EVALUATOR;

    private static final RelatedUserClassFilter filter;

    static
    {
        try
        {
            SUBTREE_EVALUATOR = new SubtreeEvaluator( new DummyOidRegistry(), new DummyAttributeTypeRegistry(true) );
            filter = new RelatedUserClassFilter( SUBTREE_EVALUATOR );
        }
        catch ( NamingException e )
        {
            throw new Error();
        }
        
        try
        {
            GROUP_NAME = new LdapDN( "ou=test,ou=groups,ou=system" );
            USER_NAME = new LdapDN( "ou=test, ou=users, ou=system" );
        }
        catch ( NamingException e )
        {
            throw new Error();
        }

        USER_NAMES.add( USER_NAME );
        GROUP_NAMES.add( GROUP_NAME );
    }


    public void testZeroTuple() throws Exception
    {
        Assert.assertEquals( 0, filter.filter( EMPTY_COLLECTION, OperationScope.ATTRIBUTE_TYPE_AND_VALUE, null, null,
            null, null, null, null, null, null, null, null ).size() );
    }


    public void testAllUsers() throws Exception
    {
        Collection tuples = getTuples( UserClass.ALL_USERS );

        Assert.assertEquals( 1, filter.filter( tuples, OperationScope.ENTRY, null, null, null, null,
            AuthenticationLevel.NONE, null, null, null, null, null ).size() );
    }


    public void testThisEntry() throws Exception
    {
        Collection tuples = getTuples( UserClass.THIS_ENTRY );

        Assert.assertEquals( 1, filter.filter( tuples, OperationScope.ENTRY, null, null, USER_NAME, null,
            AuthenticationLevel.NONE, USER_NAME, null, null, null, null ).size() );
        Assert.assertEquals( 0, filter.filter( tuples, OperationScope.ENTRY, null, null, USER_NAME, null,
            AuthenticationLevel.NONE, new LdapDN( "ou=unrelated" ), null, null, null, null ).size() );
    }


    public void testName() throws Exception
    {
        Collection tuples = getTuples( new UserClass.Name( USER_NAMES ) );
        Assert.assertEquals( 1, filter.filter( tuples, OperationScope.ENTRY, null, null, USER_NAME, null,
            AuthenticationLevel.NONE, null, null, null, null, null ).size() );
        Assert.assertEquals( 0, filter.filter( tuples, OperationScope.ENTRY, null, null,
            new LdapDN( "ou=unrelateduser, ou=users" ), null, AuthenticationLevel.NONE, USER_NAME, null, null, null,
            null ).size() );
    }


    public void testUserGroup() throws Exception
    {
        Collection tuples = getTuples( new UserClass.UserGroup( GROUP_NAMES ) );
        Assert.assertEquals( 1, filter.filter( tuples, OperationScope.ENTRY, null, GROUP_NAMES, USER_NAME, null,
            AuthenticationLevel.NONE, null, null, null, null, null ).size() );

        Set wrongGroupNames = new HashSet();
        wrongGroupNames.add( new LdapDN( "ou=unrelatedgroup" ) );

        Assert.assertEquals( 0, filter.filter( tuples, OperationScope.ENTRY, null, wrongGroupNames, USER_NAME, null,
            AuthenticationLevel.NONE, USER_NAME, null, null, null, null ).size() );
    }


    public void testSubtree() throws Exception
    {
        // TODO Don't know how to test yet.
    }


    public void testAuthenticationLevel() throws Exception
    {
        Collection tuples = getTuples( AuthenticationLevel.SIMPLE, true );

        Assert.assertEquals( 1, filter.filter( tuples, OperationScope.ENTRY, null, null, null, null,
            AuthenticationLevel.STRONG, null, null, null, null, null ).size() );
        Assert.assertEquals( 1, filter.filter( tuples, OperationScope.ENTRY, null, null, null, null,
            AuthenticationLevel.SIMPLE, null, null, null, null, null ).size() );
        Assert.assertEquals( 0, filter.filter( tuples, OperationScope.ENTRY, null, null, null, null,
            AuthenticationLevel.NONE, null, null, null, null, null ).size() );

        tuples = getTuples( AuthenticationLevel.SIMPLE, false );

        Assert.assertEquals( 1, filter.filter( tuples, OperationScope.ENTRY, null, null, null, null,
            AuthenticationLevel.NONE, null, null, null, null, null ).size() );

        Assert.assertEquals( 0, filter.filter( tuples, OperationScope.ENTRY, null, null, null, null,
            AuthenticationLevel.STRONG, null, null, null, null, null ).size() );

        tuples = getTuples( AuthenticationLevel.SIMPLE, false );

        Assert.assertEquals( 0, filter.filter( tuples, OperationScope.ENTRY, null, null, null, null,
            AuthenticationLevel.SIMPLE, null, null, null, null, null ).size() );
    }


    private static Collection getTuples( UserClass userClass )
    {
        Collection classes = new ArrayList();
        classes.add( userClass );

        Collection tuples = new ArrayList();
        tuples.add( new ACITuple( classes, AuthenticationLevel.NONE, EMPTY_COLLECTION, EMPTY_SET, true, 0 ) );

        return tuples;
    }


    private static Collection getTuples( AuthenticationLevel level, boolean grant )
    {
        Collection classes = new ArrayList();
        if ( grant )
        {
            classes.add( UserClass.ALL_USERS );
        }
        else
        {
            Set names = new HashSet();
            try
            {
                names.add( new LdapDN( "dummy=dummy" ) );
            }
            catch ( NamingException e )
            {
                throw new Error();
            }

            classes.add( new UserClass.Name( names ) );
        }

        Collection tuples = new ArrayList();
        tuples.add( new ACITuple( classes, level, EMPTY_COLLECTION, EMPTY_SET, grant, 0 ) );

        return tuples;
    }
}
