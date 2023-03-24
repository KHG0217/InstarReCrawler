package com.tapacross.sns.crawler.instagram.keyword.storm

import backtype.storm.topology.TopologyBuilder
import com.tapacross.sns.crawler.instagram.keyword.ApplicationProperty
import org.springframework.context.support.GenericXmlApplicationContext

import java.applet.AppletContext

class InstagramKeywordReCrawlerTopology {
    private static final String INSTAGRAM_KEYWORD_RECRAWLER = "InstagramKeywordReCrawler"
    private static final String TOPOLOGY_NAME = "InstagramKeywordCrawlerTopology"
    private static final String KEYWORD_RECEIVE_SPOUT_ID = "keyword-receive-spout"
    private static final String ARTICLE_PARSE_BOLT_ID = "article-parse-bolt"
    private static final String ARTICLE_EXTRACT_BOLT_ID = "article-extract-bolt"

    private int runMode = 0
    private int numWorker = 1
    private int numBolt = 1
    private int keywordSetNum = 0
    private String proxySite = ""

    private ApplicationProperty applicationProperty

    def void init(List<String> args) {
        initArgs(args)
        initSpringContext()
    }

    private void initSpringContext() {
        AppletContext context = new GenericXmlApplicationContext(
                "classpath:spring/application-context.xml")
        this.applicationProperty = context.getBean(ApplicationProperty.class)
    }

    /**
     * run mode 실행 형태, 0:로컬 실행, 1:서버 실행
     * keywordset number
     * number of worker
     * number of parse bolt
     * proxy site
     *
     * @param args
     */
    private void initArgs(List<String> args) {
        if (args.size() < 5) {
            LoggerWrapper.error(INSTAGRAM_KEYWORD_CRAWLER,
                    "Usage:<run mode> <number of worker> <number of bolt> <proxy site>")
            LoggerWrapper.error(INSTAGRAM_KEYWORD_CRAWLER, "System exit.")
            System.exit(-1)
        }
        this.runMode = Integer.parseInt(args.get(0))
        this.numWorker = Integer.parseInt(args.get(2))
        this.numBolt = Integer.parseInt(args.get(3))
        this.proxySite = args.get(4)
    }

    def void start(){
        def builder = new TopologyBuilder()
        builder.setSpout()
    }
}
