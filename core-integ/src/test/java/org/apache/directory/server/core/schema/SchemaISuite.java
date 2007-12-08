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
package org.apache.directory.server.core.schema;

import org.apache.directory.server.core.integ.CiSuite;
import org.apache.directory.server.core.integ.ServiceScope;
import org.apache.directory.server.core.integ.SetupMode;
import org.apache.directory.server.core.integ.annotations.Mode;
import org.apache.directory.server.core.integ.annotations.Scope;
import org.junit.runner.RunWith;
import org.junit.Ignore;
import org.junit.runners.Suite;


/**
 * Document me!
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 * @version $Rev$, $Date$
 */
@RunWith ( CiSuite.class )
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
        SubschemaSubentryIT.class,
        SchemaServiceIT.class
        } )
@Scope ( ServiceScope.TESTSUITE )
public class SchemaISuite
{
}
