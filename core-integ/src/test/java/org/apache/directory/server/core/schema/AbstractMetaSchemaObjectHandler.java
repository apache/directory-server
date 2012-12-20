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
package org.apache.directory.server.core.schema;


import java.io.File;

import org.apache.directory.api.ldap.model.name.Dn;
import org.apache.directory.api.ldap.model.name.Rdn;
import org.apache.directory.api.util.Strings;
import org.apache.directory.server.core.integ.AbstractLdapTestUnit;
import org.junit.Before;


/**
 * A common class for all the MetaXXXHandler test classes
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 */
public abstract class AbstractMetaSchemaObjectHandler extends AbstractLdapTestUnit
{
    protected static String workingDir;


    @Before
    public void init() throws Exception
    {
        workingDir = getService().getInstanceLayout().getPartitionsDirectory().getAbsolutePath();
    }


    /**
     * Get the path on disk where a specific SchemaObject is stored
     *
     * @param dn the SchemaObject Dn
     */
    protected String getSchemaPath( Dn dn )
    {
        StringBuilder sb = new StringBuilder();

        sb.append( workingDir ).append( '/' ).append( getService().getSchemaPartition().getId() );

        for ( Rdn rdn : dn )
        {
            sb.append( '/' );
            sb.append( Strings.toLowerCase( rdn.getName() ) );
        }

        sb.append( ".ldif" );

        return sb.toString();
    }


    /**
     * Check that a specific SchemaObject is stored on the disk at the
     * correct position in the Ldif partition
     *
     * @param dn The SchemaObject Dn
     */
    protected boolean isOnDisk( Dn dn )
    {
        // do not change the value of getSchemaPath to lowercase
        // on Linux this gives a wrong path
        String schemaObjectFileName = getSchemaPath( dn );

        File file = new File( schemaObjectFileName );

        return file.exists();
    }


    /**
     * Gets relative Dn to ou=schema.
     *
     * @param schemaName the name of the schema
     * @return the dn of the a schema entity container
     * @throws Exception on failure
     */
    protected Dn getSchemaContainer( String schemaName ) throws Exception
    {
        return new Dn( "cn=" + schemaName );
    }
}
