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
package org.apache.directory.server.core.schema;


import java.util.Comparator;

import javax.naming.NamingException;


/**
 * Monitor interface for a ComparatorRegistry.
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 * @version $Rev$
 */
public interface ComparatorRegistryMonitor
{
    /**
     * Monitors when a Comparator is registered successfully.
     * 
     * @param oid OID key used for registration
     * @param comparator the Comparator registered
     */
    void registered( String oid, Comparator comparator );


    /**
     * Monitors when a Comparator is successfully looked up.
     * 
     * @param oid OID key used for registration
     * @param comparator the Comparator looked up
     */
    void lookedUp( String oid, Comparator comparator );


    /**
     * Monitors when a lookup attempt fails.
     * 
     * @param oid the OID for the Comparator to lookup
     * @param fault the exception to be thrown for the fault
     */
    void lookupFailed( String oid, NamingException fault );


    /**
     * Monitors when a registration attempt fails.
     * 
     * @param oid OID key used for registration
     * @param comparator the Comparator which failed registration
     * @param fault the exception to be thrown for the fault
     */
    void registerFailed( String oid, Comparator comparator, NamingException fault );
}
