#!/bin/bash
# Use exec to replace the shell with the java process
exec java -cp target/consoleApp-1.0-SNAPSHOT-jar-with-dependencies.jar discord.Launcher -t "${TOKEN}"