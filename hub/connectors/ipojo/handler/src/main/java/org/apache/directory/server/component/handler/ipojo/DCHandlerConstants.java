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

package org.apache.directory.server.component.handler.ipojo;


public class DCHandlerConstants
{
    /*
     * Handler names for Component Handlers
     */
    public static final String DSCOMPONENT_HANDLER_NAME = "directorycomponent";
    public static final String DSINTERCEPTOR_HANDLER_NAME = "directoryinterceptor";
    public static final String DSPARTITION_HANDLER_NAME = "directorypartition";
    public static final String DSSERVER_HANDLER_NAME = "directoryserver";

    /*
     * Handler namespace for DirectoryComponentHandler
     */
    public static final String DSCOMPONENT_HANDLER_NS = "org.apache.directory.server.component.handler";
    public static final String DSINTERCEPTOR_HANDLER_NS = "org.apache.directory.server.component.handler";
    public static final String DSPARTITION_HANDLER_NS = "org.apache.directory.server.component.handler";
    public static final String DSSERVER_HANDLER_NS = "org.apache.directory.server.component.handler";

    /*
     * Property name for specifying components owning ApacheDS instance name; 
     */
    public static final String DSCOMPONENT_OWNER_PROP_NAME = "dscomponent.owner";

}
