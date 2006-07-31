/*
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


/**
 * An immutable configuration object for partition indices on entry attributes.
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 * @version $Rev$, $Date$
 */
public class IndexConfiguration
{
    public static final int DEFAULT_INDEX_CACHE_SIZE = 100;
    
    private String attributeId;
    private int cacheSize = DEFAULT_INDEX_CACHE_SIZE;
    
    
    protected void setAttributeId( String attributeId )
    {
        this.attributeId = attributeId;
    }
    
    
    public String getAttributeId()
    {
        return attributeId;
    }


    protected void setCacheSize( int cacheSize )
    {
        this.cacheSize = cacheSize;
    }


    public int getCacheSize()
    {
        return cacheSize;
    }
    
    
    public int hashCode()
    {
        return this.attributeId.hashCode();
    }
    
    
    public String toString()
    {
        return this.attributeId;
    }
}
