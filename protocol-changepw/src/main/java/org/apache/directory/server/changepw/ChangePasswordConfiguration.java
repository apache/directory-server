/*
 *   Copyright 2005 The Apache Software Foundation
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
package org.apache.directory.server.changepw;


import java.util.ArrayList;
import java.util.Dictionary;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import javax.security.auth.kerberos.KerberosPrincipal;

import org.apache.directory.server.core.DirectoryService;
import org.apache.directory.server.core.configuration.ConfigurationException;
import org.apache.directory.server.kerberos.shared.crypto.encryption.EncryptionType;
import org.apache.directory.server.protocol.shared.LoadStrategy;
import org.apache.directory.server.protocol.shared.ServiceConfiguration;


public class ChangePasswordConfiguration extends ServiceConfiguration
{
    private static final long serialVersionUID = 3509208713288140629L;

    /** the prop key const for changepw.principal */
    private static final String PRINCIPAL_KEY = "principal";

    /** the default change password principal */
    private static final String DEFAULT_PRINCIPAL = "kadmin/changepw@EXAMPLE.COM";

    /** the default change password base DN */
    public static final String CHANGEPW_ENTRY_BASEDN = "ou=users,dc=example,dc=com";

    /** the prop key const for primary.realm */
    private static final String REALM_KEY = "realm";

    /** the default change password realm */
    private static final String DEFAULT_REALM = "EXAMPLE.COM";

    /** the default change password port */
    private static final String DEFAULT_IP_PORT = "464";

    /** the prop key const for encryption.types */
    private static final String ENCRYPTION_TYPES_KEY = "encryption.types";

    /** the default encryption types */
    public static final String[] DEFAULT_ENCRYPTION_TYPES = new String[]
        { "des-cbc-md5" };

    /** the prop key const for allowable.clockskew */
    private static final String ALLOWABLE_CLOCKSKEW_KEY = "allowable.clockskew";

    /** the default changepw buffer size */
    private static final long DEFAULT_ALLOWABLE_CLOCKSKEW = 5 * MINUTE;

    /** the prop key const for empty.addresses.allowed */
    private static final String EMPTY_ADDRESSES_ALLOWED_KEY = "empty.addresses.allowed";

    /** the default empty addresses */
    private static final boolean DEFAULT_EMPTY_ADDRESSES_ALLOWED = true;

    /** the prop key constants for password policy */
    public static final String PASSWORD_LENGTH_KEY = "password.length";
    public static final String CATEGORY_COUNT_KEY = "category.count";
    public static final String TOKEN_SIZE_KEY = "token.size";

    /** the default change password password policies */
    public static final int DEFAULT_PASSWORD_LENGTH = 6;
    public static final int DEFAULT_CATEGORY_COUNT = 3;
    public static final int DEFAULT_TOKEN_SIZE = 3;

    private static final String DEFAULT_PID = "org.apache.changepw";
    private static final String DEFAULT_NAME = "Apache Change Password Service";
    private static final String DEFAULT_PREFIX = "changepw.";

    private EncryptionType[] encryptionTypes;


    /**
     * Creates a new instance with default settings.
     */
    public ChangePasswordConfiguration()
    {
        this( getDefaultConfig(), LoadStrategy.LDAP );
    }


    /**
     * Creates a new instance with default settings that operates on the
     * {@link DirectoryService} with the specified ID.
     */
    public ChangePasswordConfiguration(String instanceId)
    {
        this( getDefaultConfig(), LoadStrategy.LDAP );
        setInstanceId( instanceId );
    }


    public ChangePasswordConfiguration(Map properties)
    {
        this( properties, LoadStrategy.LDAP );
    }


    public ChangePasswordConfiguration(Map properties, int strategy)
    {
        if ( properties == null )
        {
            configuration = getDefaultConfig();
        }
        else
        {
            loadProperties( DEFAULT_PREFIX, properties, strategy );
        }

        int port = getPort();

        if ( port < 1 || port > 0xFFFF )
        {
            throw new ConfigurationException( "Invalid value:  " + IP_PORT_KEY + "=" + port );
        }

        prepareEncryptionTypes();
    }


    public static Map getDefaultConfig()
    {
        Map defaults = new HashMap();

        defaults.put( SERVICE_PID, DEFAULT_PID );
        defaults.put( IP_PORT_KEY, DEFAULT_IP_PORT );

        return defaults;
    }


    public boolean isDifferent( Dictionary config )
    {
        int port = getPort();

        if ( port == Integer.parseInt( ( String ) config.get( IP_PORT_KEY ) ) )
        {
            return false;
        }

        return true;
    }


    public String getName()
    {
        return DEFAULT_NAME;
    }


    public int getPort()
    {
        String key = IP_PORT_KEY;

        if ( configuration.containsKey( key ) )
        {
            return Integer.parseInt( get( key ) );
        }

        return Integer.parseInt( DEFAULT_IP_PORT );
    }


    public String getPrimaryRealm()
    {
        String key = REALM_KEY;

        if ( configuration.containsKey( key ) )
        {
            return get( key );
        }

        return DEFAULT_REALM;
    }


    public EncryptionType[] getEncryptionTypes()
    {
        return encryptionTypes;
    }


    public Map getProperties()
    {
        // Request that the krb5key value be returned as binary
        configuration.put( "java.naming.ldap.attributes.binary", "krb5Key" );

        return configuration;
    }


    public long getClockSkew()
    {
        String key = ALLOWABLE_CLOCKSKEW_KEY;

        if ( configuration.containsKey( key ) )
        {
            return MINUTE * Long.parseLong( get( key ) );
        }

        return DEFAULT_ALLOWABLE_CLOCKSKEW;
    }


    public int getBufferSize()
    {
        String key = BUFFER_SIZE_KEY;

        if ( configuration.containsKey( key ) )
        {
            return Integer.parseInt( get( key ) );
        }

        return DEFAULT_BUFFER_SIZE;
    }


    public KerberosPrincipal getChangepwPrincipal()
    {
        String key = PRINCIPAL_KEY;

        if ( configuration.containsKey( key ) )
        {
            return new KerberosPrincipal( get( key ) );
        }

        return new KerberosPrincipal( DEFAULT_PRINCIPAL );
    }


    public String getEntryBaseDn()
    {
        String key = ENTRY_BASEDN_KEY;

        if ( configuration.containsKey( key ) )
        {
            return get( key );
        }

        return CHANGEPW_ENTRY_BASEDN;
    }


    public boolean isEmptyAddressesAllowed()
    {
        String key = EMPTY_ADDRESSES_ALLOWED_KEY;

        if ( configuration.containsKey( key ) )
        {
            return "true".equalsIgnoreCase( get( key ) );
        }

        return DEFAULT_EMPTY_ADDRESSES_ALLOWED;
    }


    public int getPasswordLengthPolicy()
    {
        String key = PASSWORD_LENGTH_KEY;

        if ( configuration.containsKey( key ) )
        {
            return Integer.parseInt( get( key ) );
        }

        return DEFAULT_PASSWORD_LENGTH;
    }


    public int getCategoryCountPolicy()
    {
        String key = CATEGORY_COUNT_KEY;

        if ( configuration.containsKey( key ) )
        {
            return Integer.parseInt( get( key ) );
        }

        return DEFAULT_CATEGORY_COUNT;
    }


    public int getTokenSizePolicy()
    {
        String key = TOKEN_SIZE_KEY;

        if ( configuration.containsKey( key ) )
        {
            return Integer.parseInt( get( key ) );
        }

        return DEFAULT_TOKEN_SIZE;
    }


    private void prepareEncryptionTypes()
    {
        String[] encryptionTypeStrings = null;

        String key = ENCRYPTION_TYPES_KEY;

        if ( configuration.containsKey( key ) )
        {
            encryptionTypeStrings = ( get( key ) ).split( "\\s" );
        }
        else
        {
            encryptionTypeStrings = DEFAULT_ENCRYPTION_TYPES;
        }

        List encTypes = new ArrayList();

        for ( int ii = 0; ii < encryptionTypeStrings.length; ii++ )
        {
            String enc = encryptionTypeStrings[ii];

            Iterator it = EncryptionType.VALUES.iterator();

            while ( it.hasNext() )
            {
                EncryptionType type = ( EncryptionType ) it.next();

                if ( type.toString().equalsIgnoreCase( enc ) )
                {
                    encTypes.add( type );
                }
            }
        }

        encryptionTypes = ( EncryptionType[] ) encTypes.toArray( new EncryptionType[encTypes.size()] );
    }
}
