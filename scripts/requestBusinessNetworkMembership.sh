#!/usr/bin/env bash

echo "Request membership for all nodes in Business Network"

curl -X POST \
  http://localhost:50005/api/bnm/requestMembership \
  -H 'Content-Type: application/json' \
  -H 'alternativeName: PartyA' \
  -H 'cache-control: no-cache' \
  -H 'role: Party Alpha' \

curl -X POST \
  http://localhost:50006/api/bnm/requestMembership \
  -H 'alternativeName: PartyB' \
  -H 'cache-control: no-cache' \
  -H 'role: Party Bravo'

curl -X POST \
  http://localhost:50007/api/bnm/requestMembership \
  -H 'alternativeName: PartyC' \
  -H 'cache-control: no-cache' \
  -H 'role: Party Charlie'






