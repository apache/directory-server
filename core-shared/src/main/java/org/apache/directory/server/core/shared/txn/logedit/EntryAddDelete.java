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
package org.apache.directory.server.core.txn.shared.logedit;

import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;

import org.apache.directory.shared.ldap.model.entry.Entry;
import org.apache.directory.shared.ldap.model.entry.DefaultEntry;

/**
 * 
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 */
public class EntryAddDelete<ID> extends AbstractDataChange<ID>
{
    /** Added or deleted entry */
    private Entry entry;
    
    /** Type of change */
    Type type;
    
    // For externalizable
    public EntryAddDelete(  )
    {
        
    }
    
    public EntryAddDelete( Entry entry, Type type )
    {
        this.entry = entry;
        this.type = type;
    }
    
    
    public Entry getChangedEntry()
    {
        return entry;
    }
    
    
    public Type getType()
    {
        return type;
    }
    
    
    @Override
    public void readExternal( ObjectInput in ) throws IOException, ClassNotFoundException
    {
     entry = new DefaultEntry();
     entry.readExternal( in );
     type = Type.values()[in.readInt()];
    }


    @Override
    public void writeExternal( ObjectOutput out ) throws IOException
    {
       entry.writeExternal( out );
       out.writeInt( type.ordinal() );
    }
    
    public enum Type
    {
        ADD,
        DELETE
    }
}
