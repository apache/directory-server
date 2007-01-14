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


/**
 * A bean used to hold a schema. We keep its name and we associate whith this
 * object an inputStream mapped on the OpenLdap schema to read, and a writer
 * in which the ldif file will be dumped.
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 */
public class Schema
{
    /** The schema name */
    private String name;
    
    /** The inputStream mapped on the file to read */
    private InputStream in; 
    
    /** The writer where we dump the ldif lines */
    private Writer out;

    /**
     * Set the schema name to parse. This name is the prefix of the
     * schema file, which postifx is '.schema'.
     * 
     * For instance, 'test.schema' being the file to parse, its name 
     * will be 'test'
     * @param name
     */
    public void setName( String name )
    {
        this.name = name;
    }

    /**
     * @return The schema name.
     */
    public String getName()
    {
        return name;
    }
    
    /**
     * Set the inputStream mapped on the schema file
     * @param in The InputStream mapped on the schema file
     */
    public void setInput( InputStream in )
    {
        this.in = in;
    }
    
    /**
     * @return The InputStream mapped on the schema file
     */
    public InputStream getInput()
    {
        return in;
    }

    /**
     * @return The writer in which the ldif lines will be dumped
     */
    public Writer getOutput()
    {
        return out;
    }

    /**
     * Set a writer to dump the ldif files
     * @param out The writer 
     */
    public void setOutput( Writer out )
    {
        this.out = out;
    }
}
