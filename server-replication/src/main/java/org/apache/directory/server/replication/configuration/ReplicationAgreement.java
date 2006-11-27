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
package org.apache.directory.server.replication.configuration;


import java.util.Set;

import org.apache.directory.shared.ldap.name.LdapDN;
import org.apache.directory.shared.ldap.subtree.SubtreeSpecification;


/**
 * Document me!
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 * @version $Rev$
 */
public class ReplicationAgreement
{
    /**
     * Each replication agreement has a unique identifier.
     */
    String id;
    
    /** 
     * The set of critical attributes: those that trigger the immediate 
     * initiation of a replication cycle.  
     * 
     * These are numeric attribute OIDs so the structure can be used for 
     * fast lookups.
     */
    Set<String> criticalAttributes;
    
    /** 
     * The set of attributes that are not replicated: note that attributes
     * of type dSAOperation and collective attributes are not replicated.
     * 
     * These are numeric attribute OIDs so the structure can be used for 
     * fast lookups.
     */
    Set<String> exclusions;
    
    /** 
     * The set of attributes that are replicated.  Some attributes may be
     * dSAOperation attributes and may still need to be replicated.
     * 
     * These are numeric attribute OIDs so the structure can be used for 
     * fast lookups.
     */
    Set<String> inclusions;
    
    /**
     * The replication group composing this agreement. 
     */
    ReplicationGroup replicationGroup;
    
    /**
     * The replicatin area defined as a subtreeSpecification.
     */
    SubtreeSpecification area;
    
    /**
     * The administrative point for the replication specific autonomous area.
     */
    LdapDN replicationBase;
    
    /**
     * The schedule to use for initiating replication cycles.  This may be:
     * <ul>
     *   <li>periodic</li>
     *   <li>manual</li>
     *   <li>singular</li>
     *   <li>on change with optional delay</li>
     *   <li>after meeting a threshold of changes</li>
     *   <li>minimum time after the last replication cycle</li>
     * </ul> 
     */
    ReplicationSchedule replicationSchedule;
}
