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
package org.apache.eve.schema;


import javax.naming.NamingException;

import org.apache.ldap.common.schema.Normalizer;


/**
 * An adapter for the NormalizerRegistry's monitor.
 *
 * @author <a href="mailto:directory-dev@incubator.apache.org">Apache Directory Project</a>
 * @version $Rev$
 */
public class NormalizerRegistryMonitorAdapter 
    implements NormalizerRegistryMonitor
{
    /* (non-Javadoc)
     * @see org.apache.eve.schema.NormalizerRegistryMonitor#registered(
     * org.apache.eve.schema.Normalizer)
     */
    public void registered( Normalizer normalizer)
    {
    }

    
    /* (non-Javadoc)
     * @see org.apache.eve.schema.NormalizerRegistryMonitor#lookedUp(
     * org.apache.eve.schema.Normalizer)
     */
    public void lookedUp( Normalizer normalizer )
    {
    }

    
    /* (non-Javadoc)
     * @see org.apache.eve.schema.NormalizerRegistryMonitor#lookupFailed(
     * java.lang.String, javax.naming.NamingException)
     */
    public void lookupFailed( String oid, NamingException fault )
    {
        if ( fault != null )
        {
            fault.printStackTrace();
        }
    }

    
    /* (non-Javadoc)
     * @see org.apache.eve.schema.NormalizerRegistryMonitor#registerFailed(
     * org.apache.eve.schema.Normalizer, javax.naming.NamingException)
     */
    public void registerFailed( Normalizer normalizer, NamingException fault )
    {
        if ( fault != null )
        {
            fault.printStackTrace();
        }
    }
}
