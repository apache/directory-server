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
package org.apache.directory.server.core.subtree;

import org.apache.directory.server.core.annotations.CreateDS;
import org.apache.directory.server.core.integ.FrameworkRunner;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * Test cases for the AdministrativePoint interceptor lookup operation.
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 */
@RunWith(FrameworkRunner.class)
@CreateDS(name = "AdministrativePointServiceIT")
public class SubentryLookupOperationIT
{
    // ===================================================================
    // Test the Lookup operation on APs
    // -------------------------------------------------------------------
    /**
     * Test the lookup of an AP. All APs are searcheable by default
     */
    @Test
    public void testLookupAP() throws Exception
    {
        // TODO
    }

    
    // ===================================================================
    // Test the Lookup operation on Subentries
    // -------------------------------------------------------------------
    /**
     * Test the lookup of a subentry. Subentries can be read directly as it's 
     * a OBJECT search.
     */
    @Test
    public void testLookupSubentry() throws Exception
    {
        // TODO
    }


    /**
     * Test the lookup of a subentry with the subentries control.
     */
    @Test
    public void testLookupSubentryWithControl() throws Exception
    {
        // TODO
    }
    
    
    // ===================================================================
    // Test the Lookup operation on Entries
    // -------------------------------------------------------------------
    /**
     * Test the lookup of a entry with no APs. The entry should not have
     * any SN
     */
    @Test
    public void testLookupEntryNoAp() throws Exception
    {
        // TODO
    }


    /**
     * Test the lookup of a entry added under an AP with no subentry. All
     * the entry SN must be set to -1, and not have any subentries reference
     */
    @Test
    public void testLookupEntryUnderApNoSubentry() throws Exception
    {
        // TODO
    }


    /**
     * Test the lookup of a entry when an AP with no subentry has been added. All
     * the entry SN must be set to -1, and not have any subentries reference
     */
    @Test
    public void testLookupEntryAfterApAdditionNoSubentry() throws Exception
    {
        // TODO
    }


    /**
     * Test the lookup of a entry added under an AP with a subentry. 
     * The entry is part of the subtreeSpecification.
     * All the entry SN must be set to the AP SN, and have any subentries reference
     */
    @Test
    public void testLookupEntryUnderApWithSubentrySelected() throws Exception
    {
        // TODO
    }


    /**
     * Test the lookup of a entry when an AP with a subentry is added. 
     * The entry is part of the subtreeSpecification.
     * All the entry SN must be set to the AP SN, and have any subentries reference
     */
    @Test
    public void testLookupEntryAfterApAdditionWithSubentrySelected() throws Exception
    {
        // TODO
    }


    /**
     * Test the lookup of a entry when the subentry it referes has been 
     * removed. The entry's reference to the subentry must be removed, the 
     * SN must be the AP SN
     */
    @Test
    public void testLookupEntryAfterSubentryDeletion() throws Exception
    {
        // TODO
    }
}
