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

BRANCH=$(echo -n "${CIRCLE_BRANCH}" | sed -e 's/[^0-9a-zA-Z\._\-]/./g' | tr '[:upper:]' '[:lower:]')
AGENTVERSION=$(unzip -p agent/build/libs/solarwinds-apm-agent.jar META-INF/MANIFEST.MF)
AGENTVERSION=$(echo -n "${AGENTVERSION}" | grep Implementation-Version | awk '{ print $2 }' | sed 's/[^a-z0-9.-]//g')
export AGENTVERSION=$AGENTVERSION
