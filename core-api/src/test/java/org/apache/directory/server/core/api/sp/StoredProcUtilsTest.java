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

package org.apache.directory.server.core.api.sp;


import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.parallel.Execution;
import org.junit.jupiter.api.parallel.ExecutionMode;


@Execution(ExecutionMode.CONCURRENT)
public class StoredProcUtilsTest
{
    @Test
    public void testSPNameTokenization()
    {
        String fullSPName = "Greeter:seyHello";
        String expectedSPUnitName = "Greeter";
        String expectedSPName = "seyHello";

        String actualSPUnitName = StoredProcUtils.extractStoredProcUnitName( fullSPName );
        String actualSPName = StoredProcUtils.extractStoredProcName( fullSPName );

        assertEquals( expectedSPUnitName, actualSPUnitName );
        assertEquals( expectedSPName, actualSPName );
    }

}
