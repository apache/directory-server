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
package org.apache.directory.server.core.partition;


import java.util.Set;

import org.apache.directory.server.core.entry.ClonedServerEntry;
import org.apache.directory.server.core.interceptor.context.AddContextPartitionOperationContext;
import org.apache.directory.server.core.interceptor.context.CompareOperationContext;
import org.apache.directory.server.core.interceptor.context.GetMatchedNameOperationContext;
import org.apache.directory.server.core.interceptor.context.GetRootDSEOperationContext;
import org.apache.directory.server.core.interceptor.context.GetSuffixOperationContext;
import org.apache.directory.server.core.interceptor.context.ListSuffixOperationContext;
import org.apache.directory.server.core.interceptor.context.RemoveContextPartitionOperationContext;
import org.apache.directory.shared.ldap.name.DN;
import org.apache.directory.shared.ldap.util.StringTools;


/**
 * A root {@link Partition} that contains all other partitions, and
 * routes all operations to the child partition that matches to its base suffixes.
 * It also provides some extended operations such as accessing rootDSE and
 * listing base suffixes.
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 * @version $Rev: 927404 $, $Date: 2010-03-25 14:55:18 +0100 (Do, 25. Mär 2010) $
 */
public interface PartitionNexus extends Partition
{

    /** the admin super user uid */
    public static final String ADMIN_UID = "admin";
    
    /** the initial admin passwd set on startup */
    public static final String ADMIN_PASSWORD_STRING = "secret";
    
    public static final byte[] ADMIN_PASSWORD_BYTES = StringTools.getBytesUtf8( ADMIN_PASSWORD_STRING );


    /**
     * Get's the RootDSE entry for the DSA.
     *
     * @return the attributes of the RootDSE
     */
    public ClonedServerEntry getRootDSE( GetRootDSEOperationContext getRootDSEContext );


    /**
     * Add a partition to the server.
     * 
     * @param opContext The Add Partition context
     * @throws Exception If the addition can't be done
     */
    public void addContextPartition( AddContextPartitionOperationContext opContext ) throws Exception;


    /**
     * Remove a partition from the server.
     * 
     * @param opContext The Remove Partition context
     * @throws Exception If the removal can't be done
     */
    public void removeContextPartition( RemoveContextPartitionOperationContext removeContextPartition )
        throws Exception;


    /**
     * @return The ou=system partition 
     */
    public Partition getSystemPartition();


    /**
     * Get's the partition corresponding to a distinguished name.  This 
     * name need not be the name of the partition suffix.  When used in 
     * conjunction with get suffix this can properly find the partition 
     * associated with the DN.  Make sure to use the normalized DN.
     * 
     * @param dn the normalized distinguished name to get a partition for
     * @return the partition containing the entry represented by the dn
     * @throws Exception if there is no partition for the dn
     */
    public Partition getPartition( DN dn ) throws Exception;


    /**
     * Gets the most significant Dn that exists within the server for any Dn.
     *
     * @param getMatchedNameContext the context containing the  distinguished name 
     * to use for matching.
     * @return a distinguished name representing the matching portion of dn,
     * as originally provided by the user on creation of the matched entry or 
     * the empty string distinguished name if no match was found.
     * @throws Exception if there are any problems
     */
    public DN getMatchedName( GetMatchedNameOperationContext matchedNameContext ) throws Exception;


    /**
     * Gets the distinguished name of the suffix that would hold an entry with
     * the supplied distinguished name parameter.  If the DN argument does not
     * fall under a partition suffix then the empty string Dn is returned.
     *
     * @param suffixContext the Context containing normalized distinguished
     * name to use for finding a suffix.
     * @return the suffix portion of dn, or the valid empty string Dn if no
     * naming context was found for dn.
     * @throws Exception if there are any problems
     */
    public DN getSuffix( GetSuffixOperationContext getSuffixContext ) throws Exception;


    /**
     * Gets an iteration over the Name suffixes of the partitions managed by this
     * {@link DefaultPartitionNexus}.
     *
     * @return Iteration over ContextPartition suffix names as Names.
     * @throws Exception if there are any problems
     */
    public Set<String> listSuffixes( ListSuffixOperationContext emptyContext ) throws Exception;


    /**
     * Adds a set of supportedExtension (OID Strings) to the RootDSE.
     * 
     * @param extensionOids a set of OID strings to add to the supportedExtension 
     * attribute in the RootDSE
     */
    public void registerSupportedExtensions( Set<String> extensionOids ) throws Exception;


    /**
     * Adds a set of supportedSaslMechanisms (OID Strings) to the RootDSE.
     * 
     * @param extensionOids a set of OID strings to add to the supportedSaslMechanisms 
     * attribute in the RootDSE
     */
    public void registerSupportedSaslMechanisms( Set<String> supportedSaslMechanisms ) throws Exception;


    public boolean compare( CompareOperationContext opContext ) throws Exception;
}