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
package jdbm.helper;


import static org.junit.Assert.assertEquals;

import org.junit.Test;
import org.junit.runner.RunWith;

import com.mycila.junit.concurrent.Concurrency;
import com.mycila.junit.concurrent.ConcurrentJunitRunner;


/**
 * 
 * TODO TestActionVersioning.
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 */
@RunWith(ConcurrentJunitRunner.class)
@Concurrency()
public class TestActionVersioning
{
    @Test
    public void testVersioning()
    {
        ActionVersioning.Version version1, version2;
        ActionVersioning.Version writeVersion;
        ActionVersioning.Version minVersion;

        ActionVersioning versioning = new ActionVersioning();
        version1 = versioning.beginReadAction();
        assertEquals( version1.getVersion(), 0 );

        writeVersion = versioning.beginWriteAction();
        assertEquals( writeVersion.getVersion(), 1 );

        version2 = versioning.beginReadAction();
        assertEquals( version2.getVersion(), 0 );

        minVersion = versioning.endWriteAction();
        assertEquals( minVersion.getVersion(), 0 );

        writeVersion = versioning.beginWriteAction();
        assertEquals( writeVersion.getVersion(), 2 );

        minVersion = versioning.endWriteAction();
        assertEquals( minVersion.getVersion(), 0 );

        versioning.endReadAction( version1 );
        minVersion = versioning.endReadAction( version2 );
        assertEquals( minVersion.getVersion(), 2 );

        version1 = versioning.beginReadAction();
        assertEquals( version1.getVersion(), 2 );

        minVersion = versioning.endReadAction( version1 );
        assertEquals( minVersion, null );

    }
}