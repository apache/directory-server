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
package org.apache.directory.server.core.subtree;


import org.apache.directory.shared.ldap.subtree.SubtreeSpecification;


/**
 * An operational view of a subentry within the system.
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 * @version $Rev$
 */
public class Subentry
{
    static final int COLLECTIVE_SUBENTRY = 1;
    static final int SCHEMA_SUBENTRY = 2;
    static final int ACCESS_CONTROL_SUBENTRY = 4;
    
    private SubtreeSpecification ss;
    private int type;
    
    
    final void setSubtreeSpecification( SubtreeSpecification ss )
    {
        this.ss = ss;
    }
    

    final SubtreeSpecification getSubtreeSpecification()
    {
        return ss;
    }


    final void setTypes( int type )
    {
        this.type = type;
    }


    final int getTypes()
    {
        return type;
    }
    
    
    final boolean isCollectiveSubentry()
    {
        return ( COLLECTIVE_SUBENTRY & type ) == COLLECTIVE_SUBENTRY;
    }
    
    
    final boolean isSchemaSubentry()
    {
        return ( SCHEMA_SUBENTRY & type ) == SCHEMA_SUBENTRY;
    }
    
    
    final boolean isAccessControlSubentry()
    {
        return ( ACCESS_CONTROL_SUBENTRY & type ) == ACCESS_CONTROL_SUBENTRY;
    }
}
