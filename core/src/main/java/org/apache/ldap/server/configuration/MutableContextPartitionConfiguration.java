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

import java.util.Set;

import javax.naming.directory.Attributes;

import org.apache.ldap.server.ContextPartition;

/**
 * A mutable version of {@link ContextPartitionConfiguration}.
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 * @version $Rev$, $Date$
 */
public class MutableContextPartitionConfiguration extends
        ContextPartitionConfiguration
{
    /**
     * Creates a new instance.
     */
    public MutableContextPartitionConfiguration()
    {
    }

    public void setIndexedAttributes( Set indexedAttributes )
    {
        super.setIndexedAttributes( indexedAttributes );
    }

    public void setContextPartition( ContextPartition partition )
    {
        super.setContextPartition( partition );
    }

    public void setRootEntry( Attributes rootEntry )
    {
        super.setRootEntry( rootEntry );
    }

    public void setSuffix( String suffix )
    {
        super.setSuffix( suffix );
    }
}
