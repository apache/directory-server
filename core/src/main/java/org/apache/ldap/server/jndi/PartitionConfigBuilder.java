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
package org.apache.ldap.server.jndi;


import java.util.Hashtable;
import java.util.Enumeration;

import javax.naming.NamingException;
import javax.naming.directory.Attributes;

import org.apache.ldap.server.ContextPartitionConfig;
import org.apache.ldap.common.message.LockableAttributesImpl;
import org.apache.ldap.common.message.LockableAttributeImpl;
import org.apache.ldap.common.util.ArrayUtils;
import org.apache.ldap.common.util.StringTools;
import org.apache.ldap.common.name.LdapName;
import org.apache.ldap.server.ContextPartitionConfig;


/**
 * A partition configuration builder which produces ContextPartitionConfig
 * objects from various configuration formats, namely Hashtables.
 *
 * @author <a href="mailto:directory-dev@incubator.apache.org">Apache Directory Project</a>
 * @version $Rev$
 */
public class PartitionConfigBuilder
{
    /** keep this so we do not have create empty ones over and over again */
    private final static ContextPartitionConfig[] EMPTY = new ContextPartitionConfig[0];


    /**
     * Extracts properties from a Hashtable and builds a configuration bean for
     * a ContextPartition.
     *
     * @param id the id of the partition to extract configs for
     * @param env the Hastable containing usually JNDI environment settings
     * @return the extracted configuration object
     * @throws NamingException if a partition suffix is malformed
     */
    public static ContextPartitionConfig getContextPartitionConfig( String id, Hashtable env )
            throws NamingException
    {
        final StringBuffer buf = new StringBuffer();
        final ContextPartitionConfig config = new ContextPartitionConfig();
        final LockableAttributesImpl attrs = new LockableAttributesImpl();

        // --------------------------------------------------------------------
        // set id, empty attributes, and lookup the suffix for config
        // --------------------------------------------------------------------

        config.setId( id );
        config.setAttributes( attrs );
        buf.append( EnvKeys.SUFFIX ).append( id );
        String suffix = ( String ) env.get(  buf.toString() );

        if ( suffix != null )
        {
            suffix = new LdapName( suffix ).toString();
        }

        config.setSuffix( suffix );

        // --------------------------------------------------------------------
        // extract index list and set the list of indices in config
        // --------------------------------------------------------------------

        buf.setLength( 0 );
        buf.append( EnvKeys.INDICES ).append( id );
        String indexList = ( ( String ) env.get( buf.toString() ) );

        if ( indexList == null || indexList.trim().length() == 0 )
        {
            config.setIndices( ArrayUtils.EMPTY_STRING_ARRAY );
        }
        else
        {
            indexList = StringTools.deepTrim( indexList );
            config.setIndices( indexList.split( " " ) );
        }

        // --------------------------------------------------------------------
        // extract attributes and values adding them to the config
        // --------------------------------------------------------------------

        buf.setLength( 0 );
        buf.append( EnvKeys.ATTRIBUTES ).append( id );

        /*
         * before going on to extract attributes check to see if the
         * attributes base plus id has an Attributes object set.  Users
         * can programatically use Attributes objects rather than
         * wrestle with individual key value pairs.
         */
        String keyBase = buf.toString();
        if ( env.containsKey( keyBase ) )
        {
            config.setAttributes( ( Attributes ) env.get( keyBase ) );
            return config;
        }

        /*
         * looks like the environment attributes were not set programatically
         * with an Attributes object for the base.  So we now add the extra
         * '.' and go on to try and detect all the keys and their attributes.
         */
        buf.append( "." );
        keyBase = buf.toString();
        for ( Enumeration list = env.keys(); list.hasMoreElements(); )
        {
            String attrKey = ( String ) list.nextElement();

            if ( attrKey.startsWith( keyBase ) )
            {
                LockableAttributeImpl attr = new LockableAttributeImpl( attrs,
                        attrKey.substring( keyBase.length() ) ) ;
                String valueList = ( String ) env.get( attrKey );

                if ( valueList == null || valueList.trim().length() == 0 )
                {
                    // add the empty attribute
                    attrs.put( attr );
                    continue;
                }

                valueList = StringTools.deepTrim( valueList );
                String[] values = valueList.split( " " );
                for ( int ii = 0; ii < values.length; ii++ )
                {
                    attr.add( values[ii] );
                }

                attrs.put( attr );
            }
        }

        return config;
    }


    /**
     * Extracts properties from a Hashtable and builds a set of configurations
     * bean for ContextPartitions.
     *
     * @param env the Hastable containing usually JNDI environment settings
     * @return all the extracted configuration objects configured
     * @throws NamingException if a partition suffix is malformed
     */
    public static ContextPartitionConfig[] getContextPartitionConfigs( Hashtable env )
            throws NamingException
    {
        String idList = ( String ) env.get( EnvKeys.PARTITIONS );

        // return empty array when we got nothin to work with!
        if ( idList == null || idList.trim().length() == 0 )
        {
            return EMPTY;
        }

        idList = StringTools.deepTrim( idList );
        final String[] ids = idList.split( " " );
        final ContextPartitionConfig[] configs = new ContextPartitionConfig[ids.length];
        for ( int ii = 0; ii < configs.length; ii++ )
        {
            configs[ii] = getContextPartitionConfig( ids[ii], env );
        }

        return configs;
    }
}
