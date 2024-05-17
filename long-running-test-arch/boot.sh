#
# © SolarWinds Worldwide, LLC. All rights reserved.
#
# Licensed under the Apache License, Version 2.0 (the "License");
# you may not use this file except in compliance with the License.
# You may obtain a copy of the License at
#
# http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.
#

cluster=
user=
OPTSTRING=":c:u:"

while getopts ${OPTSTRING} opt; do
  case ${opt} in
    c)
      echo "Cluster set to: ${OPTARG}"
      cluster="${OPTARG}"
      ;;
    u)
      echo "User set to: ${OPTARG}"
      user="${OPTARG}"
      ;;
    :)
      echo "Option -${OPTARG} requires an argument."
      exit 1
      ;;
    ?)
      echo "Invalid option: -${OPTARG}."
      echo "Usage: boot.sh -c <cluster> [-u <user>]"
      exit 1
      ;;
  esac
done

if [ -z "$cluster" ]; then
  echo "Usage: boot.sh -c <cluster>"
  exit 1
fi

if [ -z "$user" ]; then
  user=$cluster
fi

eksctl create cluster -f eksctl-config.yml
kubectl config set-context k8s-bench --user="$user" --cluster="$cluster" --namespace=swo-java-agent-benchmark

kubectl config use-context k8s-bench
kubectl apply -f k8s/namespace.yml

kubectl create configmap k6 --from-file=k6
kubectl create configmap db --from-file=k8s/db
echo "Continue from step 6 in the README.md to finish"
