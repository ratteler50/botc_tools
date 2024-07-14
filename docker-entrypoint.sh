#!/bin/bash
# Read TOKEN from token.txt file
TOKEN=$(cat token.txt)
# Use exec to replace the shell with the java process
exec java -jar target/botcTools-1.0-SNAPSHOT.jar -t "${TOKEN}"