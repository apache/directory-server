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


import org.apache.directory.server.core.authn.SimpleAuthenticationIT;
import org.apache.directory.server.core.collective.CollectiveAttributeServiceIT;
import org.apache.directory.server.core.event.EventServiceIT;
import org.apache.directory.server.core.exception.ExceptionServiceIT;
import org.apache.directory.server.core.integ.FrameworkSuite;
import org.apache.directory.server.core.jndi.AddIT;
import org.apache.directory.server.core.jndi.CreateContextIT;
import org.apache.directory.server.core.jndi.DestroyContextIT;
import org.junit.runner.RunWith;
import org.junit.runners.Suite;


/**
 * Document me!
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 * @version $Rev$, $Date$
 */
@RunWith ( FrameworkSuite.class )
@Suite.SuiteClasses ( {
        SimpleAuthenticationIT.class,
        CollectiveAttributeServiceIT.class,
        ExceptionServiceIT.class,
        EventServiceIT.class,
        AddIT.class,
        CreateContextIT.class,
        DestroyContextIT.class/*,
        PartitionConfigurationIT.class,
        DIRSERVER169IT.class,
        DIRSERVER759IT.class,
        DIRSERVER783IT.class,
        DIRSERVER791IT.class,
        ListIT.class,
        ObjStateFactoryIT.class,
        ExtensibleObjectIT.class,
        ModifyContextIT.class,
        RFC2713IT.class,
        RootDSEIT.class,
        SearchIT.class,
        UniqueMemberIT.class,
        OperationalAttributeServiceIT.class,
        PreferencesIT.class,
        TriggerInterceptorIT.class,
        SubentryServiceForTriggersIT.class,
        BadSubentryServiceIT.class,
        SubentryServiceEntryModificationHandlingIT.class,
        SubentryServiceObjectClassChangeHandlingIT.class,
        SubentryServiceIT.class,
        LdapClassLoaderIT.class,
        NormalizationServiceIT.class,
        DefaultChangeLogIT.class*/
        } )
public class StockCoreISuite2
{
}
