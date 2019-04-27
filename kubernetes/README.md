### Deploy a Kubernetes network of Corda nodes

You can deploy a collection of Corda nodes within docker containers on a kubernetes cluster on your machine. The example runs the [Yo Cordapp](https://github.com/corda/samples/tree/release-V4/yo-cordapp) but any CorDapp can be used. 

The essential commands are:

**Remove any existing yo-app stacks.**
```
docker stack rm yo-app --orchestrator=kubernetes
```

**Compiles the Docker images from the sub folders**

For windows:
```
docker build .\party-a\. -t party-a
docker build .\party-b\. -t party-b
docker build .\party-c\. -t party-c
```

For mac:
```
docker build ./party-a/. -t party-a
docker build ./party-b/. -t party-b
docker build ./party-c/. -t party-c
```

**Deploy the stack**

for windows:
```
docker stack deploy yo-app --compose-file .\docker-compose.yml --orchestrator=kubernetes
```
for mac:
```
docker stack deploy yo-app --compose-file ./docker-compose.yml --orchestrator=kubernetes
```

After it has been deployed, use this command to check that it is up and running:
```
docker stack ps yo-app --orchestrator=kubernetes
```

From the above command you can also get the containers id and feed it into this command to view the output:
```
docker service logs -f <CONTAINER-ID>
```

The nodes also have SSH access to the Crash shell, which allows you to execute any flows directly on the nodes.
It may take a minute or so for the network to start up, once it does the Corda nodes be accessed via ssh with username: **user1** and password: **test**, with the following command:

```
ssh -o StrictHostKeyChecking=no user1@localhost -o UserKnownHostsFile=/dev/null -p 2221
```
Please note that the depending on which port number you select, you will connect to *party-a(2221)*, *party-b(2222)* or *party-c(2223)*.


Once in the Node Shell, you can initiate a YO Flow by running the following command:
```
flow start YoFlow target: [NODE_NAME], for example *PartyB*
```
Please note that the names of the parties are *PartyA*, *PartyB* and *PartyC*, these are the Nodes X500 names and should not be confused with the directory names which are all lower case.

At this point you may consider logging in to another Node and sending a Yo to PartyA as well.

In order to inspect if you have received a Yo from another Node, you can execute the following command:
```
run vaultQuery contractStateType: net.corda.yo.YoState