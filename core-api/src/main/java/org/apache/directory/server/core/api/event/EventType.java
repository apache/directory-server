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


import java.util.ArrayList;


/**
 * The different kinds of events a {@link DirectoryListener} may register for 
 * notification on using the {@link EventService}.  Sometimes an entry is 
 * moved and renamed at the same time.  These notifications are sent when 
 * either RENAME or MOVE notifications are enabled.
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 */
public enum EventType
{
    ADD(1), 
    DELETE(2), 
    MODIFY(4), 
    RENAME(8), 
    MOVE(16),
    MOVE_AND_RENAME(24); // MOVE | RENAME
    
    public static final int ALL_EVENT_TYPES_MASK = ADD.mask | DELETE.mask | MODIFY.mask | RENAME.mask | MOVE.mask;
    public static final int MOVE_AND_RENAME_MASK = MOVE.mask | RENAME.mask;
    private static final EventType[] EMPTY_EVENT_ARRAY = new EventType[0];

    // The internal value
    private int mask;


    EventType( int mask )
    {
        this.mask = mask;
    }


    public int getMask()
    {
        return mask;
    }


    /**
     * Gets an array of EventTypes from the PSearchControl changeTypes 
     * parameter value.  According to the documentation of the changeTypes 
     * field of the Persistent Search Control:
     * 
     * <code>
     * The changeTypes field is the logical OR of one or more of these values:
     * add (1), delete (2), modify (4), modDN (8). By default this is set to 1 |
     * 2 | 4 | 8 which is the integer value 0x0F or 15.
     * </code>
     * 
     * NOTE: When the changeTypes mask includes a modDN(8) we include both the 
     * RENAME and MOVE EventType objects in the array.
     * 
     * @param psearchChangeTypes the value of the changeTypes parameter
     * @return array of EventType objects
     */
    public static EventType[] getEventTypes( int psearchChangeTypes )
    {
        ArrayList<EventType> types = new ArrayList<>();

        if ( isAdd( psearchChangeTypes ) )
        {
            types.add( ADD );
        }

        if ( isDelete( psearchChangeTypes ) )
        {
            types.add( DELETE );
        }

        if ( isModify( psearchChangeTypes ) )
        {
            types.add( MODIFY );
        }

        if ( ( psearchChangeTypes & 8 ) > 0 )
        {
            types.add( MOVE );
            types.add( RENAME );
        }

        return types.toArray( EMPTY_EVENT_ARRAY );
    }


    /**
     * Tells if the EventType is an ADD
     * 
     * @param mask The EventType to check
     * @return <tt>true</tt> if the EventType is a ADD
     */
    public static boolean isAdd( int mask )
    {
        return ( ( mask & ADD.mask ) != 0 );
    }


    /**
     * Tells if the EventType is a DELETE
     * 
     * @param mask The EventType to check
     * @return <tt>true</tt> if the EventType is a DELETE
     */
    public static boolean isDelete( int mask )
    {
        return ( ( mask & DELETE.mask ) != 0 );
    }


    /**
     * Tells if the EventType is a MODIFY
     * 
     * @param mask The EventType to check
     * @return <tt>true</tt> if the EventType is a MODIFY
     */
    public static boolean isModify( int mask )
    {
        return ( ( mask & MODIFY.mask ) != 0 );
    }


    /**
     * Tells if the EventType is a MOVE
     * 
     * @param mask The EventType to check
     * @return <tt>true</tt> if the EventType is a MOVE
     */
    public static boolean isMove( int mask )
    {
        return ( ( mask & MOVE.mask ) != 0 );
    }


    /**
     * Tells if the EventType is a RENAME
     * 
     * @param mask The EventType to check
     * @return <tt>true</tt> if the EventType is a RENAME
     */
    public static boolean isRename( int mask )
    {
        return ( ( mask & RENAME.mask ) != 0 );
    }


    /**
     * Tells if the EventType is a MOVE and RENAME
     * 
     * @param mask The EventType to check
     * @return <tt>true</tt> if the EventType is a MOVE_AND_RENAME
     */
    public static boolean isMoveAndRename( int mask )
    {
        return ( ( mask & MOVE_AND_RENAME_MASK ) != 0 );
    }


    /**
     * Compute the mask associated with the given eventTypes
     * 
     * @param eventTypes The eventTypes 
     * @return The associated mask
     */
    public static int getMask( EventType... eventTypes )
    {
        int mask = 0;

        for ( EventType type : eventTypes )
        {
            mask |= type.getMask();
        }

        return mask;
    }


    /**
     * checks if the given mask value matches with any of the defined
     * standard EventTypes 
     *
     * @param mask the mask value of the EventType
     * @return EventType matching the mask value of the standard event types defined, else throws IllegalArgumentException 
     */
    public static EventType getType( int mask )
    {
        switch ( mask )
        {
            case 1:
                return ADD;

            case 2:
                return DELETE;

            case 4:
                return MODIFY;

            case 8:
                return RENAME;

            case 16:
                return MOVE;

            default:
                throw new IllegalArgumentException( "unknown mask value " + mask );
        }
    }


    /**
     * Print the flags
     * 
     * @param mask the flags value to print
     * @return A textual version of the mask
     */
    public static String toString( int mask )
    {
        switch ( mask )
        {
            case 0:
                return "no event";

            case 1:
                return "ADD";

            case 2:
                return "DELETE";

            case 4:
                return "MODIFY";

            case 8:
                return "RENAME";

            case 16:
                return "MOVE";

            case 24:
                return "MOVE_AND_RENAME";

            case 31:
                return "ALL EVENTS";

            default:
                return "Unknown";
        }
    }
}
