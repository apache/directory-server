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
 * A scope or level of testing.  There are four levels:
 *
 * <ul>
 *   <li>
 *     <b>system level</b>: the level external to the testing framework</li>
 *   </li>
 *   <li>
 *     <b>suite level</b>: the level representing test suite scope</li>
 *   </li>
 *   <li>
 *     <b>class level</b>: the level representing test class scope</li>
 *   </li>
 *   <li>
 *     <b>method level</b>: the lowest level representing test method scope</li>
 *   </li>
 * </ul>
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 * @version $Rev$, $Date$
 */
public enum Level
{
    SUITE( 0, "test suite level" ),
    CLASS( 1, "test class level" ),
    SYSTEM( 2, "test system level" ),
    METHOD( 3, "test method level" );

    public final int ordinal;
    public final String description;

    public static final int SUITE_ORDINAL = 0;
    public static final int CLASS_ORDINAL = 1;
    public static final int SYSTEM_ORDINAL = 2;
    public static final int METHOD_ORDINAL = 3;


    Level( int ordinal, String description )
    {
        this.ordinal = ordinal;
        this.description = description;
    }
}
