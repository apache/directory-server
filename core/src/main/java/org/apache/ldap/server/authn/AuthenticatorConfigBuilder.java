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
package org.apache.ldap.server.authn;


import java.io.FileInputStream;
import java.util.Hashtable;
import java.util.Properties;

import javax.naming.NamingException;

import org.apache.ldap.common.util.StringTools;
import org.apache.ldap.server.authn.GenericAuthenticatorConfig;
import org.apache.ldap.server.jndi.EnvKeys;


/**
 * An authenticator configuration builder which produces AuthenticatorConfig
 * objects from various configuration formats, namely Hashtables.
 *
 * @author <a href="mailto:endisd@vergenet.com">Endi S. Dewata</a>
 */
public class AuthenticatorConfigBuilder
{
    /** keep this so we do not have create empty ones over and over again */
    private final static GenericAuthenticatorConfig[] EMPTY = new GenericAuthenticatorConfig[0];


    /**
     * Extracts properties from a Hashtable and builds a configuration bean for
     * an AuthenticationService.
     *
     * @param authenticatorName the name of the authenticator to extract configs for
     * @param env the Hastable containing usually JNDI environment settings
     * @return the extracted configuration object
     * @throws javax.naming.NamingException
     */
    public static GenericAuthenticatorConfig getAuthenticatorConfig( String authenticatorName, Hashtable env )
            throws NamingException
    {
        final StringBuffer buf = new StringBuffer();
        final GenericAuthenticatorConfig config = new GenericAuthenticatorConfig();

        // --------------------------------------------------------------------
        // set id for authenticator
        // --------------------------------------------------------------------

        config.setAuthenticatorName( authenticatorName );

        // --------------------------------------------------------------------
        // set authenticator class
        // --------------------------------------------------------------------

        buf.setLength( 0 );
        buf.append( EnvKeys.AUTHENTICATOR_CLASS ).append( authenticatorName );
        String authenticatorClass = ( String ) env.get(  buf.toString() );

        if ( authenticatorClass != null )
        {
            config.setAuthenticatorClass( authenticatorClass );
        }

        // --------------------------------------------------------------------
        // set authenticator properties
        // --------------------------------------------------------------------

        buf.setLength( 0 );
        buf.append( EnvKeys.AUTHENTICATOR_PROPERTIES ).append( authenticatorName );
        String propertiesFile = ( String ) env.get(  buf.toString() );

        if ( propertiesFile != null )
        {
            try
            {
                Properties properties = config.getProperties();
                properties.load( new FileInputStream( propertiesFile ) );
                config.setProperties( properties );
            }
            catch ( Exception e )
            {
                throw new NamingException( e.getMessage() );
            }
        }

        return config;
    }


    /**
     * Extracts properties from a Hashtable and builds a set of configurations
     * bean for Authenticators.
     *
     * @param env the Hastable containing usually JNDI environment settings
     * @return all the extracted configuration objects configured
     * @throws javax.naming.NamingException
     */
    public static GenericAuthenticatorConfig[] getAuthenticatorConfigs( Hashtable env )
            throws NamingException
    {
        String idList = ( String ) env.get( EnvKeys.AUTHENTICATORS );

        // return empty array when we got nothin to work with!
        if ( idList == null || idList.trim().length() == 0 )
        {
            return EMPTY;
        }

        idList = StringTools.deepTrim( idList );
        final String[] ids = idList.split( " " );
        final GenericAuthenticatorConfig[] configs = new GenericAuthenticatorConfig[ids.length];
        for ( int ii = 0; ii < configs.length; ii++ )
        {
            configs[ii] = getAuthenticatorConfig( ids[ii], env );
        }

        return configs;
    }
}
