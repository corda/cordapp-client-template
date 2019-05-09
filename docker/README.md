<p align="center">
  <img src="https://www.corda.net/wp-content/uploads/2016/11/fg005_corda_b.png" alt="Corda" width="500">
</p>

### Run a local Docker network

You can interact with the Corda nodes on your own mini network of docker containers. You can bootstrap this network via the `docker.sh` script within docker module. This script will create containers according to how many names you specify in the participant.txt file. 
The script starts by spinning up a docker network. Each container that is generated is added to the docker network `mininet`. Furthermore each Corda node in those containers joins the local Corda network by requesting access through the `netmap` container which contains:
 1. An identity operator (previously doorman service) 
 2. A Network Map Service
 3. A Notary
 
Once the script has been successfully ran you can inspect the docker processes. via the command below which should display a list of 4 running containers; one for each of the 3 partys and one for the notary and network map service.

    docker ps

Alternatively you can display all docker containers whether they are running or not via the command 

    docker ps -a
    
Once you can see the running containers. You can `ssh` in to one to interact with the corda node via the command

    ssh rpcUser@localhost -p <ssh-port> #2221 is the first port used. The password is testingPassword
    
The template uses the Corda finance Cordapps but you can use any of your own. Just place them in the Cordapps folders by editing the script or do it after and relaunch the container. We can test this node is successfully running by running

    run vaultQuery contractStateType: net.corda.finance.contracts.asset.Cash$State
    start net.corda.finance.flows.CashIssueFlow amount: $111111, issuerBankPartyRef: 0x01, notary: Notary
    start net.corda.finance.flows.CashPaymentFlow amount: $500, recipient: "Party2"
    start net.corda.finance.flows.CashPaymentFlow amount: $500, recipient: "Party3"
    
Try other nodes too

    ssh rpcUser@localhost -p 2222
    start net.corda.finance.flows.CashPaymentFlow amount: $200, recipient: "Party1"
    start net.corda.finance.flows.CashPaymentFlow amount: $100, recipient: "Party3"