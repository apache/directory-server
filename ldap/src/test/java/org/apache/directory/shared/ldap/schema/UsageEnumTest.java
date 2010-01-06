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
package org.apache.directory.shared.ldap.schema;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

/**
 * The unit tests for methods on UsageEnum.
 * 
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 * @version $Rev: 485048 $
 */
public class UsageEnumTest
{
    @Test
    public void testGetValue()
    {
        assertEquals( 0, UsageEnum.USER_APPLICATIONS.getValue() );
        assertEquals( 1, UsageEnum.DIRECTORY_OPERATION.getValue() );
        assertEquals( 2, UsageEnum.DISTRIBUTED_OPERATION.getValue() );
        assertEquals( 3, UsageEnum.DSA_OPERATION.getValue() );
    }
    
    @Test
    public void testGetUsage()
    {
        assertEquals( UsageEnum.DIRECTORY_OPERATION, UsageEnum.getUsage( "directoryOperation" ) );
        assertEquals( UsageEnum.USER_APPLICATIONS, UsageEnum.getUsage( "userApplications" ) );
        assertEquals( UsageEnum.DISTRIBUTED_OPERATION, UsageEnum.getUsage( "distributedOperation" ) );
        assertEquals( UsageEnum.DSA_OPERATION, UsageEnum.getUsage( "dSAOperation" ) );
        assertEquals( null, UsageEnum.getUsage( "azerty" ) );
    }
    
    @Test
    public void testRenderer()
    {
        assertEquals( "directoryOperation", UsageEnum.render( UsageEnum.DIRECTORY_OPERATION ) );
        assertEquals( "userApplications", UsageEnum.render( UsageEnum.USER_APPLICATIONS ) );
        assertEquals( "distributedOperation", UsageEnum.render( UsageEnum.DISTRIBUTED_OPERATION ) );
        assertEquals( "dSAOperation", UsageEnum.render( UsageEnum.DSA_OPERATION ) );
        assertEquals( "userApplications", UsageEnum.render( null ) );
    }
}
