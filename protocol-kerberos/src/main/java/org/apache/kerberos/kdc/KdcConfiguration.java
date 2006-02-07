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

package org.apache.kerberos.kdc;

import java.util.ArrayList;
import java.util.Dictionary;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import javax.security.auth.kerberos.KerberosPrincipal;

import org.apache.kerberos.crypto.encryption.EncryptionType;
import org.apache.ldap.server.DirectoryService;
import org.apache.ldap.server.configuration.ConfigurationException;
import org.apache.protocol.common.LoadStrategy;
import org.apache.protocol.common.ServiceConfiguration;

public class KdcConfiguration extends ServiceConfiguration
{
    private static final long serialVersionUID = 522567370475574165L;

    /** the prop key const for kdc principal */
    public static final String PRINCIPAL_KEY = "principal";

    /** the prop key const for the kdc's primary realm */
    public static final String REALM_KEY = "realm";

    /** the prop key const for encryption types */
    public static final String ENCRYPTION_TYPES_KEY = "encryption.types";

    /** the prop key const for allowable clockskew */
    public static final String ALLOWABLE_CLOCKSKEW_KEY = "allowable.clockskew";

    /** the prop key const for empty addresses allowed */
    public static final String EMPTY_ADDRESSES_ALLOWED_KEY = "empty.addresses.allowed";

    /** the prop key const for requiring encrypted timestamps */
    public static final String PA_ENC_TIMESTAMP_REQUIRED_KEY = "pa.enc.timestamp.required";

    /** the prop key const for the maximum ticket lifetime */
    public static final String TGS_MAXIMUM_TICKET_LIFETIME_KEY = "tgs.maximum.ticket.lifetime";

    /** the prop key const for the maximum renewable lifetime */
    public static final String TGS_MAXIMUM_RENEWABLE_LIFETIME_KEY = "tgs.maximum.renewable.lifetime";

    /** the prop key const for allowing forwardable tickets */
    public static final String TGS_FORWARDABLE_ALLOWED_KEY = "tgs.forwardable.allowed";

    /** the prop key const for allowing proxiable tickets */
    public static final String TGS_PROXIABLE_ALLOWED_KEY = "tgs.proxiable.allowed";

    /** the prop key const for allowing postdated tickets */
    public static final String TGS_POSTDATE_ALLOWED_KEY = "tgs.postdate.allowed";

    /** the prop key const for allowing renewable tickets */
    public static final String TGS_RENEWABLE_ALLOWED_KEY = "tgs.renewable.allowed";

    /** the default kdc principal */
    private static final String DEFAULT_PRINCIPAL = "krbtgt/EXAMPLE.COM@EXAMPLE.COM";

    /** the default kdc realm */
    private static final String DEFAULT_REALM = "EXAMPLE.COM";

    /** the default kdc port */
    private static final String DEFAULT_IP_PORT = "88";

    /** the default allowable clockskew */
    private static final long DEFAULT_ALLOWABLE_CLOCKSKEW = 5 * MINUTE;

    /** the default encryption types */
    private static final String[] DEFAULT_ENCRYPTION_TYPES = new String[] { "des-cbc-md5" };

    /** the default for allowing empty addresses */
    private static final boolean DEFAULT_EMPTY_ADDRESSES_ALLOWED = true;

    /** the default for requiring encrypted timestamps */
    private static final boolean DEFAULT_PA_ENC_TIMESTAMP_REQUIRED = true;

    /** the default for the maximum ticket lifetime */
    private static final int DEFAULT_TGS_MAXIMUM_TICKET_LIFETIME = MINUTE * 1440;

    /** the default for the maximum renewable lifetime */
    private static final int DEFAULT_TGS_MAXIMUM_RENEWABLE_LIFETIME = MINUTE * 10080;

    /** the default for allowing forwardable tickets */
    private static final boolean DEFAULT_TGS_FORWARDABLE_ALLOWED = true;

    /** the default for allowing proxiable tickets */
    private static final boolean DEFAULT_TGS_PROXIABLE_ALLOWED = true;

    /** the default for allowing postdatable tickets */
    private static final boolean DEFAULT_TGS_POSTDATE_ALLOWED = true;

    /** the default for allowing renewable tickets */
    private static final boolean DEFAULT_TGS_RENEWABLE_ALLOWED = true;

    private static final String DEFAULT_PID = "org.apache.kerberos";
    private static final String DEFAULT_NAME = "Apache Kerberos Service";
    private static final String DEFAULT_PREFIX = "kdc.";

    private EncryptionType[] encryptionTypes;

    /**
     * Creates a new instance with default settings.
     */
    public KdcConfiguration()
    {
        this( getDefaultConfig(), LoadStrategy.LDAP );
    }

    /**
     * Creates a new instance with default settings that operates on the
     * {@link DirectoryService} with the specified ID.
     */
    public KdcConfiguration( String instanceId )
    {
        this( getDefaultConfig(), LoadStrategy.LDAP );
        setInstanceId( instanceId );
    }

    public KdcConfiguration( Map properties )
    {
        this( properties, LoadStrategy.LDAP );
    }

    public KdcConfiguration( Map properties, int strategy )
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

        if ( port == Integer.parseInt( (String) config.get( IP_PORT_KEY ) ) )
        {
            return false;
        }

        return true;
    }

    public String getName()
    {
        return DEFAULT_NAME;
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

    public KerberosPrincipal getKdcPrincipal()
    {
        String key = PRINCIPAL_KEY;

        if ( configuration.containsKey( key ) )
        {
            return new KerberosPrincipal( get( key ) );
        }

        return new KerberosPrincipal( DEFAULT_PRINCIPAL );
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

    public int getPort()
    {
        String key = IP_PORT_KEY;

        if ( configuration.containsKey( key ) )
        {
            return Integer.parseInt( get( key ) );
        }

        return Integer.parseInt( DEFAULT_IP_PORT );
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

    public boolean isPaEncTimestampRequired()
    {
        String key = PA_ENC_TIMESTAMP_REQUIRED_KEY;

        if ( configuration.containsKey( key ) )
        {
            return "true".equalsIgnoreCase( get( key ) );
        }

        return DEFAULT_PA_ENC_TIMESTAMP_REQUIRED;
    }

    public long getMaximumTicketLifetime()
    {
        String key = TGS_MAXIMUM_TICKET_LIFETIME_KEY;

        if ( configuration.containsKey( key ) )
        {
            return MINUTE * Long.parseLong( get( key ) );
        }

        return DEFAULT_TGS_MAXIMUM_TICKET_LIFETIME;
    }

    public long getMaximumRenewableLifetime()
    {
        String key = TGS_MAXIMUM_RENEWABLE_LIFETIME_KEY;

        if ( configuration.containsKey( key ) )
        {
            return MINUTE * Long.parseLong( get( key ) );
        }

        return DEFAULT_TGS_MAXIMUM_RENEWABLE_LIFETIME;
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

    public boolean isForwardableAllowed()
    {
        String key = TGS_FORWARDABLE_ALLOWED_KEY;

        if ( configuration.containsKey( key ) )
        {
            return "true".equalsIgnoreCase( get( key ) );
        }

        return DEFAULT_TGS_FORWARDABLE_ALLOWED;
    }

    public boolean isProxiableAllowed()
    {
        String key = TGS_PROXIABLE_ALLOWED_KEY;

        if ( configuration.containsKey( key ) )
        {
            return "true".equalsIgnoreCase( get( key ) );
        }

        return DEFAULT_TGS_PROXIABLE_ALLOWED;
    }

    public boolean isPostdateAllowed()
    {
        String key = TGS_POSTDATE_ALLOWED_KEY;

        if ( configuration.containsKey( key ) )
        {
            return "true".equalsIgnoreCase( get( key ) );
        }

        return DEFAULT_TGS_POSTDATE_ALLOWED;
    }

    public boolean isRenewableAllowed()
    {
        String key = TGS_RENEWABLE_ALLOWED_KEY;

        if ( configuration.containsKey( key ) )
        {
            return "true".equalsIgnoreCase( get( key ) );
        }

        return DEFAULT_TGS_RENEWABLE_ALLOWED;
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

        for ( int i = 0; i < encryptionTypeStrings.length; i++ )
        {
            String enc = encryptionTypeStrings[ i ];

            Iterator it = EncryptionType.VALUES.iterator();

            while ( it.hasNext() )
            {
                EncryptionType type = (EncryptionType) it.next();

                if ( type.toString().equalsIgnoreCase( enc ) )
                {
                    encTypes.add( type );
                }
            }
        }

        encryptionTypes = (EncryptionType[]) encTypes.toArray( new EncryptionType[ encTypes.size() ] );
    }
}
