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
package org.apache.eve.jndi;


import java.util.Hashtable;
import java.util.Enumeration;

import org.apache.eve.ContextPartitionConfig;
import org.apache.ldap.common.message.LockableAttributesImpl;
import org.apache.ldap.common.message.LockableAttributeImpl;


/**
 * A partition configuration builder which produces ContextPartitionConfig
 * objects from various configuration formats, namely Hashtables.
 *
 * @author <a href="mailto:directory-dev@incubator.apache.org">Apache Directory Project</a>
 * @version $Rev$
 */
public class PartitionConfigBuilder
{
    /**
     * Extracts properties from a Hashtable and builds a configuration bean for
     * a ContextPartition.
     *
     * @param id the id of the partition to extract configs for
     * @param env the Hastable containing usually JNDI environment settings
     * @return the extracted configuration object
     */
    public static ContextPartitionConfig getContextPartitionConfig( String id, Hashtable env )
    {
        StringBuffer buf = new StringBuffer();
        ContextPartitionConfig config = new ContextPartitionConfig();
        LockableAttributesImpl attrs = new LockableAttributesImpl();

        config.setId( id );

        buf.append( EnvKeys.SUFFIX ).append( id );
        config.setSuffix( ( String ) env.get(  buf.toString() ) );

        buf.setLength( 0 );
        buf.append( EnvKeys.INDICES ).append( id );
        config.setIndices( ( ( String ) env.get( buf.toString() ) ).split( " " ) );

        buf.setLength( 0 );
        buf.append( EnvKeys.ATTRIBUTES ).append( id ).append( "." );

        String keyBase = buf.toString();
        for ( Enumeration list = env.keys(); list.hasMoreElements(); )
        {
            String attrKey = ( String ) list.nextElement();

            if ( attrKey.startsWith( keyBase ) )
            {
                LockableAttributeImpl attr = new LockableAttributeImpl( attrs,
                        attrKey.substring( attrKey.length() ) ) ;
                String[] values = ( String[] ) env.get( attrKey );
                for ( int ii = 0; ii < values.length; ii++ )
                {
                    attr.add( values[ii] );
                }
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
     */
    public static ContextPartitionConfig[] getContextPartitionConfigs( Hashtable env )
    {
        final String[] ids = ( String[] ) env.get( EnvKeys.PARTITIONS );
        final ContextPartitionConfig[] configs = new ContextPartitionConfig[ids.length];

        for ( int ii = 0; ii < configs.length; ii++ )
        {
            configs[ii] = getContextPartitionConfig( ids[ii], env );
        }

        return configs;
    }
}
