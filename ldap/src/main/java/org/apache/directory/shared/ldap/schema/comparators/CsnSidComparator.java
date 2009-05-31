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
package org.apache.directory.shared.ldap.schema.comparators;


import java.util.Comparator;

import org.apache.directory.shared.ldap.csn.CSN;


/**
 * A comparator for CSN SID.
 *
 * The CSN are ordered depending on an evaluation of its component, in this order :
 * - time, 
 * - changeCount,
 * - sid
 * - modifierNumber
 * 
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 * @version $Rev$
 */
public class CsnSidComparator implements Comparator<CSN>
{
    /** A static instance of this comparator */
    public static final Comparator<CSN> INSTANCE = new CsnSidComparator();
    
    
    /**
     * @see java.util.Comparator#compare(java.lang.Object, java.lang.Object)
     */
    public int compare( CSN csn1, CSN csn2 )
    {
        // -------------------------------------------------------------------
        // Handle some basis cases
        // -------------------------------------------------------------------

        if ( csn1 == null )
        {
            return ( csn2 == null ) ? 0 : -1;
        }
        
        if ( csn2 == null )
        {
            return 1;
        }
        
        if ( csn1.getTimestamp() != csn2.getTimestamp() )
        {
            return ( csn1.getTimestamp() < csn2.getTimestamp() ? -1 : 1 );
        }
        
        if ( csn1.getChangeCount() != csn2.getChangeCount() )
        {
            return ( csn1.getChangeCount() < csn2.getChangeCount() ? -1 : 1 );
        }
        
        if ( csn1.getReplicaId() != csn2.getReplicaId() )
        {
            return ( csn1.getReplicaId() < csn2.getReplicaId() ? -1 : 1 );
        }
        
        if ( csn1.getOperationNumber() != csn2.getOperationNumber() )
        {
            return ( csn1.getOperationNumber() < csn2.getOperationNumber() ? -1 : 1 );
        }
        
        return 0;
    }
}
