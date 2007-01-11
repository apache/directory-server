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
package org.apache.directory.shared.converter.schema;

import java.io.InputStream;
import java.io.Writer;


public class Schema
{
    private String name;
    private String[] dependencies;
    private String pkg;
    private String owner;
    private InputStream in; 
    private Writer out;


    public void setDependencies( String[] dependencies )
    {
        this.dependencies = dependencies;
    }


    public String[] getDependencies()
    {
        return dependencies;
    }


    public void setPkg( String pkg )
    {
        this.pkg = pkg;
    }


    public String getPkg()
    {
        return pkg;
    }


    public void setOwner( String owner )
    {
        this.owner = owner;
    }


    public String getOwner()
    {
        return owner;
    }


    public void setName( String name )
    {
        this.name = name;
    }


    public String getName()
    {
        return name;
    }
    
    public void setInput( InputStream in )
    {
        this.in = in;
    }
    
    public InputStream getInput()
    {
        return in;
    }


    public Writer getOutput()
    {
        return out;
    }


    public void setOutput( Writer out )
    {
        this.out = out;
    }
}
