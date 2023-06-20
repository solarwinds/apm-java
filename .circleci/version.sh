BRANCH=$(echo -n "${CIRCLE_BRANCH}" | sed -e 's/[^0-9a-zA-Z\._\-]/./g' | tr '[:upper:]' '[:lower:]')
AGENTVERSION=$(unzip -p agent/build/libs/solarwinds-apm-agent.jar META-INF/MANIFEST.MF)
AGENTVERSION=$(echo -n "${AGENTVERSION}" | grep Implementation-Version | awk '{ print $2 }' | sed 's/[^a-z0-9.-]//g')
export AGENTVERSION=$AGENTVERSION