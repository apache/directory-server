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
package org.apache.directory.server.core.shared.txn.logedit;

import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;

import org.apache.directory.server.core.api.txn.logedit.AbstractDataChange;
import org.apache.directory.shared.ldap.model.entry.Modification;
import org.apache.directory.shared.ldap.model.entry.DefaultModification;

/**
 * 
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 */
public class EntryChange extends AbstractDataChange
{
    /** redo change */
    private Modification redoChange;
    
    /** undo change */
    private Modification undoChange;
    
    //For externalizable
    public EntryChange()
    {
    }
    
    public EntryChange( Modification redo, Modification undo )
    {
        redoChange = redo;
        undoChange = undo;
    }
    
    
    public Modification getRedoChange()
    {
        return redoChange;
    }
    
    
    public Modification getUndoChange()
    {
        return undoChange;
    }
    
    
    @Override
    public void readExternal( ObjectInput in ) throws IOException, ClassNotFoundException
    {
        redoChange = new DefaultModification();
        redoChange.readExternal( in );
        
        undoChange = new DefaultModification();
        undoChange.readExternal( in );
        
    }


    @Override
    public void writeExternal( ObjectOutput out ) throws IOException
    {
        redoChange.writeExternal( out );
        undoChange.writeExternal( out );
    }
}
