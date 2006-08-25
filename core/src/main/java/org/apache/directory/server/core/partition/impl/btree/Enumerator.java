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


import javax.naming.NamingEnumeration;
import javax.naming.NamingException;

import org.apache.directory.shared.ldap.filter.ExprNode;


/**
 * An enumeration builder or factory for filter expressions.
 * 
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 * @version $Rev$
 */
public interface Enumerator
{
    /**
     * Creates an enumeration to enumerate through the set of candidates 
     * satisfying a filter expression.
     * 
     * @param node a filter expression root
     * @return an enumeration over the 
     * @throws NamingException if database access fails
     */
    NamingEnumeration enumerate( ExprNode node ) throws NamingException;
}
