#!/usr/bin/env bash
#Simple script to create and label a config map from the CLI

oc create cm producer.cm --from-env-file=producer.properties
oc label cm producer.cm streamzi.io/target=Producer
oc label cm producer.cm streamzi.io/kind=ev
