#!/bin/bash

if [ "$#" -lt 1 ]; then
	echo 'Wrong number of arguments. Usage:'
	echo 'sh test.sh <path-to-compiled-module>'
	exit 1;
fi

os=$(uname)

if [ "$os" = "Linux" ]; then ##Figure out how to know terminal name
	#terminal=$(ps -o 'cmd=' -p $(ps -o 'ppid=' -p $$))
	#terminal=$(echo $terminal -e sh -c)
	terminal=$(echo urxvt -e bash -c)
elif [ "$os" = "Darwin" ]; then
	terminal=$(echo open -a Terminal)
else
	exit 1
fi

modulePath=$(realpath $1)
originalPath=$(realpath .)

echo $originalPath

echo rmiregistry -J-Djava.rmi.server.codebase=file://$modulePath

cd $1

#rmiregistry
echo "Launching rmiregistry...."
eval $terminal "\"rmiregistry -J-Djava.rmi.server.codebase=file://bin\" &"

sleep 1 #To be sure that the rmiregistry is running

#Servers 
echo "Launching server 1..."
eval $terminal "\"java peer.Peer 1.0 1 1 224.0.0.1 4445 224.0.0.2 4446 224.0.0.3 4447; read\" &"

wait

cd $originalPath
