/*
 *   Licensed to the Apache Software Foundation (ASF) under one
 *   or more contributor license agreements.  See the NOTICE file
 *   distributed with this work for additional information
 *   regarding copyright ownership.  The ASF licenses this file
 *   to you under the Apache License, Version 2.0 (the
 *   "License"); you may not use this file except in compliance
 *   with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 *   Unless required by applicable law or agreed to in writing,
 *   software distributed under the License is distributed on an
 *   "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 *   KIND, either express or implied.  See the License for the
 *   specific language governing permissions and limitations
 *   under the License.
 *
 */
package org.apache.directory.server.operations.search;


import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import java.util.ArrayList;
import java.util.List;

import org.apache.directory.api.ldap.model.constants.SchemaConstants;
import org.apache.directory.api.ldap.model.cursor.SearchCursor;
import org.apache.directory.api.ldap.model.entry.Entry;
import org.apache.directory.api.ldap.model.filter.ExprNode;
import org.apache.directory.api.ldap.model.filter.PresenceNode;
import org.apache.directory.api.ldap.model.message.ResultCodeEnum;
import org.apache.directory.api.ldap.model.message.SearchRequest;
import org.apache.directory.api.ldap.model.message.SearchRequestImpl;
import org.apache.directory.api.ldap.model.message.SearchResultDone;
import org.apache.directory.api.ldap.model.message.SearchResultEntry;
import org.apache.directory.api.ldap.model.message.SearchScope;
import org.apache.directory.api.ldap.model.message.controls.SortKey;
import org.apache.directory.api.ldap.model.message.controls.SortRequestControl;
import org.apache.directory.api.ldap.model.message.controls.SortRequestControlImpl;
import org.apache.directory.api.ldap.model.message.controls.SortResponseControl;
import org.apache.directory.api.ldap.model.message.controls.SortResultCode;
import org.apache.directory.api.ldap.model.name.Dn;
import org.apache.directory.ldap.client.api.LdapConnection;
import org.apache.directory.ldap.client.api.LdapNetworkConnection;
import org.apache.directory.server.annotations.CreateLdapServer;
import org.apache.directory.server.annotations.CreateTransport;
import org.apache.directory.server.core.annotations.ApplyLdifFiles;
import org.apache.directory.server.core.integ.AbstractLdapTestUnit;
import org.apache.directory.server.core.integ.FrameworkRunner;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;


/**
 * Tests for searching with server side sort control.
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 */
@RunWith(FrameworkRunner.class)
@CreateLdapServer(transports =
    { @CreateTransport(protocol = "LDAP") })
@ApplyLdifFiles(
    { "sortedsearch-test-data.ldif" })
public class SortedSearchIT extends AbstractLdapTestUnit
{
    private Dn baseDn;

    private ExprNode filter;

    private static LdapConnection con;

    private SearchRequest req;

    private SortKey sk;

    private SortRequestControl ctrl;


    @Before
    public void createConnection() throws Exception
    {
        if ( con == null )
        {
            con = new LdapNetworkConnection( "localhost", getLdapServer().getPort() );
            con.bind( "uid=admin,ou=system", "secret" );
            con.setTimeOut( Long.MAX_VALUE );
        }

        baseDn = new Dn( "ou=parent,ou=system" );
        filter = new PresenceNode( "objectClass" );

        req = new SearchRequestImpl();
        req.setBase( baseDn );
        req.setFilter( filter );
        req.setScope( SearchScope.SUBTREE );
        req.addAttributes( SchemaConstants.ALL_ATTRIBUTES_ARRAY );

        // tests may overwrite the fields of the below SortKey instance
        sk = new SortKey( "entryDn" );
        ctrl = new SortRequestControlImpl();
        ctrl.addSortKey( sk );
        req.addControl( ctrl );
    }


    @AfterClass
    public static void closeConnection() throws Exception
    {
        con.close();
    }


    /**
     * section #2 scenario #3
     * 
     * @throws Exception
     */
    @Test
    public void testWithInvalidAttributeAndCriticality() throws Exception
    {
        sk.setAttributeTypeDesc( "Non-existing-At" );
        ctrl.setCritical( true );

        SearchCursor cursor = con.search( req );
        assertFalse( cursor.next() );

        SearchResultDone sd = cursor.getSearchResultDone();

        cursor.close();

        SortResponseControl resp = ( SortResponseControl ) sd.getControl( SortResponseControl.OID );
        assertNotNull( resp );

        assertEquals( SortResultCode.NOSUCHATTRIBUTE, resp.getSortResult() );
        assertEquals( sk.getAttributeTypeDesc(), resp.getAttributeName() );
        assertEquals( ResultCodeEnum.UNAVAILABLE_CRITICAL_EXTENSION, sd.getLdapResult().getResultCode() );
    }


    /**
     * section #2 scenario #4
     *
     * @throws Exception
     */
    @Test
    public void testWithInvalidAttributeAndNoCriticality() throws Exception
    {
        sk.setAttributeTypeDesc( "Non-existing-At" );
        ctrl.setCritical( false );

        SearchCursor cursor = con.search( req );

        int count = 0;

        while ( cursor.next() )
        {
            cursor.get();
            count++;
        }

        cursor.close();

        assertEquals( 14, count );

        SearchResultDone sd = cursor.getSearchResultDone();

        SortResponseControl resp = ( SortResponseControl ) sd.getControl( SortResponseControl.OID );
        assertNotNull( resp );

        assertEquals( SortResultCode.NOSUCHATTRIBUTE, resp.getSortResult() );
        assertEquals( sk.getAttributeTypeDesc(), resp.getAttributeName() );
        assertEquals( ResultCodeEnum.SUCCESS, sd.getLdapResult().getResultCode() );
    }


    /**
     * section #2 scenario #6
     * 
     * @throws Exception
     */
    @Test
    public void testWithInvalidFilter() throws Exception
    {
        req.setFilter( new PresenceNode( "mail" ) );

        SearchCursor cursor = con.search( req );

        assertFalse( cursor.next() );

        cursor.close();

        SearchResultDone sd = cursor.getSearchResultDone();

        SortResponseControl resp = ( SortResponseControl ) sd.getControl( SortResponseControl.OID );
        assertNull( resp );

        assertEquals( ResultCodeEnum.SUCCESS, sd.getLdapResult().getResultCode() );
    }


    ///////////////////////////// Tests for section #2 scenario #5 \\\\\\\\\\\\\\\\\\\\\\\\\\\\\

    @Test
    public void testSortBySn() throws Exception
    {
        sk.setAttributeTypeDesc( "sn" );
        SearchCursor cursor = con.search( req );

        List<String> expectedOrder = new ArrayList<String>();
        expectedOrder.add( "uid=person1,ou=parent,ou=system" );
        expectedOrder.add( "uid=person2,ou=parent,ou=system" );
        expectedOrder.add( "uid=person3,ou=parent,ou=system" );
        expectedOrder.add( "uid=user0,ou=parent,ou=system" );
        expectedOrder.add( "uid=user1,ou=parent,ou=system" );
        expectedOrder.add( "uid=user2,ou=children,ou=parent,ou=system" );
        expectedOrder.add( "uid=user3,ou=children,ou=parent,ou=system" );
        expectedOrder.add( "uid=user4,ou=grandchildren,ou=children,ou=parent,ou=system" );
        expectedOrder.add( "uid=user5,ou=grandchildren,ou=children,ou=parent,ou=system" );
        expectedOrder.add( "uid=user6,ou=parent,ou=system" );
        expectedOrder.add( "uid=user7,ou=parent,ou=system" );

        int expectedCount = expectedOrder.size();

        List<String> actualOrder = new ArrayList<String>();

        while ( cursor.next() )
        {
            SearchResultEntry se = ( SearchResultEntry ) cursor.get();
            Entry entry = se.getEntry();
            actualOrder.add( entry.getDn().getName() );
        }

        cursor.close();

        // remove the LAST 3 entries present in the actualOrder list, they exist on top cause they don't have "sn" attribute
        // NOTE: there is no guaranteed order for these LAST 3 entries
        actualOrder.remove( actualOrder.size() - 1 );
        actualOrder.remove( actualOrder.size() - 1 );
        actualOrder.remove( actualOrder.size() - 1 );

        assertEquals( expectedCount, actualOrder.size() );

        for ( int i = 0; i < expectedOrder.size(); i++ )
        {
            assertEquals( expectedOrder.get( i ), actualOrder.get( i ) );
        }

        // check reverse order
        actualOrder.clear();

        sk.setReverseOrder( true );
        cursor = con.search( req );

        while ( cursor.next() )
        {
            SearchResultEntry se = ( SearchResultEntry ) cursor.get();
            Entry entry = se.getEntry();
            actualOrder.add( entry.getDn().getName() );
        }

        cursor.close();

        // remove the FIRST 3 entries present in the actualOrder list, they exist on top cause they don't have "sn" attribute
        // NOTE: there is no guaranteed order for these FIRST 3 entries
        actualOrder.remove( 0 );
        actualOrder.remove( 0 );
        actualOrder.remove( 0 );

        assertEquals( expectedCount, actualOrder.size() );

        expectedCount--;
        for ( int i = expectedOrder.size() - 1; i >= 0; i-- )
        {
            assertEquals( expectedOrder.get( i ), actualOrder.get( expectedCount - i ) );
        }
    }
    
    // though "sn" is also multi-valued, the test data has only one value for "sn" in each entry
    // so using "cn" for this test
    @Test
    public void testSortByMultiValuedAttribute() throws Exception
    {
        sk.setAttributeTypeDesc( "cn" );
        SearchCursor cursor = con.search( req );

        List<String> expectedOrder = new ArrayList<String>();
        expectedOrder.add( "uid=user6,ou=parent,ou=system" );
        expectedOrder.add( "uid=user0,ou=parent,ou=system" );
        expectedOrder.add( "uid=user1,ou=parent,ou=system" );
        expectedOrder.add( "uid=person3,ou=parent,ou=system" );
        expectedOrder.add( "uid=user2,ou=children,ou=parent,ou=system" );
        expectedOrder.add( "uid=user3,ou=children,ou=parent,ou=system" );
        expectedOrder.add( "uid=user4,ou=grandchildren,ou=children,ou=parent,ou=system" );
        expectedOrder.add( "uid=user5,ou=grandchildren,ou=children,ou=parent,ou=system" );
        expectedOrder.add( "uid=user7,ou=parent,ou=system" );
        expectedOrder.add( "uid=person1,ou=parent,ou=system" );
        expectedOrder.add( "uid=person2,ou=parent,ou=system" );

        int expectedCount = expectedOrder.size();

        List<String> actualOrder = new ArrayList<String>();

        while ( cursor.next() )
        {
            SearchResultEntry se = ( SearchResultEntry ) cursor.get();
            Entry entry = se.getEntry();
            actualOrder.add( entry.getDn().getName() );
        }

        cursor.close();

        // remove the LAST 3 entries present in the actualOrder list, they exist on top cause they don't have "sn" attribute
        // NOTE: there is no guaranteed order for these LAST 3 entries
        actualOrder.remove( actualOrder.size() - 1 );
        actualOrder.remove( actualOrder.size() - 1 );
        actualOrder.remove( actualOrder.size() - 1 );

        assertEquals( expectedCount, actualOrder.size() );

        for ( int i = 0; i < expectedOrder.size(); i++ )
        {
            assertEquals( expectedOrder.get( i ), actualOrder.get( i ) );
        }

        // check reverse order
        actualOrder.clear();

        sk.setReverseOrder( true );
        cursor = con.search( req );

        while ( cursor.next() )
        {
            SearchResultEntry se = ( SearchResultEntry ) cursor.get();
            Entry entry = se.getEntry();
            actualOrder.add( entry.getDn().getName() );
        }

        cursor.close();

        // remove the FIRST 3 entries present in the actualOrder list, they exist on top cause they don't have "sn" attribute
        // NOTE: there is no guaranteed order for these FIRST 3 entries
        actualOrder.remove( 0 );
        actualOrder.remove( 0 );
        actualOrder.remove( 0 );

        assertEquals( expectedCount, actualOrder.size() );

        expectedCount--;
        for ( int i = expectedOrder.size() - 1; i >= 0; i-- )
        {
            assertEquals( expectedOrder.get( i ), actualOrder.get( expectedCount - i ) );
        }
    }

    
    @Test
    public void testSortByDn() throws Exception
    {
        sk.setAttributeTypeDesc( "entryDn" );
        sk.setMatchingRuleId( "2.5.13.1" );
        SearchCursor cursor = con.search( req );

        List<Entry> actualOrder = new ArrayList<Entry>();

        while ( cursor.next() )
        {
            SearchResultEntry se = ( SearchResultEntry ) cursor.get();
            Entry entry = se.getEntry();
            actualOrder.add( entry );
        }

        cursor.close();
        
        // start deleting from the first entry
        // SHOULD succeeded if the order is as expected
        for( int i = 0; i < actualOrder.size(); i++ )
        {
            con.delete( actualOrder.get( i ).getDn() );
        }
        
        // now insert from the last entry, SHOULD succeed
        for( int i = actualOrder.size() - 1; i >= 0; i-- )
        {
            con.add( actualOrder.get( i ) );
        }
        
        actualOrder.clear();

        sk.setReverseOrder( true );
        cursor = con.search( req );
        
        while ( cursor.next() )
        {
            SearchResultEntry se = ( SearchResultEntry ) cursor.get();
            Entry entry = se.getEntry();
            actualOrder.add( entry );
        }

        // now delete again, this time from the end, SHOULD succeed
        for( int i = actualOrder.size() - 1; i >= 0; i-- )
        {
            con.delete( actualOrder.get( i ).getDn() );
        }
        
        // now insert again, but from the beginning, SHOULD succeed
        for( int i = 0; i < actualOrder.size(); i++ )
        {
            con.add( actualOrder.get( i ) );
        }
    }

}
