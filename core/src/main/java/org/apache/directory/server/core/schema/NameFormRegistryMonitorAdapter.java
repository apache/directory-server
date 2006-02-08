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


import org.apache.directory.shared.ldap.schema.NameForm;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * A simple do nothing monitor adapter for NameFormRegistries.  Note for
 * safty exception based callback print the stack tract to stderr.
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 * @version $Rev$
 */
public class NameFormRegistryMonitorAdapter implements NameFormRegistryMonitor
{
    private static final Logger log = LoggerFactory.getLogger( NameFormRegistryMonitorAdapter.class );
    
    public void registered( NameForm nameForm )
    {
    }


    public void lookedUp( NameForm nameForm )
    {
    }


    public void lookupFailed( String oid, Throwable fault )
    {
        if ( fault != null )
        {
            log.warn( "Failed to lokk up the name form: " + oid, fault );
        }
    }


    public void registerFailed( NameForm nameForm, Throwable fault )
    {
        if ( fault != null )
        {
            log.warn( "Failed to register a name form: " + nameForm, fault );
        }
    }
}
