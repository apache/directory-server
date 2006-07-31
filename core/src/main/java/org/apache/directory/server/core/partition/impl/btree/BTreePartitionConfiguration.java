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


import javax.naming.NamingException;

import org.apache.directory.server.core.configuration.PartitionConfiguration;


/**
 * A partition configuration containing parameters specific to the BTree 
 * based partition implementation.
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 * @version $Rev$, $Date$
 */
public class BTreePartitionConfiguration extends PartitionConfiguration
{
    /** 
     * whether or not to flush to disk immediately as opposed to flushing
     * on synch requests when a write (delete, add, modify, modifyRdn) 
     * opertations are performed
     */
    private boolean synchOnWrite = false;
    private boolean optimizerEnabled = true;


    protected void setOptimizerEnabled( boolean optimizerEnabled )
    {
        this.optimizerEnabled = optimizerEnabled;
    }


    public boolean isOptimizerEnabled()
    {
        return optimizerEnabled;
    }

    
    protected void setSynchOnWrite( boolean synchOnWrite )
    {
        this.synchOnWrite = synchOnWrite;
    }

    
    public boolean isSynchOnWrite()
    {
        return synchOnWrite;
    }
    
    
    public static BTreePartitionConfiguration convert( PartitionConfiguration config ) throws NamingException
    {
        if ( config instanceof BTreePartitionConfiguration )
        {
            return ( BTreePartitionConfiguration ) config;
        }
        
        BTreePartitionConfiguration newConfig = new BTreePartitionConfiguration();
        newConfig.setCacheSize( config.getCacheSize() );
        newConfig.setContextEntry( config.getContextEntry() );
        newConfig.setContextPartition( config.getContextPartition() );
        newConfig.setIndexedAttributes( config.getIndexedAttributes() );
        newConfig.setName( config.getName() );
        newConfig.setSuffix( config.getSuffix() );
        return newConfig;
    }
}
