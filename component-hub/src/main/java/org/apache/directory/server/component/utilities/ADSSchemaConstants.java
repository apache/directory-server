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


public class ADSSchemaConstants
{

    /*
     * Base schema name for holding component's elements
     */
    public static final String ADS_COMPONENT_BASE = "componenthub";
    public static final String ADS_COMPONENT_BASE_OID = "1.3.6.1.4.1.18060.0.4.4";

    /*
     * OC name of the component OC.
     */
    public static final String ADS_COMPONENT = "ads-component";
    public static final String ADS_COMPONENT_OID = "1.3.6.1.4.1.18060.0.4.4.0.2.1";

    /*
     * Attribute name of component name
     */
    public static final String ADS_COMPONENT_ATTRIB_NAME = "ads-componentname";
    public static final String ADS_COMPONENT_ATTRIB_NAME_OID = "1.3.6.1.4.1.18060.0.4.4.0.1.1";

    /*
     * Attribute name of component type
     */
    public static final String ADS_COMPONENT_ATTRIB_TYPE = "ads-componenttype";
    public static final String ADS_COMPONENT_ATTRIB_TYPE_OID = "1.3.6.1.4.1.18060.0.4.4.0.1.2";

    /*
     * Attribute name of component object class
     */
    public static final String ADS_COMPONENT_ATTRIB_OCNAME = "ads-ocname";
    public static final String ADS_COMPONENT_ATTRIB_OCNAME_OID = "1.3.6.1.4.1.18060.0.4.4.0.1.3";

    /*
     * Attribute name of component purge count
     */
    public static final String ADS_COMPONENT_ATTRIB_PURGE = "ads-componentpurgecount";
    public static final String ADS_COMPONENT_ATTRIB_PURGE_OID = "1.3.6.1.4.1.18060.0.4.4.0.1.4";

    /*
     * Attribute name of component version
     */
    public static final String ADS_COMPONENT_ATTRIB_VERSION = "ads-instance";
    public static final String ADS_COMPONENT_ATTRIB_VERSION_OID = "1.3.6.1.4.1.18060.0.4.4.0.1.5";

    /*
     * Attribute name of component instance name
     */
    public static final String ADS_COMPONENT_INSTANCE_ATTRIB_NAME = "ads-instance";
    public static final String ADS_COMPONENT_INSTANCE_ATTRIB_NAME_OID = "1.3.6.1.4.1.18060.0.4.4.0.1.0";
    
}
