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

package org.apache.directory.server.core.integ;


import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.apache.directory.server.core.annotations.CreateDS;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;


/**
 * TODO TestMultiLevelDS.
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 */
@ExtendWith( CreateDSTestExtension.class )
@CreateDS(name = "TestMultiLevelDS-class")
public class TestMultiLevelDS extends AbstractLdapTestUnit
{
    @Test
    public void testMethodWithClassLevelDs()
    {
        assertFalse( service.isAccessControlEnabled() );
        assertEquals( "TestMultiLevelDS-class", service.getInstanceId() );
    }


    @Test
    @CreateDS(enableAccessControl = true, name = "testMethodWithClassLevelDs-method")
    public void testMethodWithMethodLevelDs()
    {
        assertTrue( methodService.isAccessControlEnabled() );
        assertEquals( "testMethodWithClassLevelDs-method", methodService.getInstanceId() );
    }
}
