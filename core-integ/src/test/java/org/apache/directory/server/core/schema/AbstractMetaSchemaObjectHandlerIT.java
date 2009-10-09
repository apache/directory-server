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
import java.util.Enumeration;

import org.apache.directory.shared.ldap.name.LdapDN;
import org.apache.directory.shared.ldap.name.Rdn;
import org.apache.directory.shared.ldap.util.StringTools;
import org.junit.BeforeClass;

/**
 * A common class for all the MetaXXXHandler test classes
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 * @version $Rev$, $Date$
 */
public abstract class AbstractMetaSchemaObjectHandlerIT
{
    protected static String workingDir;

    @BeforeClass
    public static final void init()
    {
        String path = AbstractMetaSchemaObjectHandlerIT.class.getResource( "" ).getPath();
        int targetPos = path.indexOf( "target" );
        workingDir = path.substring( 0, targetPos + 6 ) + "/server-work/schema";
    }
    
    
    /**
     * Get the path on disk where a specific SchemaObject is stored
     *
     * @param dn the SchemaObject DN
     */
    protected String getSchemaPath( LdapDN dn )
    {
        StringBuilder sb = new StringBuilder();
        
        sb.append( workingDir ).append( '/' ).append( "ou=schema" );
        
        Enumeration<Rdn> rdns = dn.getAllRdn();
        
        while ( rdns.hasMoreElements() )
        {
            sb.append( '/' );
            sb.append( StringTools.toLowerCase( rdns.nextElement().getUpName() ) );
        }
        
        sb.append( ".ldif" );
        
        return sb.toString();
    }

    
    /**
     * Check that a specific SchemaObject is stored on the disk at the
     * correct position in the Ldif partition
     *
     * @param dn The SchemaObject DN
     */
    protected boolean isOnDisk( LdapDN dn )
    {
        String schemaObjectFileName = StringTools.toLowerCase( getSchemaPath( dn ) );

        File file = new File( schemaObjectFileName );
        
        return file.exists();
    }
}
