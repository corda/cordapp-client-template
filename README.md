<p align="center">
  <img src="https://www.corda.net/wp-content/uploads/2016/11/fg005_corda_b.png" alt="Corda" width="500">
</p>

# CorDapp Client Template - Kotlin

Welcome to the Kotlin CorDapp template. The CorDapp template is a stubbed-out CorDapp that you can use to bootstrap your own CorDapps. It also contains an example implementation of some simple Cordapp components that you may find helpful.

# Pre-Requisites

See https://docs.corda.net/getting-set-up.html.

This includes installing pre `1.8.0_161` versions of the Java Development Kit (JDK) and Gradle version `5.3` 

# Usage

## Build the project

To ensure everything is building correctly run open the command line at the root of the project and run `./gradlew clean build`

On success you can open the project in Intellij. There are two approaches you can follow. 

1. On launching Intellij click import project > from existing sources, in the project wizard and select the project folder you wish to import. Selecting auto-import will import the gradle modules you need. Use the default gradle wrapper.

2. If within an existing project window, select File > Open, choose the project folder. Navigate to File > Project Structure, select the Project tab on the left and choose your chosen JDK to point to Java Home directory for you JDK 1.8 and click Apply. Then go the the modules tab, import modules from the root project directory and click apply, then okay and finish. 

## Running the nodes

See https://docs.corda.net/tutorial-cordapp.html#running-the-example-cordapp.

This can be done by the network bootstrapper method simply by running the two scripts in the `script` folder. You might need to changed the file permissions first to do so. 

    cd scripts
    chmod +x deployNodes.sh runNodes.sh 
    ./deployNodes.sh 
    ./runNodes.sh

## Interacting with the nodes

### Shell

When started via the command line, each node will display an interactive shell:

    Welcome to the Corda interactive shell.
    Useful commands include 'help' to see what is available, and 'bye' to shut down the node.
    
    Tue Nov 06 11:58:13 GMT 2018>>>

You can use this shell to interact with your node. For example, enter `run networkMapSnapshot` to see a list of 
the other nodes on the network:

    Tue Nov 06 11:58:13 GMT 2018>>> run networkMapSnapshot
    [
      {
      "addresses" : [ "localhost:10002" ],
      "legalIdentitiesAndCerts" : [ "O=Notary, L=London, C=GB" ],
      "platformVersion" : 3,
      "serial" : 1541505484825
    },
      {
      "addresses" : [ "localhost:10005" ],
      "legalIdentitiesAndCerts" : [ "O=PartyA, L=London, C=GB" ],
      "platformVersion" : 3,
      "serial" : 1541505382560
    },
      {
      "addresses" : [ "localhost:10008" ],
      "legalIdentitiesAndCerts" : [ "O=PartyB, L=New York, C=US" ],
      "platformVersion" : 3,
      "serial" : 1541505384742
    },
      {
      "addresses" : [ "localhost:10011" ],
      "legalIdentitiesAndCerts" : [ "O=PartyC, L=New York, C=US" ],
      "platformVersion" : 3,
      "serial" : 1541505384742
    }
    ]
    
    Tue Nov 06 12:30:11 GMT 2018>>> 

You can find out more about the node shell [here](https://docs.corda.net/shell.html).

### Client Webserver

`clients/src/main/kotlin/com/template/webserver/` defines a simple Spring web server that connects to a node via RPC and 
allows you to interact with the node over HTTP. This connection is established via a proxy `NodeRPCConnection.kt` class.

Some helpful starter API endpoints are defined here:

     clients/src/main/kotlin/com/template/webserver/StandardController.kt
     
You can add and extend your own here:

     clients/src/main/kotlin/com/template/webserver/CustomController.kt


#### Running the webserver

#### via scripts

To start the webservers, navigate to the `scripts/webserver/` directory and run the bash scripts individually in separate terminal windows.

##### Via the command line

Each node has it's own corresponding Spring server that interacts with it via Node RPC Connection proxy which is essentially a wrapper around the CordaRPCClient class. You can start these web servers via gradle tasks for ease-of-development. 

    ./gradlew runPartyAServer 
    ./gradlew runPartyBServer
    ./gradlew runPartyCServer
    
These web servers are hosted on ports 50005, 50006 and 50007 respectively. You can test they have launched successfully by connecting to one of there endpoints.

    curl localhost:50005/api/status -> 200
    
The list of available endpoints to play with now are:
    
    /api/servertime
    /api/addresses
    /api/identities
    /api/platformversion
    /api/peers
    /api/notaries
    /api/flows/
    /api/states
    
You can add your own custom endpoints to the Spring Custom Controller, or any other controller of your choosing.


## Testing

### Quasar

Corda flows need to be instrumented using Quasar before they are run, so that they can be suspended mid-execution.

To achieve this in IntelliJ, you need to:

* Create a run config for your tests
* Open the run config and change the VM options to -ea -javaagent:PATH-TO-QUASAR-JAR
* In the CorDapp example and templates, quasar.jar is located at lib/quasar.jar, so you'd use -ea -javaagent:../lib/quasar.jar
* Alternatively, you can edit the default JUnit run config to use the Quasar javaagent by default, avoiding you having to do this every time you pick a new test to run.

## H2 Database

To install H2 db in your Corda node follow the [instruction here](https://docs.corda.net/head/node-database-access-h2.html?highlight=database#connecting-using-the-h2-console
)

Corda no longer gives you a jdbc port to connect an H2 client to by default. However, it is possible to configure the set up to do this by changing the node.config directly by [follow these instructions](https://docs.corda.net/head/node-database-access-h2.html?highlight=database#connecting-via-a-socket-on-a-running-node
)

However this is a bit clumsy as deployNodes will overwrite the config each time you deployNode. To get deployNodes to generate the correct node.config, [follow these instructions](https://docs.corda.net/head/generating-a-node.html?highlight=create%20nodes%20locally#the-cordform-task
)

The extra piece of configuration you need is: 

            extraConfig = [
                // Setting the H2 address.
                h2Settings: [ address: 'localhost:10030' ]
            ]

  
# Extending the template

You should extend this template as follows:

* Add your own state and contract definitions under `contracts/src/main/kotlin/`
* Add your own flow definitions under `workflows/src/main/kotlin/`
* Extend or replace the client and webserver under `clients/src/main/kotlin/`

For a guided example of how to extend this template, see the Hello, World! tutorial 
[here](https://docs.corda.net/hello-world-introduction.html).
 


