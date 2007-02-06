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
 * Constants for the System schema.
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 * @version $Rev$, $Date$
 */
public interface SystemSchemaConstants
{
    public static final String SCHEMA_NAME = "system";

    public static final String CN_AT = "cn";
    public static final String OBJECT_CLASS_AT = "objectClass";
    public static final String CREATORS_NAME_AT = "creatorsName";
    public static final String CREAT_TIMESTAMP_AT = "createTimestamp";
    public static final String MODIFY_TIMESTAMP_AT = "modifyTimestamp";
    public static final String MODIFIERS_NAME_AT = "modifiersName";

    public static final String LDAP_SYNTAXES_AT = "ldapSyntaxes";
    public static final String MATCHING_RULES_AT = "matchingRules";
    public static final String ATTRIBUTE_TYPES_AT = "attributeTypes";
    public static final String OBJECT_CLASSES_AT = "objectClasses";
    public static final String MATCHING_RULE_USE_AT = "matchingRuleUse";
    public static final String DIT_STRUCTURE_RULES_AT = "ditStructureRules";
    public static final String DIT_CONTENT_RULES_AT = "ditContentRules";
    public static final String NAME_FORMS_AT = "nameForms";
}
