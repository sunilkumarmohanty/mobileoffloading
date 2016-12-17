#!/usr/bin/env bash
sudo apt-get install curl
curl --insecure -H "Content-Type: application/json" -X POST -d '{"username":"username","password":"password"}' https://104.154.187.219/api/register