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
package org.apache.directory.server.component.schema;


import org.apache.directory.server.component.utilities.ADSConstants;


/**
 * This is a simple incremental generator for OID assignments of custom generated schemas.
 * Ensuring consistency among different servers on the cluster will be replication layer's duty.
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 */
public class ComponentOIDGenerator
{
    /*
     * Base OID to generate sequential OIDs against.
     */
    private static String baseOID = null;

    /*
     * Counters to keep track.
     */
    private static int componentCounter;
    private static int ocCounter;
    private static int attribCounter;

    static
    {
        baseOID = ADSConstants.ADS_COMPONENT_BASE_OID;

        componentCounter = 0;
        ocCounter = 0;
        attribCounter = 0;
    }


    /**
     * Returns OID for component
     *
     * @return oid for component
     */
    public static synchronized String generateComponentOID()
    {
        return baseOID + "." + ( ++componentCounter );
    }


    /**
     * Returns OID for object class under specified component base OID.
     *
     * @return oid for object class
     */
    public static synchronized String generateOCOID( String componentBase )
    {
        return componentBase + ".1." + ( ++ocCounter );
    }


    /**
     * Returns OID for attribute under specified component base OID.
     *
     * @return oid for attribute
     */
    public static synchronized String generateAttribOID( String componentBase )
    {
        return componentBase + ".2." + ( ++attribCounter );
    }
}
