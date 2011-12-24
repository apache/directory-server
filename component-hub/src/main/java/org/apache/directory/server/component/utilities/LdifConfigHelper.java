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
package org.apache.directory.server.component.utilities;


import java.util.Properties;

import org.apache.directory.server.component.ADSComponent;
import org.apache.directory.server.component.instance.ComponentInstance;
import org.apache.directory.shared.ldap.model.ldif.LdifEntry;


public class LdifConfigHelper
{

    /**
     * Transforms a component instance into its corresponding entry in ldif format.
     *
     * @param instance ComponentInstance to transform into Ldif entry.
     * @return Generated Ldif entry.
     */
    public static LdifEntry instanceToLdif( ComponentInstance instance )
    {
        return null;
    }


    /**
     * It extracts the configuration information from Ldif entry.
     *
     * @param entry LdifEntry reference to extract instance configuration
     * @return Extracted instance configuration
     */
    public static Properties instanceEntryToConfiguration( LdifEntry entry )
    {
        return null;
    }
}
