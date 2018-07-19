#!/usr/bin/env bash
#Simple script to create and label a config map from the CLI

oc create cm processor.cm --from-env-file=processor.properties
oc label cm processor.cm streamzi.io/target=processor
oc label cm processor.cm streamzi.io/kind=ev
