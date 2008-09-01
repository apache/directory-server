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
package org.apache.directory.mitosis.operation;


/**
 * 
 * An enum used to determinate the operation type when deserializing
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 * @version $Rev$, $Date$
 */
public enum OperationType
{
    /** The Add Entry operation */
    ADD_ENTRY(0), 

    /** The Add Attribute operation */
    ADD_ATTRIBUTE(1), 

    /** The Delete Attribute operation */
    DELETE_ATTRIBUTE(2),
    
    /** The Replace Attribute operation */
    REPLACE_ATTRIBUTE(3),
    
    /** The composite operation */
    COMPOSITE_OPERATION(4);
    
    /** The associated integer values */
    private static final int ADD_ENTRY_VALUE = 0;
    private static final int ADD_ATTRIBUTE_VALUE = 1;
    private static final int DELETE_ATTRIBUTE_VALUE = 2;
    private static final int REPLACE_ATTRIBUTE_VALUE = 3;
    private static final int COMPOSITE_OPERATION_VALUE = 4;

    
    /** The inner value */
    private int value;
    
    
    /**
     * Creates a new instance of OperationType.
     *
     * @param value the inner value
     */
    private OperationType( int value )
    {
        this.value = value;
    }

    
    /**
     * @return the associated integer value
     */
    public int getValue()
    {
        return value;
    }
    
    
    /**
     * Get the associated OperationType from an integer value
     *
     * @param value The integer value of an operation type
     * @return the associated operation type
     */
    public static OperationType get( int value )
    {
        switch ( value )
        {
            case ADD_ENTRY_VALUE :
                return ADD_ENTRY;
                
            case ADD_ATTRIBUTE_VALUE :
                return ADD_ATTRIBUTE;
                
            case DELETE_ATTRIBUTE_VALUE :
                return DELETE_ATTRIBUTE;
                
            case REPLACE_ATTRIBUTE_VALUE :
                return REPLACE_ATTRIBUTE;
                
            case COMPOSITE_OPERATION_VALUE :
                default :
                return COMPOSITE_OPERATION;
        }
    }
}
