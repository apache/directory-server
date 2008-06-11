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


/**
 * The different kinds of events a {@link DirectoryListener} may register for 
 * notification on using the {@link EventService}.  Sometimes an entry is 
 * moved and renamed at the same time.  These notifications are sent when 
 * either RENAME or MOVE notifications are enabled.
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 * @version $Rev$, $Date$
 */
public enum EventType
{
    ADD(1), DELETE(2), MODIFY(4), RENAME(8), MOVE(16);
    
    
    public static final int ALL_EVENT_TYPES_MASK = getAllEventTypesMask();
    public static final int MOVE_OR_RENAME_MASK = MOVE.mask | RENAME.mask;
    
    
    private int mask;
    
    
    private EventType( int mask )
    {
        this.mask = mask;
    }
    
    
    public int getMask()
    {
        return mask;
    }
    
    
    private static int getAllEventTypesMask()
    {
        int allTypes = 0;
        
        for ( EventType type : values() )
        {
            allTypes |= type.getMask();
        }
        
        return allTypes;
    }
    
    
    public static boolean isAdd( int mask )
    {
        if ( ( mask & ADD.mask ) > 0 )
        {
            return true;
        }
        
        return false;
    }
    
    
    public static boolean isDelete( int mask )
    {
        if ( ( mask & DELETE.mask ) > 0 )
        {
            return true;
        }
        
        return false;
    }
    
    
    public static boolean isModify( int mask )
    {
        if ( ( mask & MODIFY.mask ) > 0 )
        {
            return true;
        }
        
        return false;
    }
    
    
    public static boolean isMove( int mask )
    {
        if ( ( mask & MOVE.mask ) > 0 )
        {
            return true;
        }
        
        return false;
    }
    
    
    public static boolean isRename( int mask )
    {
        if ( ( mask & RENAME.mask ) > 0 )
        {
            return true;
        }
        
        return false;
    }
    
    
    public static boolean isMoveAndRename( int mask )
    {
        if ( ( mask & MOVE_OR_RENAME_MASK ) > 0 )
        {
            return true;
        }
        
        return false;
    }
    
    
    public static int getMask( EventType ...eventTypes )
    {
        int mask = 0;
        
        for ( EventType type : eventTypes )
        {
            mask |= type.getMask();
        }
        
        return mask;
    }
}
