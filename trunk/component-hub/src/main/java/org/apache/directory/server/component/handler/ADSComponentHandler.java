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
package org.apache.directory.server.component.handler;


import java.util.Dictionary;

import org.apache.felix.ipojo.ConfigurationException;
import org.apache.felix.ipojo.PrimitiveHandler;
import org.apache.felix.ipojo.annotations.Handler;
import org.apache.felix.ipojo.metadata.Element;


/**
 * IPojo handler for ADSComponent annotation.
 * It does not supposed to do anything. It is just to validate the custom-annotated component.
 *  
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 */
@Handler(namespace = "org.apache.directory.server.component.handler", name = "ADSComponent")
public class ADSComponentHandler extends PrimitiveHandler
{

    @Override
    public void configure( Element metadata, Dictionary configuration ) throws ConfigurationException
    {
        // TODO Auto-generated method stub

    }


    @Override
    public void stop()
    {
        // TODO Auto-generated method stub

    }


    @Override
    public void start()
    {
        // TODO Auto-generated method stub

    }

}
