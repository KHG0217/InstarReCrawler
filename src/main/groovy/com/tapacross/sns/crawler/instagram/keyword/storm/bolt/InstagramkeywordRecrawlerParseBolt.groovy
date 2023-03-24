package com.tapacross.sns.crawler.instagram.keyword.storm.bolt

import backtype.storm.task.OutputCollector
import backtype.storm.task.TopologyContext
import backtype.storm.topology.OutputFieldsDeclarer
import backtype.storm.topology.base.BaseRichBolt
import backtype.storm.tuple.Tuple
import com.google.gson.GsonBuilder
import com.tapacross.sns.crawler.instagram.keyword.FocusedInstagramKeywordCrawler
import com.tapacross.sns.crawler.instagram.keyword.storm.ConstantOutputField
import com.tapacross.sns.entity.TBProxy
import org.slf4j.LoggerFactory

import java.util.logging.Logger

class InstagramkeywordRecrawlerParseBolt extends BaseRichBolt {

    private static final String INSTAGRAM_KEYWORD_CRAWLER = "InstagramKeywordCrawler"
    private final int CRAWL_DELAY_SECS = 2
    private final int EXCEPTION_DELAY_SECS = 5
    private final Logger logger = LoggerFactory.getLogger(getClass())
    private OutputCollector collector
    private TopologyContext context

    @Override
    void prepare(Map stormConf, TopologyContext context, OutputCollector collector) {
        this.collector = collector
        this.context = context

    }

    @Override
    void execute(Tuple input) {
        def hashtag = input.getStringByField(ConstantOutputField.KEYWORD_FIELD).replaceAll(" ", "")
        def proxyJson = input.getStringByField(ConstantOutputField.PROXY_FIELD)
        def proxy = new GsonBuilder().create().fromJson(proxyJson, TBProxy.class)

        def parser = new FocusedInstagramKeywordCrawler()
    }

    @Override
    void declareOutputFields(OutputFieldsDeclarer declarer) {

    }
}
