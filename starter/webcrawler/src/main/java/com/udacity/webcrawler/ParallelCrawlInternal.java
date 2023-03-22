package com.udacity.webcrawler;

import com.udacity.webcrawler.parser.PageParser;
import com.udacity.webcrawler.parser.PageParserFactory;

import java.time.Clock;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.RecursiveAction;
import java.util.regex.Pattern;

public class ParallelCrawlInternal extends RecursiveAction {
    String url;
    Instant deadline;
    int maxDepth;
    Map<String, Integer> counts;
    Set<String> visitedUrls;
   Clock clock;
    PageParserFactory parserFactory;
    List<Pattern> ignoredUrls;


    public ParallelCrawlInternal(String url, Instant deadline, int maxDepth,
                                 Map<String, Integer> counts, Set<String> visitedUrls,
                                 Clock clock, PageParserFactory parserFactory, List<Pattern> ignoredUrls) {
        this.url = url;
        this.deadline = deadline;
        this.maxDepth = maxDepth;
        this.counts = counts;
        this.visitedUrls = visitedUrls;
        this.clock = clock;
        this.parserFactory = parserFactory;
        this.ignoredUrls = ignoredUrls;
    }

    @Override
    protected void compute() {
        if (maxDepth == 0 || clock.instant().isAfter(deadline)) {
            return;
        }
        for (Pattern pattern : ignoredUrls) {
            if (pattern.matcher(url).matches()) {
                return;
            }
        }
       // If already visited..
        if (visitedUrls.contains(url)) {
            return;
        }
        visitedUrls.add(url);

        PageParser.Result result = parserFactory.get(url).parse();
       synchronized (counts) {
           for (Map.Entry<String, Integer> e : result.getWordCounts().entrySet()) {
               if (counts.containsKey(e.getKey())) {
                   counts.put(e.getKey(), e.getValue() + counts.get(e.getKey()));
               } else {
                   counts.put(e.getKey(), e.getValue());
               }
           }
       }
       // create a list of subtasks
        List<ParallelCrawlInternal> subtasks = new ArrayList<>();
        for (String link : result.getLinks()) {
            subtasks.add(new ParallelCrawlInternal(link, deadline, maxDepth-1, counts,
                    visitedUrls,clock,parserFactory,ignoredUrls));
        }
        invokeAll(subtasks);
    }
}

