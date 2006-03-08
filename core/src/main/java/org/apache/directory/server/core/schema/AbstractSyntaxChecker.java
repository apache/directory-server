/*
 *   Copyright 2004 The Apache Software Foundation
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
package org.apache.directory.server.core.schema;


import org.apache.directory.shared.ldap.schema.SyntaxChecker;


/**
 * Document me.
 *
 * @todo put me into shared-ldap
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 * @version $Rev$
 */
public abstract class AbstractSyntaxChecker implements SyntaxChecker
{
    /** the oid of the syntax checker */
    private String oid = null;


    /**
     * Creates a SyntaxChecker with a unique OID.
     *
     * @param oid the unique OID for the SyntaxChecker.
     */
    public AbstractSyntaxChecker(String oid)
    {
        this.oid = oid;
    }


    /**
     * Gets the OID of this SyntaxChecker.
     *
     * @return the OID of this SyntaxChecker.
     */
    public String getSyntaxOid()
    {
        return oid;
    }
}
