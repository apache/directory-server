package org.apache.directory.server;


public class HelloWorldProcedure
{
    public static String sayHello()
    {
        return "Hello World!";
    }
    
    public static String sayHelloTo( String name )
    {
        return "Hello " + name + "!";
    }
}
