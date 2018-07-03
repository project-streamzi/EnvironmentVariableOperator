# Environment Variable Operator

Operator that keeps Environment Variables synchronised with ConfigMaps. 
Watches for updates to ConfigMaps and when one is created, modified or deleted this operator will create Environment Variables in the relevant DeploymentConfig.
Also watches for updates to DeploymentConfigs and will add any Environment Variables found in associated ConfigMaps.

It is not safe to have multiple ConfigMaps that target the same Deployment and Environment Variable names.
Doing so will lead to race conditions and unpredictability.

If ConfigMaps are modified the Operator is not able to remove Environment Variables from the Deployments.
However, if ConfigMaps are deleted the Environment Variables they contain will be removed from the Deployments.

## Deployment

The Operator can run inside or outside OpenShift. To start it run either

```bash
$ mvn clean package
$ java -jar target/EnvironmentVariableOperator.jar
``` 

Or for an OpenShift deployment

```bash
$ mvn clean pacakge fabric8:deploy -Popenshift
```

Depending on your permissions you may need to run the followinf command to allow your OpenShift user to access the ConfigMaps.

```bash
$ oc adm policy add-cluster-role-to-user cluster-admin system:serviceaccount:myproject:default
```

TODO: Investigate permissions more

## Usage

A full example is in the `Examples` directory where vanilla Kafka producer and consumer are configured using Environment Variables.
They will not connect to a Topic if the Environment Variables are not present.
To activate the Operator you need to create a ConfigMap with two labels:

```yaml
streamzi.io/kind=ev
streamzi.io/target=Consumer
```

The first label indicates that this ConfigMap cotains a data payload of Environment Variables.
The second label identifies the label of the application (Deployment) that will receve the enviroment variables.
The full ConfigMap should look similar to the following snippet.

```yaml
apiVersion: v1
data:
  bootstrap.servers: 'my-cluster-kafka:9092'
  topic: topic-71
kind: ConfigMap
metadata:
  creationTimestamp: '2018-07-03T10:03:53Z'
  labels:
    streamzi.io/kind: ev
    streamzi.io/target: Consumer
  name: consumer.cm
  namespace: myproject
  resourceVersion: '285304'
  selfLink: /api/v1/namespaces/myproject/configmaps/consumer.cm
  uid: 64279a1a-7ea8-11e8-bd15-7a9aad351283
```

 