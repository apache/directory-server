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
package org.apache.eve.schema.bootstrap;


import org.apache.eve.schema.SyntaxCheckerRegistry;

import java.util.Map;


/**
 * Factory that creates an OID String to Syntax map.
 *
 * @author <a href="mailto:directory-dev@incubator.apache.org">Apache Directory Project</a>
 * @version $Rev$
 */
public interface SyntaxFactory
{
    /**
     * Creates a Syntax OID String to Syntax object map.
     *
     * @param registry a registry of SyntaxChecker objects
     * @return a Map of Syntax OID Strings to Syntax objects
     */
    Map getSyntaxes( SyntaxCheckerRegistry registry );
}
