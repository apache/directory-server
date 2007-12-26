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
package org.apache.directory.server.core.prefs;


import org.apache.directory.server.core.DirectoryService;
import org.apache.directory.server.core.integ.CiRunner;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.assertNotNull;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.prefs.BackingStoreException;
import java.util.prefs.Preferences;


/**
 * Tests the ServerSystemPreferences class.
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 * @version $Rev$
 */
@RunWith ( CiRunner.class )
public class PreferencesIT
{
    public static DirectoryService service;


    @Test
    public void testSystemRoot()
    {
        ServerPreferencesFactory factory = new ServerPreferencesFactory( service );
        Preferences prefs = factory.systemRoot();

        assertNotNull( prefs );
        assertEquals( "sysPrefRoot", prefs.get( "prefNodeName", "default value" ) );
    }


    /**
     * Tests to make sure the system preferences root has entry (test, abc123).
     *
     * @throws Exception if there are failures with the store
     */
    @Test
    public void testRoot() throws Exception
    {
        ServerSystemPreferences prefs = new ServerSystemPreferences( service );
        assertEquals( "sysPrefRoot", prefs.get( "prefNodeName", "not the value" ) );
    }


    /**
     * Tests the creation and use of a new preferences node.
     *
     * @throws BackingStoreException if there are failures with the store
     */
    @Test
    public void testCreate() throws BackingStoreException
    {
        ServerSystemPreferences prefs = new ServerSystemPreferences( service );
        Preferences testNode = prefs.node( "testNode" );

        testNode.put( "cn", "testNodeValue" );
        testNode.sync();
    }


    /**
     * Tests the creation and use of a new preferences node.
     *
     * @throws BackingStoreException if there are failures with the store
     */
    @Test
    public void testCreateAndSetBoolean() throws BackingStoreException
    {
        ServerSystemPreferences prefs = new ServerSystemPreferences( service );
        Preferences testNode = prefs.node( "testNode" );
        testNode.putBoolean( "cn", false );
        testNode.sync();
        testNode = prefs.node( "testNode" );
        assertEquals( false, testNode.getBoolean( "cn", false ) );
    }


    /**
     * Tests the creation and use of a new preferences node.
     *
     * @throws BackingStoreException if there are failures with the store
     */
    /* TODO: Temporarily commented until we get a clear status about this package
    public void testCreateAndSetByteArray() throws BackingStoreException
    {
        byte[] jpegValue = new byte[]
                                   { (byte)0xFF, (byte)0xD8, (byte)0xFF, (byte)0xE0, 0x01, 0x02, 'J', 'F', 'I', 'F', 0x00, 0x45, 0x23, 0x7d, 0x7f };
        Preferences testNode = prefs.node( "testNode" );
        testNode.putByteArray( "jpegPhoto", jpegValue );
        testNode.sync();
        testNode = prefs.node( "testNode" );
    }
    */


    /**
     * Tests the creation and use of a new preferences node.
     *
     * @throws BackingStoreException if there are failures with the store
     */
    @Test
    public void testCreateAndSetDouble() throws BackingStoreException
    {
        ServerSystemPreferences prefs = new ServerSystemPreferences( service );
        Preferences testNode = prefs.node( "testNode" );
        testNode.putDouble( "cn", 3.14 );
        testNode.sync();
        testNode = prefs.node( "testNode" );
        assertTrue( 3.14 == testNode.getDouble( "cn", 9.20 ) );
    }


    /**
     * Tests the creation and use of a new preferences node.
     *
     * @throws BackingStoreException if there are failures with the store
     */
    @Test
    public void testCreateAndSetFloat() throws BackingStoreException
    {
        ServerSystemPreferences prefs = new ServerSystemPreferences( service );
        Preferences testNode = prefs.node( "testNode" );
        testNode.putFloat( "cn", ( float ) 9.20 );
        testNode.sync();
        testNode = prefs.node( "testNode" );
        assertTrue( ( float ) 9.20 == testNode.getFloat( "cn", ( float ) 9.20 ) );
    }


    /**
     * Tests the creation and use of a new preferences node.
     *
     * @throws BackingStoreException if there are failures with the store
     */
    @Test
    public void testCreateAndSetInt() throws BackingStoreException
    {
        ServerSystemPreferences prefs = new ServerSystemPreferences( service );
        Preferences testNode = prefs.node( "testNode" );
        testNode.putInt( "cn", 345 );
        testNode.sync();
        testNode = prefs.node( "testNode" );
        assertTrue( 345 == testNode.getInt( "cn", 345 ) );
    }


    /**
     * Tests the creation and use of a new preferences node.
     *
     * @throws BackingStoreException if there are failures with the store
     */
    @Test
    public void testCreateAndSetLong() throws BackingStoreException
    {
        ServerSystemPreferences prefs = new ServerSystemPreferences( service );
        Preferences testNode = prefs.node( "testNode" );
        testNode.putLong( "cn", 75449559185447L );
        testNode.sync();
        testNode = prefs.node( "testNode" );
        assertTrue( 75449559185447L == testNode.getLong( "cn", 75449559185447L ) );
    }


    /**
     * Tests the creation and use of a new preferences node.
     *
     * @throws BackingStoreException if there are failures with the store
     */
    @Test
    public void testCreateAndRemove() throws BackingStoreException
    {
        ServerSystemPreferences prefs = new ServerSystemPreferences( service );
        Preferences testNode = prefs.node( "testNode" );

        testNode.put( "cn", "testNodeValue" );
        testNode.putInt( "roomNumber", 345 );
        testNode.sync();

        testNode = prefs.node( "testNode" );
        assertEquals( 345, testNode.getInt( "roomNumber", 87 ) );
        testNode.remove( "cn" );
        testNode.remove( "roomNumber" );
        testNode.sync();

        assertEquals( "no value", testNode.get( "cn", "no value" ) );
        assertEquals( "no value", testNode.get( "roomNumber", "no value" ) );
    }
}
