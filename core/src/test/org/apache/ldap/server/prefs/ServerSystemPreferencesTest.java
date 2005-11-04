/*
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
package org.apache.ldap.server.prefs;


import java.util.prefs.BackingStoreException;
import java.util.prefs.Preferences;

import org.apache.ldap.server.unit.AbstractAdminTestCase;


/**
 * Tests the ServerSystemPreferences class.
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 * @version $Rev$
 */
public class ServerSystemPreferencesTest extends AbstractAdminTestCase
{
    private ServerSystemPreferences prefs;
    
    public void setUp() throws Exception
    {
        super.setUp();
        prefs = new ServerSystemPreferences();
    }
    
    /**
     * Tests to make sure the system preferences root has entry (test, abc123).
     */
    public void testRoot() throws Exception
    {
        assertEquals( "sysPrefRoot", prefs.get( "prefNodeName", "not the value" ) );
    }

    /**
     * Tests the creation and use of a new preferences node.
     *
     * @throws BackingStoreException if there are failures with the store
     */
    public void testCreate() throws BackingStoreException
    {
        Preferences testNode = prefs.node( "testNode" );

        testNode.put( "testNodeKey", "testNodeValue" );
        testNode.sync();
    }


    /**
     * Tests the creation and use of a new preferences node.
     *
     * @throws BackingStoreException if there are failures with the store
     */
    public void testCreateAndSet() throws BackingStoreException
    {
        Preferences testNode = prefs.node( "testNode" );

        testNode.put( "testNodeKey", "testNodeValue" );
        testNode.sync();

        testNode.putBoolean( "boolKey", true );
        testNode.putByteArray( "arrayKey", new byte[10] );
        testNode.putDouble( "doubleKey", 3.14 );
        testNode.putFloat( "floatKey", ( float ) 3.14 );
        testNode.putInt( "intKey", 345 );
        testNode.putLong( "longKey", 75449559185447L );
        testNode.sync();

        testNode = prefs.node( "testNode" );

        assertEquals( true, testNode.getBoolean( "boolKey", false ) );
        assertTrue( 3.14 == testNode.getDouble( "doubleKey", 9.20 ) );
        assertTrue( (float) 3.14 == testNode.getFloat( "floatKey", (float) 3.90 ) );
        assertEquals( 345, testNode.getInt( "intKey", 87 ) );
        assertEquals( 75449559185447L, testNode.getLong( "longKey", 75449547L ) );
    }


    /**
     * Tests the creation and use of a new preferences node.
     *
     * @throws BackingStoreException if there are failures with the store
     */
    public void testCreateAndRemove() throws BackingStoreException
    {
        Preferences testNode = prefs.node( "testNode" );

        testNode.put( "testNodeKey", "testNodeValue" );
        testNode.sync();

        testNode.putBoolean( "boolKey", true );
        testNode.putByteArray( "arrayKey", new byte[10] );
        testNode.putDouble( "doubleKey", 3.14 );
        testNode.putFloat( "floatKey", ( float ) 3.14 );
        testNode.putInt( "intKey", 345 );
        testNode.putLong( "longKey", 75449559185447L );
        testNode.sync();

        testNode = prefs.node( "testNode" );

        assertEquals( true, testNode.getBoolean( "boolKey", false ) );
        assertTrue( 3.14 == testNode.getDouble( "doubleKey", 9.20 ) );
        assertTrue( (float) 3.14 == testNode.getFloat( "floatKey", (float) 3.90 ) );
        assertEquals( 345, testNode.getInt( "intKey", 87 ) );
        assertEquals( 75449559185447L, testNode.getLong( "longKey", 75449547L ) );

        testNode.remove( "doubleKey" );
        testNode.remove( "arrayKey" );

        assertEquals( "no value", testNode.get( "doubleKey", "no value" ) );
        assertEquals( "no value", testNode.get( "arrayKey", "no value" ) );

        testNode.sync();

        assertEquals( "no value", testNode.get( "doubleKey", "no value" ) );
        assertEquals( "no value", testNode.get( "arrayKey", "no value" ) );
    }
}
