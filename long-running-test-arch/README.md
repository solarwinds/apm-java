## Benchmark Architecture

This architecture is built on kubernetes. It has 3 application deployments that deploys 3 pods of the same application with 
services that make the pods accessible in the cluster. It has a cronjob that runs every 15 minutes and uses k6 to
generate load to the 3 applications.


## Steps to deploy
You can use the `boot.sh` script to complete steps 1-3.
1. **create namespace**
```shell
 kubctl apply k8s/namespace.yml
```
2. **setup kubectl context**
```shell
kubectl config set-context bench --current --user=<username> --cluster=<cluster name> --namespace=swo-java-agent-benchmark
```
3. **create config map**
```shell
kubectl create configmap k6 --from-file=k6 
kubectl create configmap db --from-file=k8s/db
```
4. **create secret. use swo dev tokens to avoid having to modify deployment file.**
```shell
kubectl create secret generic swo-tokens \                                                
    --from-literal=service-key='<token:service>' \
    --from-literal=otel-header='authorization=Bearer <token>'
kubectl create secret docker-registry docker-cred --docker-server=https://ghcr.io --docker-username=<your-username> --docker-password=<your-pword> --docker-email=<your-email>    
```
5. **deploy the architecture** 
```sh 
  kubectl apply -f k8s/
```
