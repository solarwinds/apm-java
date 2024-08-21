/*
 * © SolarWinds Worldwide, LLC. All rights reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.solarwinds.netty;

import io.netty.util.NetUtil;
import com.solarwinds.api.ext.SolarwindsAgent;

import java.util.concurrent.TimeUnit;

public class NettyApp {
    public static void main(String[] args){
        SolarwindsAgent.setTransactionName("hello world!");
        System.out.printf("Number of interfaces: %d%n",NetUtil.NETWORK_INTERFACES.size());
        try {
            TimeUnit.MINUTES.sleep(1);
        } catch (InterruptedException ignore) {

        }
        System.out.printf("Shutting down%n");
    }
}
