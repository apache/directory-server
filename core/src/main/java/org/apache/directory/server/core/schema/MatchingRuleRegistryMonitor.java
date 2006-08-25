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


import javax.naming.NamingException;

import org.apache.directory.shared.ldap.schema.MatchingRule;


/**
 * Monitor interface for a MatchingRuleRegistry.
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 * @version $Rev$
 */
public interface MatchingRuleRegistryMonitor
{
    /**
     * Monitors when a MatchingRule is registered successfully.
     * 
     * @param matchingRule the MatchingRule registered
     */
    void registered( MatchingRule matchingRule );


    /**
     * Monitors when a MatchingRule is successfully looked up.
     * 
     * @param matchingRule the MatchingRule looked up
     */
    void lookedUp( MatchingRule matchingRule );


    /**
     * Monitors when a lookup attempt fails.
     * 
     * @param oid the OID for the MatchingRule to lookup
     * @param fault the exception to be thrown for the fault
     */
    void lookupFailed( String oid, NamingException fault );


    /**
     * Monitors when a registration attempt fails.
     * 
     * @param matchingRule the MatchingRule which failed registration
     * @param fault the exception to be thrown for the fault
     */
    void registerFailed( MatchingRule matchingRule, NamingException fault );
}
