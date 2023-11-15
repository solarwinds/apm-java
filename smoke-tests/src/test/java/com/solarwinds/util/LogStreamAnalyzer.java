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
