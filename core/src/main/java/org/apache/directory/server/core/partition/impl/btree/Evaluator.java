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


import javax.naming.NamingException;

import org.apache.directory.shared.ldap.filter.ExprNode;


/**
 * Tests if an entry is eligable for return by evaluating a filter expression on
 * the candidate.  The evaluation can proceed by applying the filter on the 
 * attributes of the entry itself or indices can be used for rapid evaluation.
 * 
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 * @version $Rev$
 */
public interface Evaluator
{
    /**
     * Evaluates a candidate to determine if a filter expression selects it.
     * 
     * @param node the filter expression to evaluate on the candidate
     * @param record the index record of the entry to evaluate
     * @return true if the filter selects the candidate false otherwise
     * @throws NamingException if there is a database fault during evaluation
     */
    boolean evaluate( ExprNode node, IndexRecord record ) throws NamingException;
}
