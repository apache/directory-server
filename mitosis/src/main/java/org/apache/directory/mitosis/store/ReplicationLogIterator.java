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
package org.apache.directory.mitosis.store;


import java.sql.ResultSet;

import org.apache.directory.mitosis.operation.Operation;

/**
 * Iterates a set of {@link Operation}s, which is a result of a query on 
 * {@link ReplicationStore}.  It's usage is similar to that of JDBC
 * {@link ResultSet}.
 * 
 * @author The Apache Directory Project (dev@directory.apache.org)
 * @version $Rev$, $Date$
 */
public interface ReplicationLogIterator
{
    /**
     * Move on to the next item.
     * @return <tt>true</tt> if and only if it has more item.
     */
    boolean next();

    /**
     * Releases all resources allocated to this iterator.
     */
    void close();

    /**
     * Returns the {@link Operation} on the current iterator position.
     */
    Operation getOperation();
}
