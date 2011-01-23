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
import org.apache.directory.server.core.LdapPrincipal;
import org.apache.directory.server.core.partition.Partition;
import org.apache.directory.server.i18n.I18n;
import org.apache.directory.shared.ldap.model.exception.LdapException;
import org.apache.directory.shared.ldap.model.exception.LdapUnwillingToPerformException;
import org.apache.directory.shared.ldap.model.ldif.LdifEntry;
import org.apache.directory.shared.ldap.model.message.ResultCodeEnum;
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
    public ChangeLogStore getChangeLogStore()
    {
        return store;
    }


    /**
     * {@inheritDoc}
     * 
     * If there is an existing changeLog store, we don't switch it 
     */
    public void setChangeLogStore( ChangeLogStore store )
    {
        if ( storeInitialized )
        {
            LOG.error( I18n.err( I18n.ERR_29 ) );
        }
        else
        {
            this.store = store;
        }
    }


    /**
     * {@inheritDoc}
     */
    public long getCurrentRevision() throws LdapException
    {
        synchronized( store )
        {
            return store.getCurrentRevision();
        }
    }


    /**
     * {@inheritDoc}
     */
    public ChangeLogEvent log( LdapPrincipal principal, LdifEntry forward, LdifEntry reverse ) throws LdapException
    {
        if ( !enabled )
        {
            throw new IllegalStateException( I18n.err( I18n.ERR_236 ) );
        }

        try
        {
            ChangeLogEvent event = store.log( principal, forward, reverse );
            
            return event;
        }
        catch ( Exception e )
        {
            throw new LdapUnwillingToPerformException( ResultCodeEnum.UNWILLING_TO_PERFORM, e.getMessage() );
        }
    }


    /**
     * {@inheritDoc}
     */
    public ChangeLogEvent log( LdapPrincipal principal, LdifEntry forward, List<LdifEntry> reverses ) throws LdapException
    {
        if ( !enabled )
        {
            throw new IllegalStateException( I18n.err( I18n.ERR_236 ) );
        }

        try
        {
            return store.log( principal, forward, reverses );
        }
        catch ( Exception e )
        {
            throw new LdapUnwillingToPerformException( ResultCodeEnum.UNWILLING_TO_PERFORM, e.getMessage() );
        }
    }


    /**
     * {@inheritDoc}
     */
    public boolean isLogSearchSupported()
    {
        return store instanceof SearchableChangeLogStore;
    }


    /**
     * {@inheritDoc}
     */
    public boolean isTagSearchSupported()
    {
        return store instanceof TaggableSearchableChangeLogStore;
    }


    /**
     * {@inheritDoc}
     */
    public boolean isTagStorageSupported()
    {
        return store instanceof TaggableChangeLogStore;
    }


    /**
     * {@inheritDoc}
     */
    public ChangeLogSearchEngine getChangeLogSearchEngine()
    {
        if ( isLogSearchSupported() )
        {
            return ( ( SearchableChangeLogStore ) store ).getChangeLogSearchEngine();
        }

        throw new UnsupportedOperationException( I18n.err( I18n.ERR_237 ) );
    }


    /**
     * {@inheritDoc}
     */
    public TagSearchEngine getTagSearchEngine()
    {
        if ( isTagSearchSupported() )
        {
            return ( ( TaggableSearchableChangeLogStore ) store ).getTagSearchEngine();
        }

        throw new UnsupportedOperationException( I18n.err( I18n.ERR_238 ) );
    }


    /**
     * {@inheritDoc}
     */
    public Tag tag( long revision, String description ) throws Exception
    {
        if ( revision < 0 )
        {
            throw new IllegalArgumentException( I18n.err( I18n.ERR_239 ) );
        }

        if ( revision > store.getCurrentRevision() )
        {
            throw new IllegalArgumentException( I18n.err( I18n.ERR_240 ) );
        }

        if ( store instanceof TaggableChangeLogStore )
        {
            return latest = ( ( TaggableChangeLogStore ) store ).tag( revision );
        }

        return latest = new Tag( revision, description );
    }


    /**
     * {@inheritDoc}
     */
    public Tag tag( long revision ) throws Exception
    {
        return tag( revision, null );
    }


    /**
     * {@inheritDoc}
     */
    public Tag tag( String description ) throws Exception
    {
        return tag( store.getCurrentRevision(), description );
    }


    /**
     * {@inheritDoc}
     */
    public Tag tag() throws Exception
    {
        return tag( store.getCurrentRevision(), null );
    }


    /**
     * {@inheritDoc}
     */
    public void setEnabled( boolean enabled )
    {
        this.enabled = enabled;
    }


    /**
     * {@inheritDoc}
     */
    public boolean isEnabled()
    {
        return enabled;
    }


    /**
     * {@inheritDoc}
     */
    public Tag getLatest() throws LdapException
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


    /**
     * Initialize the ChangeLog system. We will initialize the associated store.
     */
    public void init( DirectoryService service ) throws Exception
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
                partition.initialize( );

                service.addPartition( partition );
            }
        }
        
        // Flip the protection flag
        storeInitialized = true;
    }


    /**
     * {@inheritDoc}
     */
    public void sync() throws Exception
    {
        if ( enabled )
        {
            store.sync();
        }
    }


    /**
     * {@inheritDoc}
     */
    public void destroy() throws Exception
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
    public boolean isExposed()
    {
        return exposed;
    }


    /**
     * {@inheritDoc}
     */
    public void setExposed( boolean exposed )
    {
        this.exposed = exposed;
    }


    /**
     * {@inheritDoc}
     */
    public void setPartitionSuffix( String suffix )
    {
        this.partitionSuffix = suffix;
    }


    /**
     * {@inheritDoc}
     */
    public void setRevisionsContainerName( String revContainerName )
    {
        this.revContainerName = revContainerName;
    }


    /**
     * {@inheritDoc}
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
