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
package org.apache.directory.server.core.api.interceptor.context;


import org.apache.directory.server.core.api.changelog.ChangeLogEvent;
import org.apache.directory.server.core.api.changelog.LogChange;


/**
 * Operations (write based) causing changes extend this interface. 
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 */
public interface ChangeOperationContext extends OperationContext
{
    /**
     * Gets the ChangeLogEvent associated with this operation after the 
     * operation has been executed.  Returns null if the ChangeLogService has 
     * not been enabled. 
     *
     * @return the ChangeLogEvent associated with this operation, or null
     */
    ChangeLogEvent getChangeLogEvent();
    
    
    /**
     * Set the flag which tells the server to log the changes into
     * the changeLog file
     * 
     * @param log The flag
     */
    void setLogChange( LogChange log );

    
    /**
     * @return True if the changes are logged into the changeLog
     */
    boolean isLogChange();
}
