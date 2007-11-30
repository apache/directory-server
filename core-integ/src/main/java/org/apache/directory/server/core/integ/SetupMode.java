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
 * Different modes of conducting core tests.
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 * @version $Rev$, $Date$
 */
public enum SetupMode
{
    /*

      MATRIX FOR MODE ACTIONS BASED ON SERVER STATE
    
                | NOSERVICE | PRISTINE  | RESTART  | ROLLBACK | CUMULATIVE
                +===========+===========+==========+==========+===========
                |           |           |          |          |
       RUNNING  |  NOTHING  | SHUTDOWN  | SHUTDOWN |  REVERT  |  NOTHING
       STOPPED  |  NOTHING  | CLEANUP   | STARTUP  |  CLEANUP |  RESTART
       MISSING  |  NOTHING  | CREATE    | CREATE   |  CREATE  |  CREATE

    */


    /**
     * If a service is running this mode will shutdown the service, destroy
     * it's working directory, null it out and start all over again with a
     * new service object to start it up fresh.  If no service is running,
     * yet a valid handle to a stopped service exists, this handle is used
     * to destroy the working directory then the handle is nulled out.
     * Whether or not a valid service exists a new one is created, and
     * started up. 
     */
    PRISTINE( 0, "PRISTINE: Fresh test with full working directory cleanout." ),
    /**
     * If a service is running this mode will shutdown the service, WITHOUT
     * destroying it's working directory, so changes made in tests are or
     * should be persistant. The same service object is restarted without
     * creating a new one.  If the service exists yet is found to have been
     * shutdown it is restarted.  If no service is available, one is created
     * and started up.
     */
    RESTART( 1, "RESTART: Working directories are not cleaned out but the core is restarted." ),
    /**
     * If a service is running this mode will NOT shutdown the service,
     * instead the service's state will be reverted to it's previous state
     * before the last test which operated on it.  So changes are not
     * persisted across tests.  If the service exists yet has been shutdown
     * the working directory is cleaned out and the service is started up.
     * We must destroy working directories since reverts are not possible
     * across shutdowns at this point in time (change log is not persistent).
     */
    ROLLBACK( 2, "ROLLBACK: The service is not stopped, it's state is restored to the original startup state." ),
    /**
     * If a service is running it is used as is.  Changes across tests have
     * no isolation.  If the service has been stopped it is simply restarted.
     * If the service does not exists it is created then started.  There is
     * no attempt to destroy existing working directories if any at all exist.
     */
    CUMULATIVE( 3, "CUMULATIVE: Nothing is done to the service between tests so changes accumulate." ),
    /**
     * Does nothing at all.  Does not care if service is running or if it
     * exists.  This is the default.  Really useful with suites which you
     * may not want to do anything with.  Otherwise for all other modes a
     * suite will start up a server before all runs and shut it down after
     * all runs.
     */
    NOSERVICE( 4, "NOSERVICE: No service is required at all." );

    public static final int PRISTINE_ORDINAL = 0;
    public static final int RESTART_ORDINAL = 1;
    public static final int ROLLBACK_ORDINAL = 2;
    public static final int CUMULATIVE_ORDINAL = 3;
    public static final int NOSERVICE_ORDINAL = 4;


    public final int ordinal;
    public final String description;


    private SetupMode( int ordinal, String description )
    {
        this.ordinal = ordinal;
        this.description = description;
    }
}
