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
package org.apache.directory.server.schema.bootstrap;


import org.apache.directory.shared.ldap.util.ArrayUtils;
import org.apache.directory.shared.ldap.util.ClassUtils;


/**
 * Abstract bootstrap schema implementation.
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 * @version $Rev$
 */
public class AbstractBootstrapSchema implements BootstrapSchema
{
    protected static final String[] DEFAULT_DEPS = ArrayUtils.EMPTY_STRING_ARRAY;
    private static final String DEFAULT_OWNER = "uid=admin,ou=system";
    private static final String DEFAULT_SCHEMA_NAME = "default";
    private static final String DEFAULT_PACKAGE_NAME = AbstractBootstrapSchema.class.getPackage().getName();

    private final String owner;
    private final String schemaName;
    private final String packageName;
    private String[] dependencies;

    private transient String baseName;
    private transient String defaultBaseName;

    private transient String schemaNameCapped;


    // ------------------------------------------------------------------------
    // C O N S T R U C T O R S
    // ------------------------------------------------------------------------

    protected AbstractBootstrapSchema(String schemaName)
    {
        this( null, schemaName, null, null );
    }


    protected AbstractBootstrapSchema(String owner, String schemaName)
    {
        this( owner, schemaName, null, null );
    }


    protected AbstractBootstrapSchema(String owner, String schemaName, String packageName)
    {
        this( owner, schemaName, packageName, null );
    }


    protected AbstractBootstrapSchema(String owner, String schemaName, String packageName, String[] dependencies)
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

        if ( packageName == null )
        {
            this.packageName = DEFAULT_PACKAGE_NAME;
        }
        else
        {
            this.packageName = packageName;
        }

        if ( dependencies == null )
        {
            this.dependencies = ArrayUtils.EMPTY_STRING_ARRAY;
        }
        else
        {
            this.dependencies = dependencies;
        }

        StringBuffer buf = new StringBuffer();
        buf.append( Character.toUpperCase( schemaName.charAt( 0 ) ) );
        buf.append( schemaName.substring( 1, schemaName.length() ) );
        schemaNameCapped = buf.toString();

        buf.setLength( 0 );
        buf.append( DEFAULT_PACKAGE_NAME );
        buf.append( ClassUtils.PACKAGE_SEPARATOR_CHAR );
        buf.append( schemaNameCapped );
        defaultBaseName = buf.toString();

        buf.setLength( 0 );
        buf.append( packageName );
        buf.append( ClassUtils.PACKAGE_SEPARATOR_CHAR );
        buf.append( schemaNameCapped );
        baseName = buf.toString();
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


    protected final void setDependencies( String[] dependencies )
    {
        this.dependencies = dependencies;
    }


    public String getBaseClassName()
    {
        return baseName;
    }


    public String getDefaultBaseClassName()
    {
        return defaultBaseName;
    }


    public String getFullClassName( ProducerTypeEnum type )
    {
        return baseName + type.getName();
    }


    public String getFullDefaultBaseClassName( ProducerTypeEnum type )
    {
        return defaultBaseName + type.getName();
    }


    public String getUnqualifiedClassName( ProducerTypeEnum type )
    {
        return schemaNameCapped + type.getName();
    }


    public String getPackageName()
    {
        return packageName;
    }


    public String getUnqualifiedClassName()
    {
        return schemaNameCapped + "Schema";
    }


    public boolean isDisabled()
    {
        return false;
    }
}
