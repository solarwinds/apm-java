cluster=
OPTSTRING=":c:"

while getopts ${OPTSTRING} opt; do
  case ${opt} in
    c)
      echo "Cluster set to: ${OPTARG}"
      cluster="${OPTARG}"
      ;;
    :)
      echo "Option -${OPTARG} requires an argument."
      exit 1
      ;;
    ?)
      echo "Invalid option: -${OPTARG}."
      echo "Usage: boot.sh -c <cluster>"
      exit 1
      ;;
  esac
done

if [ -z "$cluster" ]; then
  echo "Usage: boot.sh -c <cluster>"
  exit 1
fi

kubectl config set-context k8s-bench --user="$cluster" --cluster="$cluster" --namespace=swo-java-agent-benchmark
kubectl config use-context k8s-bench
kubectl apply -f k8s/namespace.yml

kubectl create configmap k6 --from-file=k6
kubectl create configmap db --from-file=k8s/db
echo "Continue from step 4 in the README.md to finish"
