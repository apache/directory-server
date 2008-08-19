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


import javax.naming.NamingException;

import org.apache.directory.server.schema.bootstrap.ProducerTypeEnum;
import org.apache.directory.server.schema.registries.Registries;


/**
 * A schema object producer which uses a callback to announce object creation
 * rather than completely returning objects in bulk. This way registries can
 * be populated while the producer is doing is creating schema objects.
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 * @version $Rev$
 */
public interface BootstrapProducer
{
    /**
     * Gets the type of producer this is.
     *
     * @return the type of the BootstrapProducer as a enum
     */
    ProducerTypeEnum getType();


    /**
     * Produces schema objects announcing each one after creation via the
     * callback before continuing on to create more objects.
     *
     * @param registries the registry set used by this producer
     * @param cb the producer's callback
     * @throws NamingException callbacks often operate upon registries and can
     * throw these exceptions so we must throw this as well since
     * implementations will have to call the callback methods
     */
    void produce( Registries registries, ProducerCallback cb ) throws NamingException;
}
