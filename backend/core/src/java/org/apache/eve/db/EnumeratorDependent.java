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
package org.apache.eve.db;

/**
 * Cycle dependency workaround due to recusion between Enumerator and its 
 * subclasses; ConjunctionEnumerator, DisjunctionEnumerator and 
 * NegationEnumerator.  This interface enables the master top level Enumerator
 * interface to hand a reference to itself to the EnumeratorSlave 
 * 
 * @author <a href="mailto:directory-dev@incubator.apache.org">Apache Directory Project</a>
 * @version $Rev$
 */
public interface EnumeratorDependent
{
    /**
     * Sets the enumerator dependency .
     * 
     * @param enumerator the master expression enumerator handle to set
     */
    void setEnumeratorDependency( Enumerator enumerator );
}
