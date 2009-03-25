/*
 *   Licensed to the Apache Software Foundation (ASF) under one
 *   or more contributor license agreements.  See the NOTICE file
 *   distributed with this work for additional information
 *   regarding copyright ownership.  The ASF licenses this file
 *   to you under the Apache License, Version 2.0 (the
 *   "License"); you may not use this file except in compliance
 *   with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 *   Unless required by applicable law or agreed to in writing,
 *   software distributed under the License is distributed on an
 *   "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 *   KIND, either express or implied.  See the License for the
 *   specific language governing permissions and limitations
 *   under the License.
 *
 */
package org.apache.directory.server.core.event;


import org.apache.directory.server.core.interceptor.context.AddOperationContext;
import org.apache.directory.server.core.interceptor.context.DeleteOperationContext;
import org.apache.directory.server.core.interceptor.context.ModifyOperationContext;
import org.apache.directory.server.core.interceptor.context.MoveAndRenameOperationContext;
import org.apache.directory.server.core.interceptor.context.MoveOperationContext;
import org.apache.directory.server.core.interceptor.context.RenameOperationContext;


/**
 * A listener which is notified of changes to the directory service.
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 * @version $Rev$, $Date$
 */
public interface DirectoryListener
{
    /**
     * Called when an entry has been added.
     *
     * @param opContext the add operation context responsible for the change
     */
    void entryAdded( AddOperationContext opContext ); 
    
    
    /**
     * Called when an entry has been deleted.
     *
     * @param opContext the delete operation context responsible for the change
     */
    void entryDeleted( DeleteOperationContext opContext );
    
    
    /**
     * Called when an entry has been modified.
     *
     * @param opContext the modify operation context responsible for the change
     */
    void entryModified( ModifyOperationContext opContext );
    
    
    /**
     * Called when an entry has been renamed.
     *
     * @param opContext the rename operation context responsible for the change
     */
    void entryRenamed( RenameOperationContext opContext );
    
    
    /**
     * Called when an entry is moved.
     *
     * @param opContext the move operation context responsible for the change
     */
    void entryMoved( MoveOperationContext opContext );
    
    
    /**
     * Called when an entry is moved and renamed at the same time.
     *
     * @param opContext the move/rename operation context responsible for the change
     */
    void entryMovedAndRenamed( MoveAndRenameOperationContext opContext );
}
