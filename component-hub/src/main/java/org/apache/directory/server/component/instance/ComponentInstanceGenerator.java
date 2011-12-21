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
package org.apache.directory.server.component.instance;


import java.util.Properties;

import org.apache.directory.server.component.ADSComponent;
import org.apache.felix.ipojo.Factory;


public interface ComponentInstanceGenerator
{
    /**
     * Creates an instance of a supplied ADSComponent.
     * Generates actual java object an LdifEntry describing it.
     *
     * @param component ADSComponent reference to be instantiated
     * @return ADSInstance reference created from ADSComponent
     */
    public ComponentInstance createInstance( ADSComponent component, Properties properties );


    /**
     * Extract default configuration from factory. This configuration is 
     * the map of default values of the published properties of a IPojo component.
     * 
     * If some mandatory property has no default value, then this method returns null !
     *
     * @param factory Factory reference to extract default configuration to instantiate it.
     * @return Default configuration
     */
    public Properties extractDefaultConfiguration( ADSComponent component );

}
