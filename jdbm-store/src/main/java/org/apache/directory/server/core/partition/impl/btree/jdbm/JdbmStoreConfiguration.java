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
package org.apache.directory.server.core.partition.impl.btree.jdbm;

import java.io.File;
import java.util.Set;

import javax.naming.directory.Attributes;

import org.apache.directory.server.schema.registries.AttributeTypeRegistry;
import org.apache.directory.server.schema.registries.OidRegistry;


public class JdbmStoreConfiguration
{
    private AttributeTypeRegistry attributeTypeRegistry;
    private OidRegistry oidRegistry;
    private File workingDirectory;
    private Set indexedAttributes;
    private Attributes contextEntry;
    private String suffixDn;
    private boolean isSyncOnWrite;
    private boolean enableOptimizer;
    private int cacheSize = 100;
    private String name;
    
    
    public void setAttributeTypeRegistry( AttributeTypeRegistry attributeTypeRegistry )
    {
        this.attributeTypeRegistry = attributeTypeRegistry;
    }
    
    
    public AttributeTypeRegistry getAttributeTypeRegistry()
    {
        return attributeTypeRegistry;
    }
    
    
    public void setOidRegistry( OidRegistry oidRegistry )
    {
        this.oidRegistry = oidRegistry;
    }
    
    
    public OidRegistry getOidRegistry()
    {
        return oidRegistry;
    }
    
    
    public void setWorkingDirectory( File workingDirectory )
    {
        this.workingDirectory = workingDirectory;
    }
    
    
    public File getWorkingDirectory()
    {
        return workingDirectory;
    }
    
    
    public void setIndexedAttributes( Set indexedAttributes )
    {
        this.indexedAttributes = indexedAttributes;
    }
    
    
    public Set getIndexedAttributes()
    {
        return indexedAttributes;
    }
    
    
    public void setContextEntry( Attributes contextEntry )
    {
        this.contextEntry = contextEntry;
    }
    
    
    public Attributes getContextEntry()
    {
        return contextEntry;
    }
    
    
    public void setSuffixDn( String suffixDn )
    {
        this.suffixDn = suffixDn;
    }
    
    
    public String getSuffixDn()
    {
        return suffixDn;
    }


    public void setSyncOnWrite( boolean isSyncOnWrite )
    {
        this.isSyncOnWrite = isSyncOnWrite;
    }


    public boolean isSyncOnWrite()
    {
        return isSyncOnWrite;
    }


    public void setEnableOptimizer( boolean enableOptimizer )
    {
        this.enableOptimizer = enableOptimizer;
    }


    public boolean isEnableOptimizer()
    {
        return enableOptimizer;
    }


    public void setCacheSize( int cacheSize )
    {
        this.cacheSize = cacheSize;
    }


    public int getCacheSize()
    {
        return cacheSize;
    }


    public void setName( String name )
    {
        this.name = name;
    }


    public String getName()
    {
        return name;
    }
}
