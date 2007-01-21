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


import java.util.Comparator;

import javax.naming.NamingException;

import jdbm.helper.StringComparator;

import org.apache.directory.server.schema.bootstrap.ProducerTypeEnum;
import org.apache.directory.server.schema.registries.Registries;
import org.apache.directory.shared.ldap.schema.ComparableComparator;
import org.apache.directory.shared.ldap.util.BigIntegerComparator;


/**
 * A producer of Comparator objects for the eve schema.
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 * @version $Rev$
 */
public class ApacheComparatorProducer extends AbstractBootstrapProducer
{
    public ApacheComparatorProducer()
    {
        super( ProducerTypeEnum.COMPARATOR_PRODUCER );
    }


    // ------------------------------------------------------------------------
    // BootstrapProducer Methods
    // ------------------------------------------------------------------------

    /**
     * @see org.apache.directory.server.schema.bootstrap.BootstrapProducer#produce(org.apache.directory.server.schema.registries.DefaultRegistries, ProducerCallback)
     */
    public void produce( Registries registries, ProducerCallback cb ) throws NamingException
    {
        Comparator comparator;

        // For exactDnAsStringMatch -> 1.3.6.1.4.1.18060.0.4.1.1.1
        comparator = new ComparableComparator();
        cb.schemaObjectProduced( this, "1.3.6.1.4.1.18060.0.4.1.1.1", comparator );

        // For bigIntegerMatch -> 1.3.6.1.4.1.18060.0.4.1.1.2
        comparator = new BigIntegerComparator();
        cb.schemaObjectProduced( this, "1.3.6.1.4.1.18060.0.4.1.1.2", comparator );

        // For jdbmStringMatch -> 1.3.6.1.4.1.18060.0.4.1.1.3
        comparator = new StringComparator();
        cb.schemaObjectProduced( this, "1.3.6.1.4.1.18060.0.4.1.1.3", comparator );

    }
}
