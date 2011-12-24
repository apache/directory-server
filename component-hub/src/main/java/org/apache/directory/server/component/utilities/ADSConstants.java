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


import java.util.Dictionary;
import java.util.Hashtable;


/**
 * Class to hold constants for ApacheDS component management.
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 */
public class ADSConstants
{
    /*
     * Component Hub factory name
     */
    public static final String ADS_HUB_FACTORY_NAME = "ADSComponentHub";

    /*
     * base directory path for component cache
     */
    public static final String ADS_CACHE_BASE_DIR = "adscache";

    /*
     * default file name for caching schemas inside for components
     */
    public static final String ADS_CACHE_SCHEMA_FILE = "schema-elements.ldif";

    /*
     * default file name for caching instance entries inside for components
     */
    public static final String ADS_CACHE_INSTANCES_DIR = "instance-configs";

    /*
     * default file name for caching version information for components
     */
    public static final String ADS_CACHE_VERSION_FILE = "version";

    /*
     * Component type name of interceptors
     */
    public static final String ADS_COMPONENT_TYPE_INTERCEPTOR = "interceptor";

    /*
     * Component type name of partitions
     */
    public static final String ADS_COMPONENT_TYPE_PARTITION = "partition";

    /*
     * Component type name of servers
     */
    public static final String ADS_COMPONENT_TYPE_SERVER = "server";

    /*
     * Hash table for mapping property type name to its syntax in ApacheDS
     */
    public static Dictionary<String, String> syntaxMappings;

    /*
     * Hash table for mapping property type name to its equality matching rule in ApacheDS
     */
    public static Dictionary<String, String> equalityMappings;

    /*
     * Hash table for mapping property type name to its ordering rule in ApacheDS
     */
    public static Dictionary<String, String> orderingMappings;

    /*
     * Hash table for mapping property type name to its substring matching in ApacheDS
     */
    public static Dictionary<String, String> substringMappings;

    /**
     * Static constructor that initializes the values of the type mapping Hash tables.
     */
    static
    {
        syntaxMappings = new Hashtable<String, String>();
        syntaxMappings.put( "int", "1.3.6.1.4.1.1466.115.121.1.27" );
        syntaxMappings.put( "java.lang.String", "1.3.6.1.4.1.1466.115.121.1.15" );
        syntaxMappings.put( "boolean", "1.3.6.1.4.1.1466.115.121.1.7" );

        equalityMappings = new Hashtable<String, String>();
        equalityMappings.put( "int", "integerMatch" );
        equalityMappings.put( "java.lang.String", "caseExactMatch" );
        equalityMappings.put( "boolean", "booleanMatch" );

        orderingMappings = new Hashtable<String, String>();
        orderingMappings.put( "int", "integerOrderingMatch" );
        orderingMappings.put( "java.lang.String", "caseExactOrderingMatch" );
        orderingMappings.put( "boolean", "caseIgnoreOrderingMatch" );

        substringMappings = new Hashtable<String, String>();
        substringMappings.put( "int", "numericStringSubstringsMatch" );
        substringMappings.put( "java.lang.String", "caseExactSubstringsMatch" );
        substringMappings.put( "boolean", "caseIgnoreSubstringsMatch" );
    }

}
