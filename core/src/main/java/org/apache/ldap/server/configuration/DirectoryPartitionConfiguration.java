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


import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import javax.naming.Name;
import javax.naming.NamingException;
import javax.naming.directory.Attributes;
import javax.naming.directory.BasicAttributes;

import org.apache.ldap.common.name.LdapName;
import org.apache.ldap.common.schema.Normalizer;
import org.apache.ldap.server.partition.DirectoryPartition;
import org.apache.ldap.server.partition.Oid;
import org.apache.ldap.server.partition.impl.btree.jdbm.JdbmDirectoryPartition;
import org.apache.ldap.server.schema.MatchingRuleRegistry;


/**
 * A configuration for {@link DirectoryPartition}.
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 * @version $Rev$, $Date$
 */
public class DirectoryPartitionConfiguration
{
    /** The name of reserved system partition */
    public static final String SYSTEM_PARTITION_NAME = "system";

    private String name;
    private String suffix;
    private Set indexedAttributes; // Set<String>
    private Attributes contextEntry = new BasicAttributes( true );
    private DirectoryPartition contextPartition = new JdbmDirectoryPartition();
    
    /**
     * Creates a new instance.
     */
    protected DirectoryPartitionConfiguration()
    {
        setIndexedAttributes( new HashSet() );
    }
    
    /**
     * Returns user-defined name of the {@link DirectoryPartition} that
     * this configuration configures.
     */
    public String getName()
    {
        return name;
    }
    
    /**
     * Sets user-defined name of the {@link DirectoryPartition} that
     * this configuration configures.
     */
    protected void setName( String name )
    {
        // TODO name can be a directory name.
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
        Set newIndexedAttributes = ConfigurationUtil.getTypeSafeSet(
                indexedAttributes, String.class );

        Iterator i = newIndexedAttributes.iterator();
        while( i.hasNext() )
        {
            String attribute = ( String ) i.next();
            // TODO Attribute name must be normalized and validated
            newIndexedAttributes.add( attribute );
        }
        
        // Add default indices
        newIndexedAttributes.add( Oid.ALIAS );
        newIndexedAttributes.add( Oid.EXISTANCE );
        newIndexedAttributes.add( Oid.HIERARCHY );
        newIndexedAttributes.add( Oid.NDN );
        newIndexedAttributes.add( Oid.ONEALIAS );
        newIndexedAttributes.add( Oid.SUBALIAS );
        newIndexedAttributes.add( Oid.UPDN );

        this.indexedAttributes = newIndexedAttributes;
    }
    
    /**
     * Returns the {@link DirectoryPartition} that this configuration configures.
     */
    public DirectoryPartition getContextPartition()
    {
        return contextPartition;
    }
    
    /**
     * Sets the {@link DirectoryPartition} that this configuration configures.
     */
    protected void setContextPartition( DirectoryPartition partition )
    {
        if( partition == null )
        {
            throw new NullPointerException( "partition" );
        }
        this.contextPartition = partition;
    }
    
    /**
     * Returns root entry that will be added to the {@link DirectoryPartition}
     * after it is initialized.
     */
    public Attributes getContextEntry()
    {
        return ( Attributes ) contextEntry.clone();
    }
    
    /**
     * Sets root entry that will be added to the {@link DirectoryPartition}
     * after it is initialized.
     */
    protected void setContextEntry( Attributes rootEntry )
    {
        this.contextEntry = ( Attributes ) rootEntry.clone();
    }
    
    /**
     * Returns the suffix of the {@link DirectoryPartition}.
     */
    public String getSuffix()
    {
        return suffix;
    }
    
    /**
     * Returns the normalized suffix of the {@link DirectoryPartition}.
     */
    public Name getNormalizedSuffix( MatchingRuleRegistry matchingRuleRegistry ) throws NamingException
    {
        return getNormalizedSuffix( matchingRuleRegistry.lookup( "distinguishedNameMatch" ).getNormalizer() );
    }
    
    /**
     * Returns the normalized suffix of the {@link DirectoryPartition}.
     */
    public Name getNormalizedSuffix( Normalizer normalizer ) throws NamingException
    {
        return new LdapName( normalizer.normalize( suffix ).toString() );
    }
    
    /**
     * Sets the suffix of the {@link DirectoryPartition}.
     */
    protected void setSuffix( String suffix )
    {
        suffix = suffix.trim();
        try
        {
            new LdapName( suffix );
        }
        catch( NamingException e )
        {
            throw new ConfigurationException( "Failed to normalize the suffix: " + suffix );
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
        if( getName() == null || getName().length() == 0 )
        {
            throw new ConfigurationException( "Name is not specified." );
        }

        if( getSuffix() == null )
        {
            throw new ConfigurationException( "Suffix is not specified." );
        }
    }
}
