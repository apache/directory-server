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
package org.apache.directory.server.core.configuration;

import org.apache.directory.server.core.partition.DirectoryPartition;
import org.apache.directory.server.core.partition.DirectoryPartitionNexus;

/**
 * A {@link Configuration} that adds a new {@link DirectoryPartition} to
 * the current {@link DirectoryPartitionNexus}.
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 * @version $Rev$, $Date$
 */
public class AddDirectoryPartitionConfiguration extends Configuration
{
    private static final long serialVersionUID = -6690435863387769527L;
    
    private final DirectoryPartitionConfiguration directoryPartitionConfiguration;

    public AddDirectoryPartitionConfiguration( DirectoryPartitionConfiguration directoryPartitionConfiguration )
    {
        if( directoryPartitionConfiguration == null )
        {
            throw new NullPointerException( "directoryPartitionConfiguration" );
        }
        
        this.directoryPartitionConfiguration = directoryPartitionConfiguration;
    }
    
    public DirectoryPartitionConfiguration getDirectoryPartitionConfiguration()
    {
        return directoryPartitionConfiguration;
    }
    
    public void validate()
    {
        directoryPartitionConfiguration.validate();
    }
}
