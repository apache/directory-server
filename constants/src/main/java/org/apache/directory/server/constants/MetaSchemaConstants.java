/*
 *   Licensed to the Apache Software Foundation (ASF) under one
 *   or more contributor license agreements.  See the NOTICE file
 *   distributed with this work for additional information
 *   regarding copyright ownership.  The ASF licenses this file
 *   to you under the Apache License, Version 2.0 (the
 *   "License"); you may not use this file except in compliance
 *   with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 *   Unless required by applicable law or agreed to in writing,
 *   software distributed under the License is distributed on an
 *   "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 *   KIND, either express or implied.  See the License for the
 *   specific language governing permissions and limitations
 *   under the License.
 *
 */
package org.apache.directory.server.constants;


/**
 * Apache meta schema specific constants used throughout the server.
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 * @version $Rev$, $Date$
 */
public interface MetaSchemaConstants
{
    String SCHEMA_NAME = "apachemeta";

    // -- objectClass names --
    
    String META_TOP_OC = "metaTop";
    String META_SCHEMA_OC = "metaSchema";
    String META_OBJECT_CLASS_OC = "metaObjectClass";
    String META_ATTRIBUTE_TYPE_OC = "metaAttributeType";
    String META_MATCHING_RULE_OC = "metaMatchingRule";
    String META_NORMALIZER_OC = "metaNormalizer";
    String META_SYNTAX_OC = "metaSyntax";
    String META_SYNTAX_CHECKER_OC = "metaSyntaxChecker";
    String META_COMPARATOR_OC = "metaComparator";
    String META_NAME_FORM_OC = "metaNameForm";
    String META_DIT_CONTENT_RULE_OC = "metaDITContentRule";

    // -- attributeType names --
    
    String M_SUP_OBJECT_CLASS_AT = "m-supObjectClass";
    String M_BYTECODE_AT = "m-bytecode";
    String M_FQCN_AT = "m-fqcn";
    String M_DEPENDENCIES_AT = "m-dependencies";
    String M_DISABLED_AT = "m-disabled";
    String M_DESCRIPTION_AT = "m-description";
    String M_OBSOLETE_AT = "m-obsolete";
    String M_NAME_AT = "m-name";
    String M_OID_AT = "m-oid";
    String M_OC_AT = "m-oc";
    String M_AUX_AT = "m-aux";
    String M_USAGE_AT = "m-usage";
    String M_NO_USER_MODIFICATION_AT = "m-noUserModification";
    String M_SINGLE_VALUE_AT = "m-singleValue";
    String M_COLLECTIVE_AT = "m-collective";
    String M_SUBSTR_AT = "m-substr";
    String M_SUP_ATTRIBUTE_TYPE_AT = "m-supAttributeType";
    String M_ORDERING_AT = "m-ordering";
    String M_EQUALITY_AT = "m-equality";
    String M_SYNTAX_AT = "m-syntax";
    String M_MUST_AT = "m-must";
    String M_MAY_AT = "m-may";
    String M_TYPE_OBJECT_CLASS_AT = "m-typeObjectClass";
    String X_HUMAN_READABLE_AT = "x-humanReadable";
    
    // -- schema extensions & values --
    
    String X_SCHEMA = "X-SCHEMA";
    String X_IS_HUMAN_READABLE = "X-IS-HUMAN-READABLE";
    String SCHEMA_OTHER = "other";
}
