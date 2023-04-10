package com.tapacross.sns.crawler.instagram.keyword.storm

import backtype.storm.Config
import backtype.storm.LocalCluster
import backtype.storm.StormSubmitter
import backtype.storm.generated.AlreadyAliveException
import backtype.storm.generated.InvalidTopologyException
import backtype.storm.topology.TopologyBuilder
import com.tapacross.sns.crawler.instagram.keyword.ApplicationProperty
import com.tapacross.sns.crawler.instagram.keyword.storm.bolt.InstagramkeywordRecrawlerExtractBolt
import com.tapacross.sns.crawler.instagram.keyword.storm.bolt.InstagramkeywordRecrawlerParseBolt
import com.tapacross.sns.crawler.instagram.keyword.storm.spout.InstagramkeywordRecrawlerSpout
import com.tapacross.sns.util.DateFormatUtil
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.context.ApplicationContext
import org.springframework.context.support.GenericXmlApplicationContext


/**
 * @author hgkim
 * 인스타그램 특정 키워드를 과거 기간동안 수집하는 스톰 토폴로지
 * 파싱시 Proxy IP를 사용하고, 로컬에서 구동한다.
 *
 */
class InstagramKeywordReCrawlerTopology {
    private static final String INSTAGRAM_KEYWORD_RECRAWLER = "InstagramKeywordReCrawler"
    private static final String TOPOLOGY_NAME = "InstagramKeywordReCrawlerTopology"
    private static final String KEYWORD_RECEIVE_SPOUT_ID = "keyword-receive-spout"
    private static final String ARTICLE_PARSE_BOLT_ID = "article-parse-bolt"
    private static final String ARTICLE_EXTRACT_BOLT_ID = "article-extract-bolt"

    private int runMode = 0
    private int numWorker = 1
    private int numBolt = 1
    private String proxySite = ""
    private final Logger logger = LoggerFactory.getLogger(getClass())

    private ApplicationProperty applicationProperty

    def void init(List<String> args) {
        initArgs(args)
        initSpringContext()
    }

    private void initSpringContext() {
        ApplicationContext context = new GenericXmlApplicationContext(
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
            logger.error(INSTAGRAM_KEYWORD_RECRAWLER +
                    " Usage:<run mode> <number of worker> <number of bolt> <proxy site>")
            logger.error(INSTAGRAM_KEYWORD_RECRAWLER + " System exit.")
            System.exit(-1)
        }
        this.runMode = Integer.parseInt(args.get(0))
        this.numWorker = Integer.parseInt(args.get(1))
        this.numBolt = Integer.parseInt(args.get(2))
        this.proxySite = args.get(3)
    }

    def void start(){
        def builder = new TopologyBuilder()
        builder.setSpout(KEYWORD_RECEIVE_SPOUT_ID, new InstagramkeywordRecrawlerSpout(), numWorker)
        builder.setBolt(ARTICLE_PARSE_BOLT_ID, new InstagramkeywordRecrawlerParseBolt(), numBolt)
                .localOrShuffleGrouping(KEYWORD_RECEIVE_SPOUT_ID)
        builder.setBolt(ARTICLE_EXTRACT_BOLT_ID, new InstagramkeywordRecrawlerExtractBolt(), 1)
                .localOrShuffleGrouping(ARTICLE_PARSE_BOLT_ID)
        def config = new Config()

        config.setNumWorkers(numWorker)
        config.put("proxy.site", proxySite)

        // 스파우트와 볼트 사이의 처리속도를 맞추기 위해 송수신 큐 사이즈를 최소화
        config.put(Config.TOPOLOGY_EXECUTOR_RECEIVE_BUFFER_SIZE, new Integer(1));
        config.put(Config.TOPOLOGY_EXECUTOR_SEND_BUFFER_SIZE, new Integer(1));
        config.put(Config.TOPOLOGY_RECEIVER_BUFFER_SIZE, new Integer(1));
        config.put(Config.TOPOLOGY_TRANSFER_BUFFER_SIZE, new Integer(1));

        // pending되는 볼트 튜플이 개수를 초과할 경우 스파우트의 nextTuple 메서드가 호출되지 않는다.
        config.setMaxSpoutPending(numBolt)

        // bolt가 슬립을 제외한 동작에 시간이 소요되더라도 자동 fail이 되지 않도록 타임아웃 증가
        config.setMessageTimeoutSecs(1200 * 60)

        try {
            // 로컬 실행할 때
            if (runMode == 0) {
                def cluster = new LocalCluster()
                cluster.submitTopology(TOPOLOGY_NAME, config, builder.createTopology())
                while (true) {

                }
                cluster.killTopology(TOPOLOGY_NAME)
                cluster.shutdown()
            } else { // 서버에서 실행할 때
                // 인스타그램은 로그인 특성으로 토플로지를 개별 관리해야 하므로 토플로지명을 유니크하게 생성한다.
                def datetime = DateFormatUtil.format(new Date(), "yyyyMMddHHmmss")
                StormSubmitter.submitTopology(TOPOLOGY_NAME + "-" + datetime, config, builder.createTopology())
            }
        } catch (AlreadyAliveException e) {
            logger.error(INSTAGRAM_KEYWORD_RECRAWLER + e)
        } catch (InvalidTopologyException e) {
            logger.error(INSTAGRAM_KEYWORD_RECRAWLER + e)
        }

    }

    static main(args) {
        def topology = new InstagramKeywordReCrawlerTopology()
        topology.init(args.collect() as List<String>)
        topology.start()
    }
}
