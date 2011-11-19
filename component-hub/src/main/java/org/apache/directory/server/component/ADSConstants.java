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
package org.apache.directory.server.component;


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
     * base directory path for component cache
     */
    public static final String ADS_CACHE_BASE_DIR = "adscache";

    /*
     * default file name for caching schemas inside for components
     */
    public static final String ADS_CACHE_SCHEMA_FILE = "schema.ldif";

    /*
     * default file name for caching instance entries inside for components
     */
    public static final String ADS_CACHE_INSTANCES_FILE = "instances.ldif";

    /*
     * default file name for caching version information for components
     */
    public static final String ADS_CACHE_VERSION_FILE = "version";

    /*
     * IPojo Handler name for ADSComponent annotation.
     */
    public static final String ADS_COMPONENT_HANDLER_NS = "org.apache.directory.server.component.handler";

    /*
     * IPojo Handler name for ADSComponent annotation.
     */
    public static final String ADS_COMPONENT_HANDLER_NAME = "ADSComponentHandler";

    /*
     * IPojo Handler name for ADSComponent annotation.
     */
    public static final String ADS_COMPONENT_HANDLER_FULLNAME = "org.apache.directory.server.component.handler:ADSComponentHandler";

    /*
     * iPOJO Component Type and Instance declaration header.
     */
    public static final String IPOJO_HEADER = "iPOJO-Components";

    /*
     * Base OID value for ApacheDS component schemas.
     */
    public static final String ADS_COMPONENT_BASE_OID = "1.3.6.1.4.1.18060.0.4.4";

    /*
     * Component type name of user defined components.
     */
    public static final String ADS_COMPONENT_TYPE_USER = "user";

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
