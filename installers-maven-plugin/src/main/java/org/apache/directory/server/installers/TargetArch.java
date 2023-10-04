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
package org.apache.directory.server.installers;

/**
 * The various installer architectures
 * 
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 */
public enum TargetArch
{
    /** The OS architecture for 'amd64' */
    OS_ARCH_AMD64( "amd64" ),
    
    /** The OS architecture for 'Any' */
    OS_ARCH_ANY( "Any" ),
    
    /** The OS architecture for 'i386' */
    OS_ARCH_I386( "i386" ),
    
    /** The OS architecture for 'sparc' */
    OS_ARCH_SPARC( "sparc" ),
    
    /** The OS architecture for 'x86' */
    OS_ARCH_X86( "x86" ),
    
    /** The OS architecture for 'x86_64' */
    OS_ARCH_X86_64( "x86_64" );
    
    /** The internal name */
    private String value;
    
    /**
     * 
     * Creates a new instance of TargetArch.
     *
     * @param value The interned value
     */
    TargetArch( String value ) 
    {
        this.value = value;
    }

    /**
     * @return The interned valuue
     */
    public String getValue()
    {
        return value;
    }
}
