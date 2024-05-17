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

package com.solarwinds.util;

import lombok.Getter;
import org.testcontainers.containers.output.BaseConsumer;
import org.testcontainers.containers.output.OutputFrame;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;


public class LogStreamAnalyzer<DELEGATE extends BaseConsumer<DELEGATE>> extends BaseConsumer<LogStreamAnalyzer<DELEGATE>> {
    private final Map<String, Pattern> queries = new HashMap<>();

    @Getter
    private final Map<String, Boolean> answer = new HashMap<>();

    private final BaseConsumer<DELEGATE> delegate;

    public LogStreamAnalyzer(List<String> queries, BaseConsumer<DELEGATE> delegateBaseConsumer) {
        queries.forEach(query -> this.queries.put(query, Pattern.compile(query)));
        this.delegate = delegateBaseConsumer;
    }

    @Override
    public void accept(OutputFrame outputFrame) {
        String utf8StringWithoutLineEnding = outputFrame.getUtf8StringWithoutLineEnding();
        queries.forEach((query, pattern) ->
                answer.compute(query, (key, ans) -> Boolean.TRUE.equals(ans) || pattern.matcher(utf8StringWithoutLineEnding).find())
        );
        delegate.accept(outputFrame);
    }
}
