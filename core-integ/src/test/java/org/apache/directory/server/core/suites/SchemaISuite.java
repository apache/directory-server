/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.directory.server.core.suites;


import org.apache.directory.server.core.annotations.CreateDS;
import org.apache.directory.server.core.integ.FrameworkSuite;
import org.apache.directory.server.core.schema.MetaAttributeTypeHandlerIT;
import org.apache.directory.server.core.schema.MetaComparatorHandlerIT;
import org.apache.directory.server.core.schema.MetaMatchingRuleHandlerIT;
import org.apache.directory.server.core.schema.MetaNormalizerHandlerIT;
import org.apache.directory.server.core.schema.MetaObjectClassHandlerIT;
import org.apache.directory.server.core.schema.MetaSchemaHandlerIT;
import org.apache.directory.server.core.schema.MetaSyntaxCheckerHandlerIT;
import org.apache.directory.server.core.schema.MetaSyntaxHandlerIT;
import org.apache.directory.server.core.schema.ObjectClassCreateIT;
import org.apache.directory.server.core.schema.SchemaPersistenceIT;
import org.apache.directory.server.core.schema.SchemaServiceIT;
import org.junit.runner.RunWith;
import org.junit.runners.Suite;


/**
 * Document me!
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 * @version $Rev$, $Date$
 */
@RunWith ( FrameworkSuite.class )
@CreateDS( name="SchemaISuite" )
@Suite.SuiteClasses ( {
        MetaAttributeTypeHandlerIT.class,
        MetaComparatorHandlerIT.class,
        MetaMatchingRuleHandlerIT.class,
        MetaNormalizerHandlerIT.class,
        MetaObjectClassHandlerIT.class,
        MetaSchemaHandlerIT.class,
        MetaSyntaxCheckerHandlerIT.class,
        MetaSyntaxHandlerIT.class,
        ObjectClassCreateIT.class,
        SchemaPersistenceIT.class,
        //SubschemaSubentryIT.class,
        SchemaServiceIT.class
        } )
public class SchemaISuite
{
}
