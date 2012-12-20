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
package org.apache.directory.server.core.trigger;


import java.util.List;

import org.apache.directory.api.ldap.model.exception.LdapException;
import org.apache.directory.api.ldap.trigger.StoredProcedureParameter;
import org.apache.directory.server.core.api.interceptor.context.OperationContext;


/**
 * TODO - can we get some documentation on this?
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 */
public interface StoredProcedureParameterInjector
{
    List<Object> getArgumentsToInject( OperationContext opContext,
        List<StoredProcedureParameter> parameterList ) throws LdapException;

    public interface MicroInjector
    {
        Object inject( OperationContext opContext, StoredProcedureParameter param ) throws LdapException;
    }
}
