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
package org.apache.directory.server.core.administrative;

import java.util.Map;

import org.apache.directory.shared.ldap.name.DN;

/**
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 */
public interface AdministrativePoint
{
    /**
     * @return The AdministrativePoint DN
     */
    DN getDn();


    /**
     * @return The AdministrativePoint UUID
     */
    String getUuid();


    /**
     * @return true if the AdministrativePoint is an AutonomousArea
     */
    boolean isAutonomous();


    /**
     * @return true if the AdministrativePoint is an InnerArea
     */
    boolean isInner();


    /**
     * @return true if the AdministrativePoint is a SpecificArea
     */
    boolean isSpecific();


    /**
     * @return The parent AdministrativePoint, if any
     */
    AdministrativePoint getParent();


    /**
     * Set the AdministrativePoint parent
     *
     * @param parent the AdministrativePoint parent
     */
    void setParent( AdministrativePoint parent );


    /**
     * @return The list of children AdministrativePoint. May be empty
     */
    Map<String, AdministrativePoint> getChildren();


    /**
     * Add an AdministrativePoint child
     *
     * @param child the AdministrativePoint child to add
     */
    void addChild( AdministrativePoint child );


    /**
     * Set the AdministrativePoint children
     *
     * @param children the AdministrativePoint children
     */
    void setChildren( Map<String, AdministrativePoint> children );
}
