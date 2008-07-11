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
package org.apache.directory.shared.ldap.ldif;


/**
 * A type safe enumeration for an LDIF record's change type.
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 * @version $Rev$, $Date$
 */
public enum ChangeType
{
    Add( 0 ),
    Modify( 1 ),
    ModDn( 2 ),
    ModRdn( 3 ),
    Delete( 4 );
    
    /** Add ordinal value */
    public static final int ADD_ORDINAL = 0;

    /** Modify ordinal value */
    public static final int MODIFY_ORDINAL = 1;

    /** ModDN ordinal value */
    public static final int MODDN_ORDINAL = 2;

    /** ModRDN ordinal value */
    public static final int MODRDN_ORDINAL = 3;

    /** Delete ordinal value */
    public static final int DELETE_ORDINAL = 4;

    /* the ordinal value for a change type */
    private final int changeType;
    
    
    /**
     * Creates a new instance of ChangeType.
     *
     * @param changeType
     */
    private ChangeType( int changeType )
    {
        this.changeType = changeType;
    }


    /**
     * Get's the ordinal value for a ChangeType.
     * 
     * @return the changeType
     */
    public int getChangeType()
    {
        return changeType;
    }
    
    public static ChangeType getChangeType( int val )
    {
        switch( val )
        {
            case 0: return Add;
            
            case 1: return Modify;
            
            case 2: return ModDn;
            
            case 3: return ModRdn;
            
            case 4: return Delete;
            
            default:
                throw new IllegalArgumentException( "Unknown change type value " + val );
        }
    }
}
