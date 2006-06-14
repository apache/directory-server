package org.apache.directory.shared.ldap.schema;

import java.util.Map;

import javax.naming.NamingException;


/**
 * A class is used to resolve the normalizer mapping hash used for normalization.
 * This interface is implemented and passed into several kinds of parsers that
 * need to handle the normalization of LDAP name strings.
 * 
 * Why you may ask are we doing this?  Why not just pass in the map of 
 * normalizers to these parsers and let them use that?  First off this mapping
 * will not be static when dynamic updates are enabled to schema.  So if
 * we just passed in the map then there would be no way to set a new map or
 * trigger the change of the map when schema changes.  Secondly we cannot just
 * pass server side objects that return this mapping because these parsers may
 * and will be used in client side applications.  They will not have access to
 * these server side objects that generate these mappings.  Instead when a 
 * resolver is used we can create mock or almost right implementations.
 * 
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 * @version $Rev$
 */
public interface NormalizerMappingResolver
{
    Map getNormalizerMapping() throws NamingException;
}
