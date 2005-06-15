/*
 *   @(#) $Id$
 *
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
package org.apache.ldap.server.configuration;

import org.apache.ldap.server.interceptor.Interceptor;

/**
 * A configuration for {@link Interceptor}.
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 * @version $Rev$, $Date$
 */
public class InterceptorConfiguration
{
    private String name;
    private Interceptor interceptor;

    protected InterceptorConfiguration()
    {
    }

    public Interceptor getInterceptor()
    {
        return interceptor;
    }

    protected void setInterceptor( Interceptor authenticator )
    {
        this.interceptor = authenticator;
    }

    public String getName()
    {
        return name;
    }

    protected void setName( String name )
    {
        this.name = name.trim();
    }

    public void validate()
    {
        if( name == null )
        {
            throw new ConfigurationException( "Name is not specified." );
        }
        
        if( interceptor == null )
        {
            throw new ConfigurationException( "Authenticator is not specified." );
        }
    }
}
