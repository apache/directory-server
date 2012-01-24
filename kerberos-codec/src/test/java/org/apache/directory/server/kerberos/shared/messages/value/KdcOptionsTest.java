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
package org.apache.directory.server.kerberos.shared.messages.value;


import static org.junit.Assert.assertTrue;

import java.util.Arrays;

import com.mycila.junit.concurrent.Concurrency;
import com.mycila.junit.concurrent.ConcurrentJunitRunner;
import org.apache.directory.shared.kerberos.codec.options.KdcOptions;
import org.junit.Test;
import org.junit.runner.RunWith;


/**
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 */
@RunWith(ConcurrentJunitRunner.class)
@Concurrency()
public class KdcOptionsTest
{
    private static final byte[] fpriOptions =
        { 0x00, ( byte ) 0x50, ( byte ) 0x00, ( byte ) 0x00, ( byte ) 0x10 };


    /**
     * Tests the basic construction of the {@link KdcOptions}.
     */
    @Test
    public void testConstruction()
    {
        KdcOptions options = new KdcOptions( fpriOptions );
        assertTrue( Arrays.equals( options.getBytes(), fpriOptions ) );
    }
}
