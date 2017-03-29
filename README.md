# SAPLaMa-Openstack-Adapters
Virtualization and storage adapters that enable users to manage their OpenStack clouds via SAP Landscape Management (SAP LaMa), including all SAP LaMa functions e.g. copy, clone, relocate, refresh, etc. While these adapters, provided by SAP, are tested with Suse Cloud 6, everyone can customize them to work with their own environments.

* This storage adapter is for SAP LaMa 3.X releases only  
* This build is based on Openstack4j 3.0.2
* This build needs Java 8
* This build supports V3 authentication with SAP LaMa 3.X - e.g. http://Openstack_hostname:5000/v3.0
* This build was tested on SUSE Cloud 6 with Netapp storage as backend
*

## How is it done?

This OpenStack virtualization adapter and storage adapter that connects SAP LaMa to OpenStack works similarly to other SAP LaMa adapters, the main difference being that this one is open source with Apache License Version 2.0. We use Apache Maven (https://maven.apache.org/) to build the war files and package them into the deployable ear file. We used the OpenStack4j (http://openstack4j.com/) library to interact with the OpenStack server web services. These dependencies will be automatically downloaded from maven repository during the build process.  


##Building, installing and using the adapters


The OpenStack adapters, StorageManager  and Virtualization Manager, compile to a war file and are packaged into a single deployable ear file.


Here are the instructions for downloading, compiling and deploying the SAP LaMa Openstack Virtualization and Storage adapters. 

note: rather than go through the build steps below, users can deploy the  precompiled .ear file in https://github.com/SAP/SAPLaMa-Openstack-Adapters/blob/master/LaMaAdaptersApp/target/LaMaAdaptersApp-1.3.0.ear

You need git client installed locally to clone the project, I’ve tested with: git version 2.11.0.windows.1

Steps to build and deploy:

### Clone the project
```
 git clone https://github.com/SAP/SAPLaMa-Openstack-Adapters
```
###Build the project

You will need maven installed to package the ear file (tested with [Apache Maven 3.2.5](http://archive.apache.org/dist/maven/maven-3/3.2.5/binaries/); does not work with maven 2.x):

```
cd LaMa-openstack-adapters
mvn clean package
```

Note: For maven build be sure to set your JAVA_HOME variable to a Java 1.8 jdk (tested with jdk1.8.0_112)

###Deploy project
You should now have an ear file in LaMa-openstack-adapters\LaMaAdaptersApp\target\LaMaAdaptersApp-1.0.ear 

You can deploy this using eclipse: open the deploy view, select External Deployable Archives, find the ear file in the popup, right click and select deploy.

The EAR file can de deployed either via the IDE, or using the Telnet commands.


In case you want to deploy it using Telnet, these are the steps you must follow:
  0. Copy the File you need to deploy to the SAP LaMa machine and then deploy it via Telnet: 
  1.  Open a Telnet connection to the AS Java on which you want to deploy the application. On Windows, you can do this from a DOS prompt with the command:
telnet localhost <port>,
where <port> is the Telnet port of your server. For example, if your server installation is c:\usr\sap\<some_three_letter_SID>\JCxx\..., then your Telnet port should be 5xx08.
  2.  Log on using your AS Java administrator user name and password.
  3.  When you have logged on, type the following Telnet commands:
> lsc

This will list the cluster elements. Find "Server 0", look the value in the "ID" column and type your next command:

> jump x

where x is this ID. Then type these commands:

> add deploy
> deploy <path to the location of the file >\LaMaAdaptersApp-1.0.ear

For more information on telnet deploy see: http://help.sap.com/saphelp_banking50/helpdata/en/44/ee4a09d85a627de10000000a155369/content.htm


###Configure and Use
Now you should see the OpenStack adapters in SAP LaMa. The configuration procedure is similar to other adapters:
  1.  navigate to Infrastructure->Virtualization Managers 
  2.  select add 
  3.  choose Openstack
 

Fill in the form with Openstack connection details:
* OpenStack username 
* OpenStack password
* URL - e.g. http://Openstack_hostname:5000/v3.0
* Region - e.g. Default
* Tenant - also known as "Project" 
* Domain - e.g. Default 
* Project -  
 
Now add the Openstack Storage Manager. Navigate to Infrastructure->Storage Managers and repeat the process above to add the Openstack adapter.

You should now see a list of Openstack VMs under Operations -> Virtualization and a list of volumes under Operations->Storage.


##Code structure in github repository

The top level contains the following directories:
LaMaAdapters:  contains java sources for the Openstack storage manager virtualization manager adapters and a pom.xml to build the war file
LaMaAdaptersApp: App project, contains a pom.xml that builds the deployable ear file containing the above war file and some SAP specific files                  


Note: To make modifications to the code in eclipse, first generate the eclipse files(after packaging):
```
mvn  eclipse:eclipse
```

Then import the external projects under File->Import Existing projects into workspace and then navigate to the LaMa-openstack-adapters directory

Note: You may need to manually set your M2_REPO variable in the eclipse Java Build path view for the LaMaAdapters project (in Windows this is typically in c:\users\\\<username>\\.m2\repository) 

##Other maven commands
You can use maven to generate the javadocs by excuting: 
* mvn javadoc:javadoc 

Then use your browser to open: LaMa-openstack-adapters/LaMaAdapters/target/site/apidocs/index.html
