/*
 *   Copyright 2006 The Apache Software Foundation
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
package org.apache.directory.server.core.subtree;


import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import org.apache.directory.shared.ldap.subtree.SubtreeSpecification;


/**
 * A cache for subtree specifications.
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 * @version $Rev$
 */
public class SubtreeCache
{
    private final Map subtrees = new HashMap();
    
    
    final SubtreeSpecification getSubtreeSpecification( String normalizedName )
    {
        return ( SubtreeSpecification ) subtrees.get( normalizedName );
    }
    
    
    final SubtreeSpecification removeSubtreeSpecification( String normalizedName )
    {
        return ( SubtreeSpecification ) subtrees.remove( normalizedName );
    }
    
    
    final SubtreeSpecification setSubtreeSpecification( String normalizedName, SubtreeSpecification ss )
    {
        return ( SubtreeSpecification ) subtrees.put( normalizedName, ss );
    }
    
    
    final boolean hasSubtreeSpecification( String normalizedName )
    {
        return subtrees.containsKey( normalizedName );
    }
    
    
    final Iterator nameIterator()
    {
        return subtrees.keySet().iterator();
    }
}
