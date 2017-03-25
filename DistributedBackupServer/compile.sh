#!/bin/bash

mkdir -p bin
javac -d bin $(find .  -name \*.java)

echo Application compiled!\\n