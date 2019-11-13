#!/bin/sh

# CHANGE THIS LINE TO POINT TO THE REFERENCE COMPILER
REFERENCE_COMPILER="./decaf-1.0.jar"
OUR_COMPILER="./decaf.sh"
# CHANGE THIS FOR EACH PA
REF_FLAG="--fdump-iloc"

# Abort if the reference compiler path is invalid
if [ ! -f "$REFERENCE_COMPILER" ]; then
    >&2 echo "!!! Unable to find the decaf reference compiler."
    >&2 echo "    Please set this variable in the script."
    >&2 echo "    Given: $REFERENCE_COMPILER"
    exit 1
fi

# Abort if no decaf file is passed in
if [ -z "$1" ]; then
    >&2 echo "!!! No decaf program given"
    exit 2
fi

echo "Rebuilding the project"
# Give the user time to see what's happening
sleep 2
mvn compile

echo "Comparing both compilers"
# Give the user time to see what's happening
sleep 2

# Save the output from our compiler
./decaf.sh $1 > ours.txt
java -jar "$REFERENCE_COMPILER" ${REF_FLAG} $1 > ref.txt

diff -Naur ours.txt ref.txt