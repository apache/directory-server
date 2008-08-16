/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.directory.server.core.changelog;


import java.util.List;

import org.apache.directory.server.core.DirectoryService;
import org.apache.directory.server.core.authn.LdapPrincipal;
import org.apache.directory.server.core.partition.Partition;
import org.apache.directory.shared.ldap.ldif.LdifEntry;


/**
 * The default ChangeLog service implementation.
 *
 * @org.apache.xbean.XBean
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 * @version $Rev$, $Date$
 */
public class DefaultChangeLog implements ChangeLog
{
    private boolean enabled;
    private Tag latest;
    private ChangeLogStore store = new MemoryChangeLogStore();

    private boolean exposeChangeLog;

    // default values for ChangeLogStorePartition containers
    private String partitionSuffix = "ou=changelog";
    private String revContainerName = "ou=revisions";
    private String tagContainerName = "ou=tags";


    public ChangeLogStore getChangeLogStore()
    {
        return store;
    }


    public void setChangeLogStore( ChangeLogStore store )
    {
        this.store = store;
    }


    public long getCurrentRevision() throws Exception
    {
        return store.getCurrentRevision();
    }


    /**
     * {@inheritDoc}
     */
    public ChangeLogEvent log( LdapPrincipal principal, LdifEntry forward, LdifEntry reverse ) throws Exception
    {
        if ( !enabled )
        {
            throw new IllegalStateException( "The ChangeLog has not been enabled." );
        }

        return store.log( principal, forward, reverse );
    }


    /**
     * {@inheritDoc}
     */
    public ChangeLogEvent log( LdapPrincipal principal, LdifEntry forward, List<LdifEntry> reverses ) throws Exception
    {
        if ( !enabled )
        {
            throw new IllegalStateException( "The ChangeLog has not been enabled." );
        }

        return store.log( principal, forward, reverses );
    }


    public boolean isLogSearchSupported()
    {
        return store instanceof SearchableChangeLogStore;
    }


    public boolean isTagSearchSupported()
    {
        return store instanceof TaggableSearchableChangeLogStore;
    }


    public boolean isTagStorageSupported()
    {
        return store instanceof TaggableChangeLogStore;
    }


    public ChangeLogSearchEngine getChangeLogSearchEngine()
    {
        if ( isLogSearchSupported() )
        {
            return ( ( SearchableChangeLogStore ) store ).getChangeLogSearchEngine();
        }

        throw new UnsupportedOperationException(
            "The underlying changelog store does not support searching through it's logs" );
    }


    public TagSearchEngine getTagSearchEngine()
    {
        if ( isTagSearchSupported() )
        {
            return ( ( TaggableSearchableChangeLogStore ) store ).getTagSearchEngine();
        }

        throw new UnsupportedOperationException(
            "The underlying changelog store does not support searching through it's tags" );
    }


    public Tag tag( long revision, String description ) throws Exception
    {
        if ( revision < 0 )
        {
            throw new IllegalArgumentException( "revision must be greater than or equal to 0" );
        }

        if ( revision > store.getCurrentRevision() )
        {
            throw new IllegalArgumentException( "revision must be less than or equal to the current revision" );
        }

        if ( store instanceof TaggableChangeLogStore )
        {
            return latest = ( ( TaggableChangeLogStore ) store ).tag( revision );
        }

        return latest = new Tag( revision, description );
    }


    public Tag tag( long revision ) throws Exception
    {
        return tag( revision, null );
    }


    public Tag tag( String description ) throws Exception
    {
        return tag( store.getCurrentRevision(), description );
    }


    public Tag tag() throws Exception
    {
        return tag( store.getCurrentRevision(), null );
    }


    public void setEnabled( boolean enabled )
    {
        this.enabled = enabled;
    }


    public boolean isEnabled()
    {
        return enabled;
    }


    public Tag getLatest() throws Exception
    {
        if ( latest != null )
        {
            return latest;
        }

        if ( store instanceof TaggableChangeLogStore )
        {
            return latest = ( ( TaggableChangeLogStore ) store ).getLatest();
        }

        return null;
    }


    public void init( DirectoryService service ) throws Exception
    {
        if ( enabled )
        {
            store.init( service );

            if ( exposeChangeLog && isTagSearchSupported() )
            {
                Partition partition = ( ( TaggableSearchableChangeLogStore ) store ).getPartition( partitionSuffix, revContainerName, tagContainerName );
                partition.init( service );

                service.addPartition( partition );
            }
        }
    }


    public void sync() throws Exception
    {
        if ( enabled )
        {
            store.sync();
        }
    }


    public void destroy() throws Exception
    {
        if ( enabled )
        {
            store.destroy();
        }
    }


    /**
     * @see ChangeLog#isExposeChangeLog()
     */
    public boolean isExposeChangeLog()
    {
        return exposeChangeLog;
    }


    /**
     * @see ChangeLog#setExposeChangeLog(boolean)
     */
    public void setExposeChangeLog( boolean exposeChangeLog )
    {
        this.exposeChangeLog = exposeChangeLog;
    }


    /**
     * @see ChangeLog#setPartitionSuffix(String)
     */
    public void setPartitionSuffix( String suffix )
    {
        this.partitionSuffix = suffix;
    }


    /**
     * @see ChangeLog#setRevisionsContainerName(String)
     */
    public void setRevisionsContainerName( String revContainerName )
    {
        this.revContainerName = revContainerName;
    }


    /**
     * @see ChangeLog#setTagsContainerName(String)
     */
    public void setTagsContainerName( String tagContainerName )
    {
        this.tagContainerName = tagContainerName;
    }

    
    /**
     * @see Object#toString()
     */
    public String toString()
    {
        StringBuilder sb = new StringBuilder();
        
        sb.append( "ChangeLog tag[" ).append( latest ).append( "]\n" );
        sb.append( "    store : \n" ).append( store );
        
        return sb.toString();
    }
}
