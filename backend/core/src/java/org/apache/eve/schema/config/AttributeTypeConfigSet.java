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
package org.apache.eve.schema.config;


import org.apache.ldap.common.schema.AttributeType;
import org.apache.eve.schema.SyntaxRegistry;
import org.apache.eve.schema.MatchingRuleRegistry;
import org.apache.eve.schema.AttributeTypeRegistry;

import javax.naming.NamingException;


/**
 * Document me.
 *
 * @author <a href="mailto:directory-dev@incubator.apache.org">Apache Directory Project</a>
 * @version $Rev$
 */
public interface AttributeTypeConfigSet
{
    AttributeType[] load( SyntaxRegistry syntaxReg, 
                          MatchingRuleRegistry mruleReg,
                          AttributeTypeRegistry attrTypeReg ) throws NamingException;
}
