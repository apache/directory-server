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
package org.apache.directory.server.core.api.event;


import org.apache.directory.server.core.api.interceptor.context.AddOperationContext;
import org.apache.directory.server.core.api.interceptor.context.DeleteOperationContext;
import org.apache.directory.server.core.api.interceptor.context.ModifyOperationContext;
import org.apache.directory.server.core.api.interceptor.context.MoveAndRenameOperationContext;
import org.apache.directory.server.core.api.interceptor.context.MoveOperationContext;
import org.apache.directory.server.core.api.interceptor.context.RenameOperationContext;


/**
 * A DirectoryListener adapter class for convenient subclassing
 * It doesn't handle any operation
 *  
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 */
public abstract class DirectoryListenerAdapter implements DirectoryListener
{

    @Override
    public void entryAdded( AddOperationContext addContext )
    {
    }


    @Override
    public void entryDeleted( DeleteOperationContext deleteContext )
    {
    }


    @Override
    public void entryModified( ModifyOperationContext modifyContext )
    {
    }


    @Override
    public void entryRenamed( RenameOperationContext renameContext )
    {
    }


    @Override
    public void entryMoved( MoveOperationContext moveContext )
    {
    }


    @Override
    public void entryMovedAndRenamed( MoveAndRenameOperationContext moveAndRenameContext )
    {
    }


    @Override
    public boolean isSynchronous()
    {
        return false;
    }
}
