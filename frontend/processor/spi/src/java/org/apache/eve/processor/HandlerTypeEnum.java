/*
 *   Copyright 2004 The Apache Software Foundation
 *
 *   Licensed under the Apache License, Version 2.0 (the "License");
 *   you may not use this file except in compliance with the License.
 *   You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *   Unless required by applicable law or agreed to in writing, software
 *   distributed under the License is distributed on an "AS IS" BASIS,
 *   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *   See the License for the specific language governing permissions and
 *   limitations under the License.
 *
 */
package org.apache.eve.processor ;


import org.apache.commons.lang.enum.ValuedEnum ;


/**
 * Valued enumeration for the three types of handlers: NOREPLY, SINGLEREPLY,
 * and SEARCH.
 * 
 * @author <a href="mailto:directory-dev@incubator.apache.org">
 * Apache Directory Project</a>
 * @version $Rev$   
 */
public class HandlerTypeEnum extends ValuedEnum
{
    /** Value for noreply enumeration type */
    public static final int NOREPLY_VAL = 0 ;
    /** Value for singlereply enumeration type */
    public static final int SINGLEREPLY_VAL = 1 ;
    /** Value for many value enumeration type */
    public static final int MANYREPLY_VAL = 2 ;

    /** Enum for noreply type */
	public static final HandlerTypeEnum NOREPLY =
        new HandlerTypeEnum( "NOREPLY", NOREPLY_VAL ) ;
    /** Enum for singlereply type */
	public static final HandlerTypeEnum SINGLEREPLY =
        new HandlerTypeEnum( "SINGLEREPLY", SINGLEREPLY_VAL ) ;
    /** Enum for search type */
	public static final HandlerTypeEnum SEARCH =
        new HandlerTypeEnum( "MANYREPLY", MANYREPLY_VAL ) ;


    /**
     * Enables creation of constants in this class only.
     *
     * @param name the name of the enum
     * @param value the value of the enum
     */
	private HandlerTypeEnum( String name, int value )
    {
        super( name, value ) ;
    }
}
