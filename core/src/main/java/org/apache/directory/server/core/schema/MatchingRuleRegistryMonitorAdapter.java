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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * An adapter for a MatchingRuleRegistryMonitor.
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 * @version $Rev$
 */
public class MatchingRuleRegistryMonitorAdapter implements MatchingRuleRegistryMonitor
{
    private static final Logger log = LoggerFactory.getLogger( MatchingRuleRegistryMonitorAdapter.class );


    /**
     * @see org.apache.directory.server.core.schema.MatchingRuleRegistryMonitor#registered(
     * org.apache.directory.shared.ldap.schema.MatchingRule)
     */
    public void registered( MatchingRule rule )
    {
    }


    /**
     * @see org.apache.directory.server.core.schema.MatchingRuleRegistryMonitor#lookedUp(
     * org.apache.directory.shared.ldap.schema.MatchingRule)
     */
    public void lookedUp( MatchingRule rule )
    {
    }


    /**
     * @see org.apache.directory.server.core.schema.MatchingRuleRegistryMonitor#lookupFailed(
     * java.lang.String, javax.naming.NamingException)
     */
    public void lookupFailed( String oid, NamingException fault )
    {
        if ( fault != null )
        {
            log.warn( "Failed to look up the matching rule: " + oid, fault );
        }
    }


    /**
     * @see org.apache.directory.server.core.schema.MatchingRuleRegistryMonitor#registerFailed(
     * org.apache.directory.shared.ldap.schema.MatchingRule, javax.naming.NamingException)
     */
    public void registerFailed( MatchingRule rule, NamingException fault )
    {
        if ( fault != null )
        {
            log.warn( "Failed to register a matching rule: " + rule, fault );
        }
    }
}
