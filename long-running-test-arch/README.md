## Benchmark Architecture

This architecture is built on kubernetes. It has 3 application deployments that deploys 3 pods of the same application with 
services that make the pods accessible in the cluster. It has a cronjob that runs every 15 minutes and uses k6 to
generate load to the 3 applications.


## Steps to deploy
You can use the `boot.sh` script to complete steps 2-5.

0. **[install](https://eksctl.io/installation/) eksctl**

1. update the `iamIdentityMappings` and `vpc` sections of `eksctl-config.yml`. if you don't have an existing vpc, just specify the cidr. You may get an error if your region has run out of public ips(max 5 per region in aws). If that happens, use an existing vpc and replace values in the vpc section. Examples [here](https://github.com/eksctl-io/eksctl/tree/main/examples)

2. **authenticate to aws with temp credentials for authorized user/role**
```shell
export AWS_ACCESS_KEY_ID="<key-id>"
export AWS_SECRET_ACCESS_KEY="<key>"
export AWS_SESSION_TOKEN="<token>"
```
3. **create cluster**
```shell
eksctl create cluster -f eksctl-config.yml
```
4. **create namespace**
```shell
 kubctl apply k8s/namespace.yml
```
5. **setup kubectl context**
```shell
kubectl config set-context bench --current --user=<username> --cluster=<cluster name> --namespace=swo-java-agent-benchmark
```
6. **create config map**
```shell
kubectl create configmap k6 --from-file=k6 
kubectl create configmap db --from-file=k8s/db
```
7. **create secret. use swo dev tokens to avoid having to modify deployment file. put the commands fragments on the same line if running with line breaks isn't working.**
```shell
kubectl create secret generic swo-tokens \
--from-literal=service-key='<token:service>' \
--from-literal=otel-header='authorization=Bearer <token>'
kubectl create secret docker-registry docker-cred --docker-server=https://ghcr.io --docker-username=<your-username> --docker-password=<your-pword> --docker-email=<your-email>    
```
8. **deploy the architecture** 
```shell
  kubectl apply -f k8s/
```
