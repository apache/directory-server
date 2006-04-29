/*
 *   Copyright 2006 The Apache Software Foundation
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

package org.apache.directory.shared.ldap.trigger;


/**
 * An enumeration that represents action times
 * for an LDAP trigger specification.
 * 
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 * @version $Rev:$, $Date:$
 */
public class ActionTime
{
    public static final ActionTime BEFORE = new ActionTime( "BEFORE" );

    public static final ActionTime AFTER = new ActionTime( "AFTER" );

    public static final ActionTime INSTEADOF = new ActionTime( "INSTEADOF" );

    
    private final String name;


    private ActionTime( String name )
    {
        this.name = name;
    }


    /**
     * Returns the name of this action time.
     */
    public String getName()
    {
        return name;
    }


    public String toString()
    {
        return name;
    }
}
