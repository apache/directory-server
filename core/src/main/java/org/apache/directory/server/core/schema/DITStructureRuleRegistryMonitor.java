/*
 *   Copyright 2004 The Apache Software Foundation
 *
 *   Licensed under the Apache License, Version 2.0 (the "License");
 *   you may not use this file except in compliance with the License.
 *   You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *   Unless required by applicable law or agreed to in writing, software
 *   distributed under the License is distributed on an "AS IS" BASIS,
 *   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *   See the License for the specific language governing permissions and
 *   limitations under the License.
 *
 */
package org.apache.directory.server.core.schema;


import org.apache.directory.shared.ldap.schema.DITStructureRule;


/**
 * Interface for DITStructureRuleRegitery callback event monitors.
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 * @version $Rev$
 */
public interface DITStructureRuleRegistryMonitor
{
    /**
     * Monitors when a DITStructureRule is registered successfully.
     *
     * @param dITStructureRule the DITStructureRule successfully registered
     */
    void registered( DITStructureRule dITStructureRule );


    /**
     * Monitors when a Comparator is successfully looked up.
     *
     * @param dITStructureRule the DITStructureRule successfully lookedup
     */
    void lookedUp( DITStructureRule dITStructureRule );


    /**
     * Monitors when a lookup attempt fails.
     *
     * @param oid the OID for the DITStructureRule to lookup
     * @param fault the exception to be thrown for the fault
     */
    void lookupFailed( String oid, Throwable fault );


    /**
     * Monitors when a registration attempt fails.
     *
     * @param dITStructureRule the DITStructureRule which failed registration
     * @param fault the exception to be thrown for the fault
     */
    void registerFailed( DITStructureRule dITStructureRule, Throwable fault );
}
