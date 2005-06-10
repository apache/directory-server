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

import javax.naming.directory.Attributes;
import javax.naming.directory.BasicAttributes;

import org.apache.ldap.server.ContextPartition;


/**
 * A configuration for {@link ContextPartition}.
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 * @version $Rev$, $Date$
 */
public class ContextPartitionConfiguration
{
    private String suffix;
    private Set indexedAttributes = new HashSet(); // Set<String>
    private Attributes rootEntry = new BasicAttributes();
    private ContextPartition partition;
    
    /**
     * Creates a new instance.
     */
    protected ContextPartitionConfiguration()
    {
    }

    public Set getIndexedAttributes()
    {
        Set result = new HashSet();
        result.addAll( indexedAttributes );
        return result;
    }
    
    protected void setIndexedAttributes( Set indexedAttributes )
    {
        Set newIndexedAttributes = new HashSet();

        Iterator i = indexedAttributes.iterator();
        while( i.hasNext() )
        {
            Object e = i.next();
            if( !(e instanceof String) )
            {
                throw new ConfigurationException( "All elements of indexedAttributes must be strings." );
            }
            
            // TODO Attribute name must be normalized and validated
            String attr = ( ( String ) e ).trim();
            newIndexedAttributes.add( attr );
        }
        this.indexedAttributes = newIndexedAttributes;
    }
    
    public ContextPartition getPartition()
    {
        return partition;
    }
    
    protected void setPartition( ContextPartition partition )
    {
        this.partition = partition;
    }
    
    public Attributes getRootEntry()
    {
        return ( Attributes ) rootEntry.clone();
    }
    
    protected void setRootEntry( Attributes rootEntry )
    {
        this.rootEntry = ( Attributes ) rootEntry.clone();
    }
    
    public String getSuffix()
    {
        return suffix;
    }
    
    protected void setSuffix( String suffix )
    {
        // TODO Suffix should be normalized before being set
        this.suffix = suffix.trim();
    }
}
