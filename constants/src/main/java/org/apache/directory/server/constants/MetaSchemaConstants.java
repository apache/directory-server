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
    public static final Object META_SCHEMA_OC = "metaSchema";
    public static final String META_OBJECT_CLASS_OC = "metaObjectClass";
    public static final String META_ATTRIBUTE_TYPE_OC = "metaAttributeType";
    public static final String META_MATCHING_RULE_OC = "metaMatchingRule";
    public static final String META_NORMALIZER_OC = "metaNormalizer";
    public static final String META_SYNTAX_OC = "metaSyntax";
    public static final String META_SYNTAX_CHECKER_OC = "metaSyntaxChecker";
    public static final String META_COMPARATOR_OC = "metaComparator";

    public static final String M_SUP_OBJECT_CLASS_AT = "m-supObjectClass";
    public static final String M_BYTECODE_AT = "m-bytecode";
    public static final String M_FQCN_AT = "m-fqcn";
    public static final String M_DEPENDENCIES_AT = "m-dependencies";
    public static final String M_DISABLED_AT = "m-disabled";
    public static final String M_OWNER_AT = "m-owner";
    public static final String M_DESCRIPTION_AT = "m-description";
    public static final String M_OBSOLETE_AT = "m-obsolete";
    public static final String M_NAME_AT = "m-name";
    public static final String M_OID_AT = "m-oid";
    public static final String M_USAGE_AT = "m-usage";
    public static final String M_NO_USER_MODIFICATION_AT = "m-noUserModification";
    public static final String M_SINGLE_VALUE_AT = "m-singleValue";
    public static final String M_COLLECTIVE_AT = "m-collective";
    public static final String M_SUBSTR_AT = "m-substr";
    public static final String M_SUP_ATTRIBUTE_TYPE_AT = "m-supAttributeType";
    public static final String M_ORDERING_AT = "m-ordering";
    public static final String M_EQUALITY_AT = "m-equality";
    public static final String M_SYNTAX_AT = "m-syntax";
    public static final String M_DESC_AT = "m-description";
    public static final String M_MUST_AT = "m-must";
    public static final String M_MAY_AT = "m-may";
    public static final String M_TYPE_OBJECT_CLASS_AT = "m-typeObjectClass";
    public static final String X_HUMAN_READIBLE_AT = "x-humanReadible";
}
