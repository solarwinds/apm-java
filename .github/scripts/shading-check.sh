code=0
for path in $(jar -tf agent/build/libs/solarwinds-apm-agent.jar | grep -E -v '^((com/solarwinds|inst|io/open|META))')
do
  PACKAGE=$(echo "$path" | awk -F/ '{print $2}')
  if [ -n "$PACKAGE" ] && [ "$PACKAGE" != "annotation" ]; then
    echo "Package ($path) is not shaded"
    code=1
  fi
done

if [[ code -ne 0 ]]; then
    exit $code
fi

lambda=0
for path in $(jar -tf agent-lambda/build/libs/solarwinds-apm-agent-lambda.jar | grep -E -v '^((com/solarwinds|inst|io/open|META))')
do
  PACKAGE=$(echo "$path" | awk -F/ '{print $2}')
  if [ -n "$PACKAGE" ] && [ "$PACKAGE" != "annotation" ]; then
    echo "Package ($path) is not shaded"
    lambda=1
  fi
done
exit $lambda