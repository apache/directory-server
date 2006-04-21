/*
 *   Copyright 2006 The Apache Software Foundation
 *
 *   Licensed under the Apache License, Version 2.0 (the "License");
 *   you may not use this file except in compliance with the License.
 *   You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *   Unless required by applicable law or agreed to in writing, software
 *   distributed under the License is distributed on an "AS IS" BASIS,
 *   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *   See the License for the specific language governing permissions and
 *   limitations under the License.
 *
 */


package org.apache.directory.server.ldap.support.extended;

import org.apache.directory.server.core.jndi.ServerLdapContext;
import org.apache.directory.shared.ldap.codec.extended.operations.StoredProcedure;


/**
 * An extension (hook) point that enables an implementor to provide his or her
 * own Language Specific Stored Procedure Extended Operation handler.  
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 * @version $Rev$ $Date$
 */
public interface LanguageSpecificStoredProceureExtendedOperationHandler
{
    void handleStoredProcedureExtendedOperation( ServerLdapContext ctx, StoredProcedure spBean ) throws Exception;
}
