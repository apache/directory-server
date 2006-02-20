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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * An adapter for the SyntaxCheckerRegistry's monitor.
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 * @version $Rev$
 */
public class SyntaxCheckerRegistryMonitorAdapter implements SyntaxCheckerRegistryMonitor
{
    private static final Logger log = LoggerFactory.getLogger( SyntaxCheckerRegistryMonitorAdapter.class );


    /* (non-Javadoc)
     * @see org.apache.directory.server.schema.SyntaxCheckerRegistryMonitor#registered(
     * org.apache.eve.schema.SyntaxChecker)
     */
    public void registered( String oid, SyntaxChecker syntaxChecker )
    {
    }


    /* (non-Javadoc)
     * @see org.apache.directory.server.schema.SyntaxCheckerRegistryMonitor#lookedUp(
     * org.apache.eve.schema.SyntaxChecker)
     */
    public void lookedUp( String oid, SyntaxChecker syntaxChecker )
    {
    }


    /* (non-Javadoc)
     * @see org.apache.directory.server.schema.SyntaxCheckerRegistryMonitor#lookupFailed(
     * java.lang.String, javax.naming.NamingException)
     */
    public void lookupFailed( String oid, NamingException fault )
    {
        if ( fault != null )
        {
            log.warn( "Failed to look up the syntax checker: " + oid, fault );
        }
    }


    /* (non-Javadoc)
     * @see org.apache.directory.server.schema.SyntaxCheckerRegistryMonitor#registerFailed(
     * org.apache.eve.schema.SyntaxChecker, javax.naming.NamingException)
     */
    public void registerFailed( String oid, SyntaxChecker syntaxChecker, NamingException fault )
    {
        if ( fault != null )
        {
            log.warn( "Failed to register a syntax checker: " + oid, fault );
        }
    }
}
