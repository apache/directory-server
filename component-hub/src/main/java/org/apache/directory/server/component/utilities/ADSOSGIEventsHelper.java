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
package org.apache.directory.server.component.utilities;


public class ADSOSGIEventsHelper
{
    /** Header for all events under ComponentHub */
    private static final String eventHeader = "org/apache/directory/server/";

    /** DirectoryService reference argument name for ComponentHub OSGI Events. */
    public static final String ADS_EVENT_ARG_DS = "org/apache/directory/server/dsreference";


    public static String getTopic_CoreInterceptorsReady( String instanceDir )
    {
        return eventHeader + instanceDir + "/CoreInterceptorsReady";
    }


    public static String getTopic_CorePartitionsReady( String instanceDir )
    {
        return eventHeader + instanceDir + "/CorePartitionsReady";
    }


    public static String getTopic_DSInitialized( String instanceDir )
    {
        return eventHeader + instanceDir + "/DSInitialized";
    }
}
