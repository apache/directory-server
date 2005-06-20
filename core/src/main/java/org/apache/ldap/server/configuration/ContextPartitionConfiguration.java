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
import org.apache.ldap.server.partition.ContextPartition;
import org.apache.ldap.server.schema.MatchingRuleRegistry;


/**
 * A configuration for {@link ContextPartition}.
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 * @version $Rev$, $Date$
 */
public class ContextPartitionConfiguration
{
    private String name;
    private String suffix;
    private Set indexedAttributes = new HashSet(); // Set<String>
    private Attributes contextEntry = new BasicAttributes();
    private ContextPartition contextPartition;
    
    /**
     * Creates a new instance.
     */
    protected ContextPartitionConfiguration()
    {
    }
    
    public String getName()
    {
        return name;
    }
    
    protected void setName( String name )
    {
        // TODO name can be a directory name.
        this.name = name.trim();
    }

    public Set getIndexedAttributes()
    {
        return ConfigurationUtil.getClonedSet( indexedAttributes );
    }
    
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
        this.indexedAttributes = newIndexedAttributes;
    }
    
    public ContextPartition getContextPartition()
    {
        return contextPartition;
    }
    
    protected void setContextPartition( ContextPartition partition )
    {
        this.contextPartition = partition;
    }
    
    public Attributes getContextEntry()
    {
        return ( Attributes ) contextEntry.clone();
    }
    
    protected void setContextEntry( Attributes rootEntry )
    {
        this.contextEntry = ( Attributes ) rootEntry.clone();
    }
    
    public String getSuffix()
    {
        return suffix;
    }
    
    public Name getNormalizedSuffix( MatchingRuleRegistry matchingRuleRegistry ) throws NamingException
    {
        return getNormalizedSuffix( matchingRuleRegistry.lookup( "distinguishedNameMatch" ).getNormalizer() );
    }
    
    public Name getNormalizedSuffix( Normalizer normalizer ) throws NamingException
    {
        return new LdapName( normalizer.normalize( suffix ).toString() );
    }
    
    protected void setSuffix( String suffix )
    {
        suffix = suffix.trim();
        try
        {
            new LdapName( suffix );
        }
        catch( NamingException e )
        {
            throw new ConfigurationException( "Failed to normalized the suffix: " + suffix );
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
        if( getName() == null )
        {
            throw new ConfigurationException( "Name is not specified." );
        }

        if( getSuffix() == null )
        {
            throw new ConfigurationException( "Suffix is not specified." );
        }
        
        if( getContextPartition() == null )
        {
            throw new ConfigurationException( "Context partition is not specifiec." );
        }
    }
}
