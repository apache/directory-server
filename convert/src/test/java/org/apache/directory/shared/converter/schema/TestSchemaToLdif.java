package org.apache.directory.shared.converter.schema;
import java.io.OutputStreamWriter;
import java.util.ArrayList;
import java.util.List;

import junit.framework.TestCase;



public class TestSchemaToLdif extends TestCase
{
    public void testConvert() throws ParserException
    {
        SchemaToLdif converter = new SchemaToLdif();
        
        List<Schema> schemas = new ArrayList<Schema>();
        Schema schema = new Schema();
        schema.setName( "test.schema" );
        schema.setInput( getClass().getResourceAsStream( "test.schema" ) );
        schema.setOutput( new OutputStreamWriter( System.out ) );
        schemas.add( schema );
        
        converter.transform( schemas );
    }
}
