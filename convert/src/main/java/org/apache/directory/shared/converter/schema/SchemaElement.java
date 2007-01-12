
package org.apache.directory.shared.converter.schema;

import java.util.List;

public interface SchemaElement
{
    boolean isObsolete();
    String getOid();
    String getDescription();
    List<String> getNames();
    void setNames( List<String> names );
    String getShortAlias();
}
