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
package org.apache.directory.server.xdbm;


import org.junit.Assume;
import org.junit.BeforeClass;

import junit.framework.TestCase;


/**
 * Tests a Table implementation to make sure it correctly observes the 
 * semantics of the Table interface.
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 * @version $Rev$, $Date$
 */
public class TableTest extends TestCase
{
    private XdbmFactory factory;
    
    
    @BeforeClass
    public void setupFactory() throws Exception
    {
        factory = XdbmFactory.instance();
    }
    
    
    /**
     * Tests a Table implementation without duplicate keys to see if it 
     * behaves correctly with it's put and get methods.
     */
    public void testNoDuplicatesPutGet() throws Exception
    {
        Assume.assumeNotNull( factory );
        
        Table<Integer, Integer> table = factory.createTable();
        
    }
}
