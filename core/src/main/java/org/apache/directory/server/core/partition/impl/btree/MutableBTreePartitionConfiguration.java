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
package org.apache.directory.server.core.partition.impl.btree;

import java.util.Set;

import javax.naming.NamingException;
import javax.naming.directory.Attributes;

import org.apache.directory.server.core.configuration.PartitionConfiguration;
import org.apache.directory.server.core.partition.Partition;


/**
 * A mutable form of {@link BTreePartitionConfiguration}.
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 * @version $Rev$, $Date$
 */
public class MutableBTreePartitionConfiguration extends BTreePartitionConfiguration
{
    public void setSynchOnWrite( boolean synchOnWrite )
    {
        super.setSynchOnWrite( synchOnWrite );
    }
    
    
    public void setName( String name )
    {
        super.setName( name );
    }


    public void setIndexedAttributes( Set indexedAttributes )
    {
        super.setIndexedAttributes( indexedAttributes );
    }


    public void setContextPartition( Partition partition )
    {
        super.setContextPartition( partition );
    }


    public void setContextEntry( Attributes rootEntry )
    {
        super.setContextEntry( rootEntry );
    }


    public void setSuffix( String suffix ) throws NamingException
    {
        super.setSuffix( suffix );
    }
    
    
    public void setCacheSize( int cacheSize )
    {
        super.setCacheSize( cacheSize );
    }
    
    
    public void setOptimizerEnabled( boolean optimizerEnabled )
    {
        super.setOptimizerEnabled( optimizerEnabled );
    }
    
    
    public static MutableBTreePartitionConfiguration getConfiguration( PartitionConfiguration config ) 
        throws NamingException
    {
        MutableBTreePartitionConfiguration newConfig = new MutableBTreePartitionConfiguration();
        newConfig.setCacheSize( config.getCacheSize() );
        newConfig.setContextEntry( config.getContextEntry() );
        newConfig.setIndexedAttributes( config.getIndexedAttributes() );
        newConfig.setName( config.getName() );
        newConfig.setSuffix( config.getSuffix() );
        newConfig.setSynchOnWrite( false );
        return newConfig;
    }
}
