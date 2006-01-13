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
package org.apache.ldap.server.schema;


import org.apache.ldap.common.schema.DITContentRule;


/**
 * Interface for DITContentRuleRegitery callback event monitors.
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 * @version $Rev$
 */
public interface DITContentRuleRegistryMonitor
{
    /**
     * Monitors when a DITContentRule is registered successfully.
     *
     * @param dITContentRule the DITContentRule successfully registered
     */
    void registered( DITContentRule dITContentRule );

    /**
     * Monitors when a Comparator is successfully looked up.
     *
     * @param dITContentRule the DITContentRule successfully lookedup
     */
    void lookedUp( DITContentRule dITContentRule );

    /**
     * Monitors when a lookup attempt fails.
     *
     * @param oid the OID for the DITContentRule to lookup
     * @param fault the exception to be thrown for the fault
     */
    void lookupFailed( String oid, Throwable fault );

    /**
     * Monitors when a registration attempt fails.
     *
     * @param dITContentRule the DITContentRule which failed registration
     * @param fault the exception to be thrown for the fault
     */
    void registerFailed( DITContentRule dITContentRule, Throwable fault );
}
