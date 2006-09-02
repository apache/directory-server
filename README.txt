This directory was constructed to reproduct the same environment in trunks for
the 1.0 apacheds branch.  This is done to allow things like the proper 
generation of eclipse descriptors and to be able to build from top down.

The following versions and branches are associated with projects in this folder.
If there are any questions just take a look at the svn:externals set in this 
folder.

apacheds branch 1.0
daemon branch 1.0
shared branch 0.9.5 
mina releases

Before committing after making changes make sure that all integration tests pass
for apacheds.  You can run these integration tests with maven like so:

   mvn -Dintegration test 

Thanks,
The Apache Directory Team
