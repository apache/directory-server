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
package org.apache.directory.shared.ldap.filter;

/**
 * All the different kind of assertions :
 * 
 * 
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 * @version $Rev: 470116 $
 */
public enum AssertionEnum {
    /** equality assertion node */
    EQUALITY(0),

    /** presence assertion node */
    PRESENCE(1),

    /** substring match assertion node */
    SUBSTRING(2),

    /** greater than or equal to assertion node */
    GREATEREQ(3),

    /** less than or equal to assertion node */
    LESSEQ(4),

    /** approximate assertion node */
    APPROXIMATE(5),

    /** extensible match assertion node */
    EXTENSIBLE(6),

    /** scope assertion node */
    SCOPE(7),

    /** Predicate assertion node */
    ASSERTION(8),

    /** OR operator constant */
    OR(9),

    /** AND operator constant */
    AND(10),

    /** NOT operator constant */
    NOT(11);
    
    /** Stores the integer value of each element of the enumeration */
    private int value;
    
    /**
     * Private constructor so no other instances can be created other than the
     * public static constants in this class.
     * 
     * @param value the integer value of the enumeration.
     */
    private AssertionEnum( int value )
    {
       this.value = value;
    }

    
    /**
     * @return The value associated with the current element.
     */
    public int getValue()
    {
        return value;
    }

}
