/*
 *  Licensed to the Apache Software Foundation (ASF) under one
 *  or more contributor license agreements.  See the NOTICE file
 *  distributed with this work for additional information
 *  regarding copyright ownership.  The ASF licenses this file
 *  to you under the Apache License, Version 2.0 (the
 *  "License"); you may not use this file except in compliance
 *  with the License.  You may obtain a copy of the License at
 *  
 *    http://www.apache.org/licenses/LICENSE-2.0
 *  
 *  Unless required by applicable law or agreed to in writing,
 *  software distributed under the License is distributed on an
 *  "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 *  KIND, either express or implied.  See the License for the
 *  specific language governing permissions and limitations
 *  under the License. 
 *  
 */
package org.apache.directory.server.core.partition.impl.btree;


import javax.naming.NamingException;


/**
 * Asserts whether or not a candidate should be returned in searching based on
 * hard coded logic.  This interface is not related to the filter AssertionNode.
 * It is strictly used for purposes internal to the search engine.
 * 
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 * @version $Rev$
 */
public interface IndexAssertion
{
    /**
     * Tests to see if a perspective candidate should be returned based on 
     * the evaluation of hard coded logic.  If the entry has not been 
     * resusitated then the getAttributes member of the record will be null.  As
     * a side-effect an index assertion may populate the entry attribute after
     * resusitating an entry from the master table.
     * 
     * @param record an index record of the entry
     * @return true if the entry should be returned, false otherwise
     * @throws NamingException if their are failures while asserting the 
     * condition
     */
    boolean assertCandidate( IndexRecord record ) throws NamingException;
}
