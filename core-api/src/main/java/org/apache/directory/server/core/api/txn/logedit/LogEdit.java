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
package org.apache.directory.server.core.api.txn.logedit;

import org.apache.directory.server.core.api.log.LogAnchor;

import java.io.Externalizable;

/**
 * 
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 */
public interface LogEdit extends Externalizable 
{
    /**
     * Returns the position the edit is inserted in the wal.
     * Log anchor is initialized is set after the edit is serialized and inserted into
     * the wal so it should be transient.
     *
     * @return position of the log edit in the wal
     */
    LogAnchor getLogAnchor();
    
    /**
     * Applies the logedit to the underlying partitions
     *
     * @param recovery true if at recovery stage 
     */
    void apply( boolean recovery ) throws Exception;
}
