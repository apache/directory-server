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
package org.apache.directory.server.kerberos.shared.messages.value;


import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;


/**
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 * @version $Rev$, $Date$
 */
public class PrincipalNameModifier
{
    private static final String COMPONENT_SEPARATOR = "/";

    List<String> components = new ArrayList<String>();
    int nameType;


    /**
     * Returns the {@link PrincipalName}.
     *
     * @return The {@link PrincipalName}.
     */
    public PrincipalName getPrincipalName()
    {
        StringBuffer sb = new StringBuffer();
        Iterator<String> it = components.iterator();

        while ( it.hasNext() )
        {
            String component = it.next();
            sb.append( component );

            if ( it.hasNext() )
            {
                sb.append( COMPONENT_SEPARATOR );
            }
        }

        return new PrincipalName( sb.toString(), nameType );
    }


    /**
     * Sets the type.
     *
     * @param type
     */
    public void setType( int type )
    {
        nameType = type;
    }


    /**
     * Adds a name component.
     *
     * @param name
     */
    public void addName( String name )
    {
        components.add( name );
    }
}
