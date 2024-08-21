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

import org.apache.directory.api.ldap.model.exception.LdapException;
import org.apache.directory.api.ldap.model.exception.LdapUnwillingToPerformException;
import org.apache.directory.api.ldap.model.ldif.LdifEntry;
import org.apache.directory.api.ldap.model.message.ResultCodeEnum;
import org.apache.directory.server.core.api.DirectoryService;
import org.apache.directory.server.core.api.LdapPrincipal;
import org.apache.directory.server.core.api.changelog.ChangeLog;
import org.apache.directory.server.core.api.changelog.ChangeLogEvent;
import org.apache.directory.server.core.api.changelog.ChangeLogSearchEngine;
import org.apache.directory.server.core.api.changelog.ChangeLogStore;
import org.apache.directory.server.core.api.changelog.SearchableChangeLogStore;
import org.apache.directory.server.core.api.changelog.Tag;
import org.apache.directory.server.core.api.changelog.TagSearchEngine;
import org.apache.directory.server.core.api.changelog.TaggableChangeLogStore;
import org.apache.directory.server.core.api.changelog.TaggableSearchableChangeLogStore;
import org.apache.directory.server.core.api.partition.Partition;
import org.apache.directory.server.i18n.I18n;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * The default ChangeLog service implementation. It stores operations 
 * in memory.
 * 
 * Entries are stored into a dedicated partition, named ou=changelog, under which
 * we have two other sub-entries : ou=tags and ou= revisions :
 * 
 *  ou=changelog
 *    |
 *    +-- ou=revisions
 *    |
 *    +-- ou=tags
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 */
public class DefaultChangeLog implements ChangeLog
{
    /** The class logger */
    private static final Logger LOG = LoggerFactory.getLogger( DefaultChangeLog.class );

    /** Tells if the service is activated or not */
    private boolean enabled;

    /** The latest tag set */
    private Tag latest;

    /** 
     * The default store is a InMemory store.
     **/
    private ChangeLogStore store;

    /** A volatile flag used to avoid store switching when in use */
    private volatile boolean storeInitialized = false;

    /** A flag used to tell if the changeLog system is visible by the clients */
    private boolean exposed;

    // default values for ChangeLogStorePartition containers
    private static final String DEFAULT_PARTITION_SUFFIX = "ou=changelog";
    private static final String DEFAULT_REV_CONTAINER_NAME = "ou=revisions";
    private static final String DEFAULT_TAG_CONTAINER_NAME = "ou=tags";

    // default values for ChangeLogStorePartition containers
    private String partitionSuffix = DEFAULT_PARTITION_SUFFIX;
    private String revContainerName = DEFAULT_REV_CONTAINER_NAME;
    private String tagContainerName = DEFAULT_TAG_CONTAINER_NAME;


    /**
     * {@inheritDoc}
     */
    @Override
    public ChangeLogStore getChangeLogStore()
    {
        return store;
    }


    /**
     * {@inheritDoc}
     * 
     * If there is an existing changeLog store, we don't switch it 
     */
    @Override
    public void setChangeLogStore( ChangeLogStore store )
    {
        if ( storeInitialized )
        {
            LOG.error( I18n.err( I18n.ERR_16000_CANNOT_SET_CHANGE_LOG_STORE ) );
        }
        else
        {
            this.store = store;
        }
    }


    /**
     * {@inheritDoc}
     */
    @Override
public long getCurrentRevision() throws LdapException
    {
        synchronized ( store )
        {
            return store.getCurrentRevision();
        }
    }


    /**
     * {@inheritDoc}
     */
    @Override
    public ChangeLogEvent log( LdapPrincipal principal, LdifEntry forward, LdifEntry reverse ) throws LdapException
    {
        if ( !enabled )
        {
            throw new IllegalStateException( I18n.err( I18n.ERR_16001_CHANGE_LOG_NOT_ENABLED ) );
        }

        try
        {
            return store.log( principal, forward, reverse );
        }
        catch ( Exception e )
        {
            throw new LdapUnwillingToPerformException( ResultCodeEnum.UNWILLING_TO_PERFORM, e.getMessage(), e );
        }
    }


    /**
     * {@inheritDoc}
     */
    @Override
    public ChangeLogEvent log( LdapPrincipal principal, LdifEntry forward, List<LdifEntry> reverses )
        throws LdapException
    {
        if ( !enabled )
        {
            throw new IllegalStateException( I18n.err( I18n.ERR_16001_CHANGE_LOG_NOT_ENABLED ) );
        }

        try
        {
            return store.log( principal, forward, reverses );
        }
        catch ( Exception e )
        {
            throw new LdapUnwillingToPerformException( ResultCodeEnum.UNWILLING_TO_PERFORM, e.getMessage(), e );
        }
    }


    /**
     * {@inheritDoc}
     */
    @Override
    public boolean isLogSearchSupported()
    {
        return store instanceof SearchableChangeLogStore;
    }


    /**
     * {@inheritDoc}
     */
    @Override
    public boolean isTagSearchSupported()
    {
        return store instanceof TaggableSearchableChangeLogStore;
    }


    /**
     * {@inheritDoc}
     */
    @Override
    public boolean isTagStorageSupported()
    {
        return store instanceof TaggableChangeLogStore;
    }


    /**
     * {@inheritDoc}
     */
    @Override
    public ChangeLogSearchEngine getChangeLogSearchEngine()
    {
        if ( isLogSearchSupported() )
        {
            return ( ( SearchableChangeLogStore ) store ).getChangeLogSearchEngine();
        }

        throw new UnsupportedOperationException( I18n.err( I18n.ERR_16002_CHANGLE_LOG_STORE_CANNOT_BE_SEARCHED ) );
    }


    /**
     * {@inheritDoc}
     */
    @Override
    public TagSearchEngine getTagSearchEngine()
    {
        if ( isTagSearchSupported() )
        {
            return ( ( TaggableSearchableChangeLogStore ) store ).getTagSearchEngine();
        }

        throw new UnsupportedOperationException( I18n.err( I18n.ERR_16002_CHANGLE_LOG_STORE_CANNOT_BE_SEARCHED ) );
    }


    /**
     * {@inheritDoc}
     */
    @Override
    public Tag tag( long revision, String description ) throws Exception
    {
        if ( revision < 0 )
        {
            throw new IllegalArgumentException( I18n.err( I18n.ERR_00023_NEGATIVE_REVISION ) );
        }

        if ( revision > store.getCurrentRevision() )
        {
            throw new IllegalArgumentException( I18n.err( I18n.ERR_16003_REVISION_TOO_HIGH ) );
        }

        if ( store instanceof TaggableChangeLogStore )
        {
            latest = ( ( TaggableChangeLogStore ) store ).tag( revision );
            return latest;
        }

        latest = new Tag( revision, description );
        return latest;
    }


    /**
     * {@inheritDoc}
     */
    @Override
    public Tag tag( long revision ) throws Exception
    {
        return tag( revision, null );
    }


    /**
     * {@inheritDoc}
     */
    @Override
    public Tag tag( String description ) throws Exception
    {
        return tag( store.getCurrentRevision(), description );
    }


    /**
     * {@inheritDoc}
     */
    @Override
    public Tag tag() throws Exception
    {
        return tag( store.getCurrentRevision(), null );
    }


    /**
     * {@inheritDoc}
     */
    @Override
    public void setEnabled( boolean enabled )
    {
        this.enabled = enabled;
    }


    /**
     * {@inheritDoc}
     */
    @Override
    public boolean isEnabled()
    {
        return enabled;
    }


    /**
     * {@inheritDoc}
     */
    @Override
    public Tag getLatest() throws LdapException
    {
        if ( latest != null )
        {
            return latest;
        }

        if ( store instanceof TaggableChangeLogStore )
        {
            latest = ( ( TaggableChangeLogStore ) store ).getLatest();
            return latest;
        }

        return null;
    }


    /**
     * Initialize the ChangeLog system. We will initialize the associated store.
     */
    @Override
    public void init( DirectoryService service ) throws LdapException
    {
        if ( enabled )
        {
            if ( store == null )
            {
                // If no store has been defined, create an In Memory store
                store = new MemoryChangeLogStore();
            }

            store.init( service );

            if ( exposed && isTagSearchSupported() )
            {
                TaggableSearchableChangeLogStore tmp = ( TaggableSearchableChangeLogStore ) store;

                tmp.createPartition( partitionSuffix, revContainerName, tagContainerName );

                Partition partition = tmp.getPartition();
                partition.initialize();

                service.addPartition( partition );
            }
        }

        // Flip the protection flag
        storeInitialized = true;
    }


    /**
     * {@inheritDoc}
     */
    @Override
    public void sync() throws LdapException
    {
        if ( enabled )
        {
            store.sync();
        }
    }


    /**
     * {@inheritDoc}
     */
    @Override
    public void destroy() throws LdapException
    {
        if ( enabled )
        {
            store.destroy();
        }

        storeInitialized = false;
    }


    /**
     * {@inheritDoc}
     */
    @Override
    public boolean isExposed()
    {
        return exposed;
    }


    /**
     * {@inheritDoc}
     */
    @Override
    public void setExposed( boolean exposed )
    {
        this.exposed = exposed;
    }


    /**
     * {@inheritDoc}
     */
    @Override
    public void setPartitionSuffix( String suffix )
    {
        this.partitionSuffix = suffix;
    }


    /**
     * {@inheritDoc}
     */
    @Override
    public void setRevisionsContainerName( String revContainerName )
    {
        this.revContainerName = revContainerName;
    }


    /**
     * {@inheritDoc}
     */
    @Override
    public void setTagsContainerName( String tagContainerName )
    {
        this.tagContainerName = tagContainerName;
    }


    /**
     * @see Object#toString()
     */
    @Override
    public String toString()
    {
        StringBuilder sb = new StringBuilder();

        sb.append( "ChangeLog tag[" ).append( latest ).append( "]\n" );
        sb.append( "    store : \n" ).append( store );

        return sb.toString();
    }
}
