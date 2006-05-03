/*
 *   Copyright 2006 The Apache Software Foundation
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
package org.apache.directory.server.core.trigger;


import javax.naming.Name;
import javax.naming.NamingException;
import javax.naming.directory.Attributes;

import org.apache.directory.server.core.DirectoryServiceConfiguration;
import org.apache.directory.server.core.authn.LdapPrincipal;
import org.apache.directory.server.core.configuration.InterceptorConfiguration;
import org.apache.directory.server.core.interceptor.BaseInterceptor;
import org.apache.directory.server.core.interceptor.InterceptorChain;
import org.apache.directory.server.core.interceptor.NextInterceptor;
import org.apache.directory.server.core.invocation.Invocation;
import org.apache.directory.server.core.invocation.InvocationStack;
import org.apache.directory.server.core.jndi.ServerContext;
import org.apache.directory.server.core.partition.DirectoryPartitionNexusProxy;
import org.apache.directory.server.core.schema.AttributeTypeRegistry;
import org.apache.directory.server.core.schema.ConcreteNameComponentNormalizer;
import org.apache.directory.shared.ldap.name.DnParser;
import org.apache.directory.shared.ldap.trigger.TriggerSpecificationParser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * The Trigger Service based on the Trigger Specification.
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 * @version $Rev:$
 */
public class TriggerService extends BaseInterceptor
{
    /** the logger for this class */
    private static final Logger log = LoggerFactory.getLogger( TriggerService.class );
    
    /** the entry trigger attribute string: entryTrigger */
    private static final String ENTRY_TRIGGER_ATTR = "entryTrigger";

    /**
     * the multivalued operational attribute used to track the prescriptive
     * trigger subentries that apply to an entry
     */
    private static final String TRIGGER_SUBENTRIES_ATTR = "triggerSubentries";


    /** a triggerSpecCache that responds to add, delete, and modify attempts */
    private TriggerSpecCache triggerSpecCache;
    /** a normalizing Trigger Specification parser */
    private TriggerSpecificationParser triggerParser;
    /** a normalizing Distinguished Name parser */
    private DnParser dnParser;
    /** the interceptor chain */
    private InterceptorChain chain;
    /** the attribute type registry */
    private AttributeTypeRegistry attrRegistry;
    /** whether or not this interceptor is activated */
    private boolean enabled = false;


    /**
     * Initializes this interceptor based service by getting a handle on the nexus.
     *
     * @param dirServCfg the ContextFactory configuration for the server
     * @param intCfg the interceptor configuration
     * @throws NamingException if there are problems during initialization
     */
    public void init( DirectoryServiceConfiguration dirServCfg, InterceptorConfiguration intCfg ) throws NamingException
    {
        super.init( dirServCfg, intCfg );
        triggerSpecCache = new TriggerSpecCache( dirServCfg );
        attrRegistry = dirServCfg.getGlobalRegistries().getAttributeTypeRegistry();
        triggerParser = new TriggerSpecificationParser( new ConcreteNameComponentNormalizer( attrRegistry ) );
        dnParser = new DnParser( new ConcreteNameComponentNormalizer( attrRegistry ) );
        chain = dirServCfg.getInterceptorChain();
    }


    public void add( NextInterceptor next, String upName, Name normName, Attributes entry ) throws NamingException
    {
        // Access the principal requesting the operation
        Invocation invocation = InvocationStack.getInstance().peek();
        LdapPrincipal principal = ( ( ServerContext ) invocation.getCaller() ).getPrincipal();
        Name userName = dnParser.parse( principal.getName() );

        // Bypass trigger code if we are disabled
        if ( !enabled )
        {
            next.add( upName, normName, entry );
            return;
        }
        
        /**
         * 
         */
        
    }


    public void delete( NextInterceptor next, Name name ) throws NamingException
    {
        // Access the principal requesting the operation
        Invocation invocation = InvocationStack.getInstance().peek();
        DirectoryPartitionNexusProxy proxy = invocation.getProxy();
        Attributes entry = proxy.lookup( name, DirectoryPartitionNexusProxy.LOOKUP_BYPASS );
        LdapPrincipal principal = ( ( ServerContext ) invocation.getCaller() ).getPrincipal();
        Name userName = dnParser.parse( principal.getName() );

        // Bypass trigger code if we are disabled
        if ( !enabled )
        {
            next.delete( name );
            return;
        }
        
        /**
         * 
         */

    }

}
