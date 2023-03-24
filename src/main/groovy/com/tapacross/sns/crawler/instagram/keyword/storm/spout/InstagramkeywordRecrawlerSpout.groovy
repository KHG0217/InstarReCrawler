package com.tapacross.sns.crawler.instagram.keyword.storm.spout

import backtype.storm.spout.SpoutOutputCollector
import backtype.storm.task.TopologyContext
import backtype.storm.topology.OutputFieldsDeclarer
import backtype.storm.topology.base.BaseRichSpout
import backtype.storm.tuple.Fields
import backtype.storm.tuple.Values
import com.google.gson.GsonBuilder
import com.tapacross.sns.crawler.instagram.keyword.storm.ConstantOutputField
import com.tapacross.sns.entity.KeyValueItem
import com.tapacross.sns.entity.TBProxy
import com.tapacross.sns.queue.QueueManager
import com.tapacross.sns.service.IRedisService
import com.tapacross.sns.service.RedisService
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.context.support.GenericXmlApplicationContext

class InstagramkeywordRecrawlerSpout extends BaseRichSpout {
	private static final String INSTAGRAM_KEYWORD_RE_CRAWLER = "InstagramKeywordReCrawler"
	
	private final String INSTAGRAM_PROXY_IP_QUEUE_PREFIX = "proxy:instagram"
	
	private final Logger logger = LoggerFactory.getLogger(getClass())
	private SpoutOutputCollector collector
	private IRedisService redisService
	private QueueManager proxyQueueManager
	
	private String proxySite
	
	private Map<String, KeyValueItem> AccountMap
	private long searchCount = 0
	private long successCount = 0
	private long failCount = 0


	public void open(Map conf, TopologyContext context, SpoutOutputCollector collector) {
		this.collector = collector
		this.logger.info(INSTAGRAM_KEYWORD_RE_CRAWLER,"open() Start!")

		this.AccountMap = new HashMap<>();
		this.proxySite = conf.get("proxy.site")

		def applicationContext = new GenericXmlApplicationContext("classpath:spring/application-context.xml")
		this.redisService = applicationContext.getBean(RedisService.class)

		// service 작성
//		this.service = applicationContext.getBean()

		logger.info(INSTAGRAM_KEYWORD_RE_CRAWLER,"open() End !")
	}
	@Override
	public void nextTuple() {
		def proxyJson = redisService.popBlockingQueue(INSTAGRAM_PROXY_IP_QUEUE_PREFIX + ":" +proxySite)
		def proxy = new GsonBuilder().create().fromJson(proxyJson, TBProxy.class)
		def keywordList = readKeywordList()
		keywordList.forEach { it ->
			emitItem(it, proxyJson)
		}
	}
	@Override
	public void declareOutputFields(OutputFieldsDeclarer declarer) {
		declarer.declare(new Fields(ConstantOutputField.KEYWORD_FIELD, ConstantOutputField.PROXY_FIELD))
		
	}

	@Override
	void ack(Object msgId) {
		++successCount
		def item = AccountMap.remove(MsgId as String) // value1이 삭제되어서 ?
		logger.info(INSTAGRAM_KEYWORD_RE_CRAWLER, "bolt acked. value=${item.getValue()}, successCount=$successCount")

		def proxy = new GsonBuilder().create().fromJson(item.value2) // 2개 넣는 이유?

		def keyword = null
		while (keyword == null){
			// ?
		}
		//emitItem()
	}

	@Override
	void fail(Object msgId) {
		++failCount
		successCount = 0
		def item = AccountMap.remove(msgId as String)
		logger.info(INSTAGRAM_KEYWORD_CRAWLER, "bolt failed. emit spout item. value=$item.value, value2=$item.value2, failCount=$failCount")

		def proxy = new GsonBuilder().create().fromJson(item.value2, TBProxy.class)
	}

	private List<String> readKeywordList(){
		List<String> keywordList = new ArrayList<>();
		try {
			BufferedReader reader = new BufferedReader(
					new FileReader("../instagram-keyword-recrawler/src/main/resources/data/keywordList_1.txt"), // Read file
					16 * 1024
			);
			String str;
			while((str = reader.readLine()) != null) {
				keywordList.add(str)
			}
			return keywordList
		}catch (Exception e) {
			e.printStackTrace()
		}
		return keywordList
	}

	private void emitItem(String keyword, String proxyJson) {
		def emitKey = UUID.randomUUID().toString()
		AccountMap.put(emitKey, new KeyValueItem(emitKey, keyword, proxyJson))
		++searchCount
		// 여기서부터 다시시작

		collector.emit(new Values(keyword, proxyJson), emitKey)
	}
	
	
}
