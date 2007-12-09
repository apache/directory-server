/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.directory.server.core.integ;


/**
 * Document me!
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 * @version $Rev$, $Date$
 */
public enum ServiceCleanupLevel
{
    TESTSUITE( 0, "service has test suite level scope" ),
    TESTCLASS( 1, "service has test class level scope" ),
    TESTSYSTEM( 2, "service has test system level scope" ),
    TESTMETHOD( 3, "service has test method level scope" );

    public final int ordinal;
    public final String description;

    public static final int TESTSUITE_ORDINAL = 0;
    public static final int TESTCLASS_ORDINAL = 1;
    public static final int TESTSYSTEM_ORDINAL = 2;
    public static final int TESTMETHOD_ORDINAL = 2;


    ServiceCleanupLevel( int ordinal, String description )
    {
        this.ordinal = ordinal;
        this.description = description;
    }
}
