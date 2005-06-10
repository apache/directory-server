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

import java.io.File;
import java.util.Set;

import org.apache.ldap.server.interceptor.InterceptorChain;
import org.apache.mina.registry.ServiceRegistry;

/**
 * A mutable version of {@link ServerStartupConfiguration}.
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 * @version $Rev$, $Date$
 */
public class MutableServerStartupConfiguration extends
        ServerStartupConfiguration
{
    private static final long serialVersionUID = 515104910980600099L;

    public MutableServerStartupConfiguration()
    {
        super();
    }

    public void setAllowAnonymousAccess( boolean arg0 )
    {
        super.setAllowAnonymousAccess( arg0 );
    }

    public void setAuthenticatorConfigurations( Set arg0 )
    {
        super.setAuthenticatorConfigurations( arg0 );
    }

    public void setBootstrapSchemas( Set arg0 )
    {
        super.setBootstrapSchemas( arg0 );
    }

    public void setContextPartitionConfigurations( Set arg0 )
    {
        super.setContextPartitionConfigurations( arg0 );
    }

    public void setInterceptors( InterceptorChain arg0 )
    {
        super.setInterceptors( arg0 );
    }

    public void setTestEntries( Set arg0 )
    {
        super.setTestEntries( arg0 );
    }

    public void setWorkingDirectory( File arg0 )
    {
        super.setWorkingDirectory( arg0 );
    }

    public void setEnableKerberos( boolean enableKerberos )
    {
        super.setEnableKerberos( enableKerberos );
    }

    public void setLdapPort( int ldapPort )
    {
        super.setLdapPort( ldapPort );
    }

    public void setLdapsPort( int ldapsPort )
    {
        super.setLdapsPort( ldapsPort );
    }

    public void setMinaServiceRegistry( ServiceRegistry minaServiceRegistry )
    {
        super.setMinaServiceRegistry( minaServiceRegistry );
    }
}
