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
package org.apache.directory.server.core.unit;


/**
 * Different modes of conducting core tests.
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 * @version $Rev$, $Date$
 */
public enum TestMode
{
    PRISTINE( 0, "Fresh test with full working directory cleanout." ),
    RESTART( 1, "Working directories are not cleaned out but the core is restarted." ),
    ROLLBACK( 2, "The server is not stopped, it's state is restored to the original startup state." ),
    ADDITIVE( 3, "Nothing is done to the server which collects changes across tests." );
    
    public static final int PRISTINE_ORDINAL = 0;
    public static final int RESTART_ORDINAL = 1;
    public static final int ROLLBACK_ORDINAL = 2;
    public static final int ADDITIVE_ORDINAL = 3;


    public final int ordinal;
    public final String description;


    private TestMode( int ordinal, String description )
    {
        this.ordinal = ordinal;
        this.description = description;
    }
}
