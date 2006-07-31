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


import java.util.HashSet;
import java.util.Set;

import javax.naming.Name;
import javax.naming.NamingException;
import javax.naming.directory.Attributes;
import javax.naming.directory.BasicAttributes;

import org.apache.directory.server.core.partition.Partition;
import org.apache.directory.server.core.partition.impl.btree.jdbm.JdbmPartition;
import org.apache.directory.server.core.schema.MatchingRuleRegistry;
import org.apache.directory.shared.ldap.exception.LdapConfigurationException;
import org.apache.directory.shared.ldap.name.LdapDN;
import org.apache.directory.shared.ldap.schema.Normalizer;


/**
 * A configuration for {@link Partition}.
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 * @version $Rev$, $Date$
 */
public class PartitionConfiguration
{
    /** The name of reserved system partition */
    public static final String SYSTEM_PARTITION_NAME = "system";
    public static final int DEFAULT_CACHE_SIZE = 10000;

    private String name;
    private int cacheSize = -1;
    private String suffix;
    private Set indexedAttributes; // Set<String> or <IndexConfiguration>
    private Attributes contextEntry = new BasicAttributes( true );
    private Partition contextPartition = new JdbmPartition();


    /**
     * Creates a new instance.
     */
    protected PartitionConfiguration()
    {
        setIndexedAttributes( new HashSet() );
    }


    /**
     * Returns user-defined name of the {@link Partition} that
     * this configuration configures.
     */
    public String getName()
    {
        return name;
    }


    /**
     * Sets user-defined name of the {@link Partition} that
     * this configuration configures.
     */
    protected void setName( String name )
    {
        name = name.trim();
        this.name = name;
    }


    /**
     * Returns the set of attribute type strings to create an index on.
     */
    public Set getIndexedAttributes()
    {
        return ConfigurationUtil.getClonedSet( indexedAttributes );
    }


    /**
     * Sets the set of attribute type strings to create an index on.
     */
    protected void setIndexedAttributes( Set indexedAttributes )
    {
        this.indexedAttributes = indexedAttributes;
    }


    /**
     * Returns the {@link Partition} that this configuration configures.
     */
    public Partition getContextPartition()
    {
        return contextPartition;
    }


    /**
     * Sets the {@link Partition} that this configuration configures.
     */
    protected void setContextPartition( Partition partition )
    {
        if ( partition == null )
        {
            throw new NullPointerException( "partition" );
        }
        this.contextPartition = partition;
    }


    /**
     * Returns root entry that will be added to the {@link Partition}
     * after it is initialized.
     */
    public Attributes getContextEntry()
    {
        return ( Attributes ) contextEntry.clone();
    }


    /**
     * Sets root entry that will be added to the {@link Partition}
     * after it is initialized.
     */
    protected void setContextEntry( Attributes rootEntry )
    {
        this.contextEntry = ( Attributes ) rootEntry.clone();
    }


    /**
     * Returns the suffix of the {@link Partition}.
     */
    public String getSuffix()
    {
        return suffix;
    }


    /**
     * Returns the normalized suffix of the {@link Partition}.
     */
    public Name getNormalizedSuffix( MatchingRuleRegistry matchingRuleRegistry ) throws NamingException
    {
        return getNormalizedSuffix( matchingRuleRegistry.lookup( "distinguishedNameMatch" ).getNormalizer() );
    }


    /**
     * Returns the normalized suffix of the {@link Partition}.
     */
    public Name getNormalizedSuffix( Normalizer normalizer ) throws NamingException
    {
        return new LdapDN( normalizer.normalize( suffix ).toString() );
    }


    /**
     * Sets the suffix of the {@link Partition}.
     */
    protected void setSuffix( String suffix ) throws NamingException
    {
        suffix = suffix.trim();
        try
        {
            new LdapDN( suffix );
        }
        catch ( NamingException e )
        {
            throw new LdapConfigurationException( "Failed to parse the suffix: " + suffix, e );
        }
        this.suffix = suffix;
    }


    /**
     * Validates this configuration.
     * 
     * @throws ConfigurationException if this configuration is not valid
     */
    public void validate()
    {
        if ( getName() == null || getName().length() == 0 )
        {
            throw new ConfigurationException( "Name is not specified." );
        }

        if ( getSuffix() == null )
        {
            throw new ConfigurationException( "Suffix is not specified." );
        }
    }


    /**
     * Used to specify the entry cache size for a partition.  Various partition
     * implementations may interpret this value in different ways: i.e. total cache 
     * size limit verses the number of entries to cache.
     * 
     * @param cacheSize the size of the cache
     */
    protected void setCacheSize( int cacheSize )
    {
        this.cacheSize = cacheSize;
    }


    /**
     * Get's the entry cache size for this partition.
     */
    public int getCacheSize()
    {
        return cacheSize;
    }
}
