#!/bin/bash

# Ensure user is authenicated, and run login if not.
gcloud auth print-identity-token &> /dev/null
if [ $? -gt 0 ]; then
    gcloud auth login
fi
kubectl config use-context dev-gcp
kubectl config set-context --current --namespace=okonomi

# Get AZURE system variables
envValue=$(kubectl exec -it $(kubectl get pods | grep sokos-pdl-proxy | cut -f1 -d' ') -c sokos-pdl-proxy -- env | egrep "^AZURE|^PDL")

# Set AZURE as local environment variables
rm -f defaults.properties
echo "$envValue" > defaults.properties
echo "AZURE stores as defaults.properties"