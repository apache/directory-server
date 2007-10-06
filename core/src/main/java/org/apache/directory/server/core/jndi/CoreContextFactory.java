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
package org.apache.directory.server.core.jndi;


import org.apache.directory.server.core.configuration.*;
import org.apache.directory.server.core.DirectoryService;
import org.apache.directory.server.core.partition.PartitionNexusProxy;
import org.apache.directory.server.core.interceptor.context.AddContextPartitionOperationContext;
import org.apache.directory.server.core.interceptor.context.RemoveContextPartitionOperationContext;
import org.apache.directory.shared.ldap.name.LdapDN;
import org.apache.directory.shared.ldap.util.StringTools;

import javax.naming.spi.InitialContextFactory;
import javax.naming.*;
import javax.naming.ConfigurationException;
import java.util.Hashtable;


/**
 * A simplistic implementation of {@link AbstractContextFactory}.
 * This class simply extends {@link AbstractContextFactory} and leaves all
 * abstract event listener methods as empty.
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 * @version $Rev$
 */
public class CoreContextFactory implements InitialContextFactory
{
    public synchronized Context getInitialContext( Hashtable env ) throws NamingException
    {
        env = ( Hashtable<String, Object> ) env.clone();
        LdapDN principalDn = null;
        if ( env.containsKey( Context.SECURITY_PRINCIPAL ) )
        {
            if ( env.get( Context.SECURITY_PRINCIPAL ) instanceof LdapDN )
            {
                principalDn = ( LdapDN ) env.get( Context.SECURITY_PRINCIPAL );
            }
        }

        String principal = getPrincipal( env );
        byte[] credential = getCredential( env );
        String authentication = getAuthentication( env );
        String providerUrl = getProviderUrl( env );

        DirectoryService service = ( DirectoryService ) env.get( DirectoryService.JNDI_KEY );

        if ( service == null )
        {
            throw new ConfigurationException( "Cannot find directory service in environment: " + env );
        }

        if ( ! service.isStarted() )
        {
            return new DeadContext();
        }

        ServerLdapContext ctx = ( ServerLdapContext ) service.getJndiContext( principalDn, principal, credential,
                authentication, providerUrl );

        // check to make sure we have access to the specified dn in provider URL
        ctx.lookup( "" );
        return ctx;        
    }


    public static String getProviderUrl( Hashtable<String, Object> env )
    {
        String providerUrl;
        Object value;
        value = env.get( Context.PROVIDER_URL );
        if ( value == null )
        {
            value = "";
        }
        providerUrl = value.toString();

        env.put( Context.PROVIDER_URL, providerUrl );

        return providerUrl;
    }


    public static String getAuthentication( Hashtable<String, Object> env )
    {
        String authentication;
        Object value = env.get( Context.SECURITY_AUTHENTICATION );
        if ( value == null )
        {
            authentication = "none";
        }
        else
        {
            authentication = value.toString();
        }

        env.put( Context.SECURITY_AUTHENTICATION, authentication );

        return authentication;
    }


    public static byte[] getCredential( Hashtable<String, Object> env ) throws javax.naming.ConfigurationException
    {
        byte[] credential;
        Object value = env.get( Context.SECURITY_CREDENTIALS );
        if ( value == null )
        {
            credential = null;
        }
        else if ( value instanceof String )
        {
            credential = StringTools.getBytesUtf8( (String)value );
        }
        else if ( value instanceof byte[] )
        {
            credential = ( byte[] ) value;
        }
        else
        {
            throw new javax.naming.ConfigurationException( "Can't convert '" + Context.SECURITY_CREDENTIALS + "' to byte[]." );
        }

        if ( credential != null )
        {
            env.put( Context.SECURITY_CREDENTIALS, credential );
        }

        return credential;
    }


    public static String getPrincipal( Hashtable<String,Object> env )
    {
        String principal;
        Object value = env.get( Context.SECURITY_PRINCIPAL );
        if ( value == null )
        {
            principal = null;
        }
        else
        {
            principal = value.toString();
            env.put( Context.SECURITY_PRINCIPAL, principal );
        }

        return principal;
    }
}
