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
package org.apache.ldap.server.invocation;


import org.apache.ldap.common.filter.ExprNode;
import org.apache.ldap.server.BackingStore;

import javax.naming.Name;
import javax.naming.NamingException;
import javax.naming.directory.SearchControls;
import java.util.Map;


/**
 * Represents an {@link Invocation} on {@link BackingStore#search(Name, Map, ExprNode, SearchControls)}.
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 * @version $Rev$, $Date$
 */
public class Search extends Invocation
{

    private Name baseName;

    private final Map environment;

    private final ExprNode filter;

    private final SearchControls controls;


    public Search( Name baseName, Map environment, ExprNode filters,
                   SearchControls controls )
    {
        if ( baseName == null )
        {
            throw new NullPointerException( "baseName" );
        }
        if ( environment == null )
        {
            throw new NullPointerException( "environment" );
        }
        if ( filters == null )
        {
            throw new NullPointerException( "filter" );
        }
        if ( controls == null )
        {
            throw new NullPointerException( "controls" );
        }

        this.baseName = baseName;

        this.environment = environment;

        this.filter = filters;

        this.controls = controls;
    }


    public Name getBaseName()
    {
        return baseName;
    }


    public Map getEnvironment()
    {
        return environment;
    }


    public ExprNode getFilter()
    {
        return filter;
    }


    public SearchControls getControls()
    {
        return controls;
    }


    protected Object doExecute( BackingStore store ) throws NamingException
    {
        return store.search( baseName, environment, filter, controls );
    }


    public void setBaseName( Name baseName )
    {
        this.baseName = baseName;
    }
}
