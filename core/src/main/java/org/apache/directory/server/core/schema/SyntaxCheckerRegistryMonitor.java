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


import javax.naming.NamingException;

import org.apache.directory.shared.ldap.schema.SyntaxChecker;


/**
 * Monitor interface for a SyntaxCheckerRegistry.
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 * @version $Rev$
 */
public interface SyntaxCheckerRegistryMonitor
{
    /**
     * Monitors when a SyntaxChecker is registered successfully.
     * 
     * @param oid
     * @param syntaxChecker the SyntaxChecker registered
     */
    void registered( String oid, SyntaxChecker syntaxChecker );

    /**
     * Monitors when a SyntaxChecker is successfully looked up.
     * 
     * @param oid
     * @param syntaxChecker the SyntaxChecker looked up
     */
    void lookedUp( String oid, SyntaxChecker syntaxChecker );

    /**
     * Monitors when a lookup attempt fails.
     * 
     * @param oid the OID for the SyntaxChecker to lookup
     * @param fault the exception to be thrown for the fault
     */
    void lookupFailed( String oid, NamingException fault );
    
    /**
     * Monitors when a registration attempt fails.
     * 
     * @param oid
     * @param syntaxChecker the SyntaxChecker which failed registration
     * @param fault the exception to be thrown for the fault
     */
    void registerFailed( String oid, SyntaxChecker syntaxChecker, NamingException fault );
}
