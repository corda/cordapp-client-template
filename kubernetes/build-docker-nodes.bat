@echo off
cls
set orchestrator=
set orchestrator=--orchestrator=kubernetes
set stack_name=yo-app
echo Removing pre-existing stack...
docker stack rm %stack_name% %orchestrator% 
echo Building docker images...
docker build .\party-a\. -t party-a
docker build .\party-b\. -t party-b
docker build .\party-c\. -t party-c
echo Deploying stack...
docker stack deploy %stack_name% %orchestrator% --compose-file .\docker-compose.yml
echo Docker ps:
docker stack ps %stack_name% %orchestrator%

echo docker service logs -f CONTAINER

echo party-a SSH shell access
echo ssh -o StrictHostKeyChecking=no user1@localhost -o UserKnownHostsFile=/dev/null -p 2221
echo party-b SSH shell access
echo ssh -o StrictHostKeyChecking=no user1@localhost -o UserKnownHostsFile=/dev/null -p 2222
echo party-c SSH shell access
echo ssh -o StrictHostKeyChecking=no user1@localhost -o UserKnownHostsFile=/dev/null -p 2223
