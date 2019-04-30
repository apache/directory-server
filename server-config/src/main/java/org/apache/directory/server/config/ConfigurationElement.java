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
package org.apache.directory.server.config;


import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;


/**
 * An annotation used to specify that the qualified field is configuration element.
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 */
@Documented
@Inherited
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.FIELD)
public @interface ConfigurationElement
{
    /**
     * Returns the attribute type.
     *
     * @return the attribute type
     */
    String attributeType() default "";


    /**
     * Returns the object class.
     *
     * @return the object class
     */
    String objectClass() default "";


    /**
     * Returns true if of the qualified field (attribute type and value) 
     * is the Rdn of the entry.
     *
     * @return <code>true</code> if of the qualified field (attribute type and value) 
     * is the Rdn of the entry,
     *         <code>false</code> if not.
     */
    boolean isRdn() default false;


    /**
     * Returns the string value of the Dn of the container.
     *
     * @return the string value of the Dn of the container.
     */
    String container() default "";


    /**
     * Returns true if the qualified field is optional.
     *
     * @return <code>true</code> if the qualified field is optional,
     *         <code>false</code> if not.
     */
    boolean isOptional() default false;


    /**
     * Returns the string value of the default value.
     *
     * @return the string value of the default value
     */
    String defaultValue() default "";
    
    /**
     * Returns the list of string values that are the 
     * default values when the element is multi-valued.
     * 
     * @return The default values
     */
    String[] defaultValues() default {};
}
