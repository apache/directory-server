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
package org.apache.eve;


import javax.naming.Name;


/**
 * ContextPartitions are indivisible BackingStores associated with a naming
 * context as a base suffix.  All JNDI Attributes entries at and under the
 * context of this suffix are stored within this partition.
 *
 * @author <a href="mailto:directory-dev@incubator.apache.org">Apache Directory Project</a>
 * @version $Rev$
 */
public interface ContextPartition extends BackingStore
{
    /**
     * Gets the distinguished/absolute name of the suffix for all entries
     * stored within this BackingStore.
     *
     * @param normalized boolean value used to control the normalization of the
     * returned Name.  If true the normalized Name is returned, otherwise the 
     * original user provided Name without normalization is returned.
     * @return Name representing the distinguished/absolute name of this
     * BackingStores root context.
     */
    Name getSuffix( boolean normalized ) ;
}
