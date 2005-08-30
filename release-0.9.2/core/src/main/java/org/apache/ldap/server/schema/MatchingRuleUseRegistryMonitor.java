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


import org.apache.ldap.common.schema.MatchingRuleUse;


/**
 * Interface for MatchingRuleUseRegitery callback event monitors.
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 * @version $Rev$
 */
public interface MatchingRuleUseRegistryMonitor
{
    /**
     * Monitors when a MatchingRuleUse is registered successfully.
     *
     * @param matchingRuleUse the MatchingRuleUse successfully registered
     */
    void registered( MatchingRuleUse matchingRuleUse );

    /**
     * Monitors when a Comparator is successfully looked up.
     *
     * @param matchingRuleUse the MatchingRuleUse successfully lookedup
     */
    void lookedUp( MatchingRuleUse matchingRuleUse );

    /**
     * Monitors when a lookup attempt fails.
     *
     * @param name the name of the matchingRuleUse
     * @param fault the exception to be thrown for the fault
     */
    void lookupFailed( String name, Throwable fault );

    /**
     * Monitors when a registration attempt fails.
     *
     * @param matchingRuleUse the MatchingRuleUse which failed registration
     * @param fault the exception to be thrown for the fault
     */
    void registerFailed( MatchingRuleUse matchingRuleUse, Throwable fault );
}
