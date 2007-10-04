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

package org.apache.directory.server.core.sp;


import static org.junit.Assert.assertNotNull;

import java.util.ArrayList;
import java.util.List;

import javax.naming.NamingException;
import javax.naming.directory.Attributes;
import javax.naming.directory.BasicAttributes;

import org.junit.Test;


/**
 * Tests for class StoredProcExecutionManager.
 * 
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 * @version $Rev$
 */
public class StoredProcExecutionManagerTest
{
    private StoredProcExecutionManager manager;
    private List<StoredProcEngineConfig> cfgs;


    /**
     * The StoredProcExecutionManager should find an engine for a known language.
     */
    @Test
    public void shouldFindAnEngineForKnownLanguage() throws NamingException
    {
        String langId = "myLanguage";

        cfgs = new ArrayList<StoredProcEngineConfig>();

        cfgs.add( new DummyStoredProcEngineConfig( "aLanguage" ) );
        cfgs.add( new DummyStoredProcEngineConfig( langId ) );
        cfgs.add( new DummyStoredProcEngineConfig( "yourLanguage" ) );
        manager = new StoredProcExecutionManager( "cn=anyContainer", cfgs );

        Attributes attrs = new BasicAttributes( "storedProcLangId", langId );

        StoredProcEngine engine = manager.getStoredProcEngineInstance( attrs );
        assertNotNull( engine );
    }


    /**
     * The StoredProcExecutionManager should throw an exception for an unknown language.
     */
    @Test(expected = NamingException.class)
    public void shouldThrowExceptionOnUnknownLanguage() throws NamingException
    {
        String langId = "myUnknownLanguage";

        cfgs = new ArrayList<StoredProcEngineConfig>();

        cfgs.add( new DummyStoredProcEngineConfig( "aLanguage" ) );
        cfgs.add( new DummyStoredProcEngineConfig( "bLanguage" ) );
        manager = new StoredProcExecutionManager( "cn=anyContainer", cfgs );

        Attributes attrs = new BasicAttributes( "storedProcLangId", langId );

        manager.getStoredProcEngineInstance( attrs );
    }
}
