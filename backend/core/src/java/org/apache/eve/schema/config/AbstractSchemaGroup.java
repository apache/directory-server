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
package org.apache.eve.schema.config;


import org.apache.ldap.common.util.ArrayUtils;

import javax.naming.NamingException;


/**
 * Document me.
 *
 * @author <a href="mailto:directory-dev@incubator.apache.org">Apache Directory Project</a>
 * @version $Rev$
 */
public class AbstractSchemaGroup implements SchemaGroup
{
    private static final String DEFAULT_OWNER = "uid=admin,ou=system";
    private static final String DEFAULT_SCHEMA_NAME = "default";

    private final String owner;
    private final String schemaName;
    private final String[] dependencies;


    // ------------------------------------------------------------------------
    // C O N S T R U C T O R S
    // ------------------------------------------------------------------------


    protected AbstractSchemaGroup( String owner,
                                   String schemaName,
                                   String[] dependencies )
    {
        if ( owner == null )
        {
            this.owner = DEFAULT_OWNER;
        }
        else
        {
            this.owner = owner;
        }

        if ( schemaName == null )
        {
            this.schemaName = DEFAULT_SCHEMA_NAME;
        }
        else
        {
            this.schemaName = schemaName;
        }

        if ( dependencies == null )
        {
            this.dependencies = ArrayUtils.EMPTY_STRING_ARRAY;
        }
        else
        {
            this.dependencies = dependencies;
        }
    }


    public final String getOwner()
    {
        return owner;
    }


    public final String getSchemaName()
    {
        return schemaName;
    }


    public final String[] getDependencies()
    {
        return dependencies;
    }


    public final void populate( BootstrapRegistries registries )
        throws NamingException
    {
    }


    // ------------------------------------------------------------------------
    // Utility Methods
    // ------------------------------------------------------------------------


}
