#!/usr/bin/env bash
#Simple script to create and label a config map from the CLI

oc create cm consumer.cm --from-env-file=consumer.properties
oc label cm consumer.cm streamzi.io/targetKey=app
oc label cm consumer.cm streamzi.io/targetValue=Consumer
oc label cm consumer.cm streamzi.io/kind=ev
