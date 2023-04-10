package com.tapacross.sns.crawler.instagram.keyword.storm.spout

import backtype.storm.spout.SpoutOutputCollector
import backtype.storm.task.TopologyContext
import backtype.storm.topology.OutputFieldsDeclarer
import backtype.storm.topology.base.BaseRichSpout
import backtype.storm.tuple.Fields
import backtype.storm.tuple.Values
import com.tapacross.sns.crawler.instagram.keyword.storm.ConstantOutputField
import com.tapacross.sns.crawler.instagram.keyword.instagramKeywordDataVO
import com.tapacross.sns.entity.KeyValueItem
import com.tapacross.sns.service.IRedisService
import com.tapacross.sns.service.RedisService
import com.tapacross.sns.util.ThreadUtil
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.context.support.GenericXmlApplicationContext

import java.util.concurrent.LinkedBlockingQueue

/**
 * @author hgkim
 * Redis DB에 들어있는 keywordList와 ProxyIP를 읽어 InstagramkeywordRecrawlerParseBolt에 전달하는 스파우트
 * 재수집 keywrod의 pageId(기간) 에 따른, 파싱 유무를 위해 JAVA QUEUE를 사용한다.
 *
 */
class InstagramkeywordRecrawlerSpout extends BaseRichSpout {
	private static final String INSTAGRAM_KEYWORD_RE_CRAWLER = "InstagramKeywordReCrawler"
	private static final String INSTAGRAM_PROXY_IP_QUEUE_PREFIX = "proxy:instagram"

	private final Logger logger = LoggerFactory.getLogger(getClass())
	private SpoutOutputCollector collector
	private IRedisService redisService
	private volatile Queue<instagramKeywordDataVO> keywordReuseQueue = new LinkedBlockingQueue<instagramKeywordDataVO>()
	
	private String proxySite
	
	private Map<String, KeyValueItem> AccountMap
	private long searchCount = 0
	private long successCount = 0
	private long failCount = 0
	private long keywordList = 0
	private int emitIndex = 1

	public void open(Map conf, TopologyContext context, SpoutOutputCollector collector) {

		this.logger.info(INSTAGRAM_KEYWORD_RE_CRAWLER + " open() Start!")
		this.collector = collector
		this.AccountMap = new HashMap()
		this.proxySite = conf.get("proxy.site")
		def applicationContext = new GenericXmlApplicationContext("classpath:spring/application-context.xml")
		this.redisService = applicationContext.getBean(RedisService.class)
		logger.info(INSTAGRAM_KEYWORD_RE_CRAWLER + " open() End !")

	}
	@Override
	public void nextTuple() {
		def keyword
		def pageid
		def proxyJson
		def keywordQue= keywordReuseQueue.peek() // LinkedBlockingQueue 선두에 담겨있는 데이터를 읽는다.

		// 큐에 담겨있는 데이터 유무 확인
		if(keywordQue != null){

			// 같은 키워드로 특정 기간까지 더 파싱할 데이터가 있는지 유무 확인

			if(!keywordQue.isContiune()){ // 더이상 파싱할 데이터 없는 경우
				logger.info(INSTAGRAM_KEYWORD_RE_CRAWLER + " keyword exist ")

				keywordReuseQueue.remove(keywordQue) // 해당 큐 삭제
				return;
			}

				// 해당 키워드로 더 파싱할 데이터가 있는 경우
				keyword = keywordQue.getKeyword()
				proxyJson = keywordQue.getProxyJson()

				try{
					if(proxyJson == null){
						proxyJson = redisService.popRedisQueue(INSTAGRAM_PROXY_IP_QUEUE_PREFIX + ":" +proxySite)
					}

				}catch (Exception e){
					e.printStackTrace()
				}

				if(proxyJson == null){
					ThreadUtil.sleepSec(1)
					return
				}

				pageid = keywordQue.getPageId()

				logger.info(INSTAGRAM_KEYWORD_RE_CRAWLER + " keyword emit Spout -> Parse Bolt Start : $keyword, pageId : $pageid ")
				emitItem(keyword,proxyJson,pageid, keywordReuseQueue)

				keywordReuseQueue.remove(keywordQue) // 해당 데이터 큐를 비워준다 (Parse Bolt 에서 데이터 큐를 다시 채워주기 때문)

				logger.info(INSTAGRAM_KEYWORD_RE_CRAWLER + " keyword emit Spout -> Parse Bolt Success! : $keyword, pageId : $pageid ")

		// 큐에 담겨있는 데이터가 없는경우 (신규 키워드로 파싱)
		}else{
			try{
				keyword = redisService.popRedisQueue("keywordListTest")

				if(keyword == null){
					logger.info("keyword is null.")
					ThreadUtil.sleepSec(1)
					return
				}

				proxyJson = redisService.popRedisQueue(INSTAGRAM_PROXY_IP_QUEUE_PREFIX + ":" +proxySite)
				logger.info(INSTAGRAM_KEYWORD_RE_CRAWLER + " proxyJson : $proxyJson")

				if(proxyJson == null){
					logger.info("proxyJson is null.")
					ThreadUtil.sleepSec(1)
					return
				}

				pageid = "QVFBY3hTa3lhZHhIY2hhdzFXRWpnRV9DcnFnNWNqWXNMcUo2Uktjb3ROcnd6WUljU05IZzZoaXAzbEF1V21JOFdnTkw4bWsxS3o4MFl5UFF5Nzg1OFJlRA=="

				logger.info(INSTAGRAM_KEYWORD_RE_CRAWLER + " keyword emit Spout -> Parse Bolt Start : $keyword")
				emitItem(keyword, proxyJson, pageid, keywordReuseQueue)

				keywordReuseQueue.remove(keywordQue) // 해당 데이터 큐를 비워준다 (Parse Bolt 에서 데이터 큐를 다시 채워주기 때문)

				logger.info(INSTAGRAM_KEYWORD_RE_CRAWLER + " keyword emit Spout -> Parse Bolt Success! : $keyword")
			}catch (NullPointerException e1){
				logger.info("keywordQue : ${keywordQue.toString()}")
			}
			catch (Exception e ){
				e.printStackTrace()
			}
		}

	}
	@Override
	public void declareOutputFields(OutputFieldsDeclarer declarer) {
		declarer.declare(new Fields(ConstantOutputField.KEYWORD_FIELD, ConstantOutputField.PROXY_FIELD, ConstantOutputField.PAGE_ID, ConstantOutputField.QUEUE_FILED))
	}

	@Override
	void ack(Object msgId) {
		++successCount
		++emitIndex
		def item = AccountMap.remove(msgId as String)
		logger.info(INSTAGRAM_KEYWORD_RE_CRAWLER + " acked value=${item.getValue()}, successCount=$successCount" )

	}

	@Override
	void fail(Object msgId) {
		++failCount
		successCount = 0
		def item = AccountMap.remove(msgId as String)
		logger.info(INSTAGRAM_KEYWORD_RE_CRAWLER + " failed value=$item.value, value2=$item.value2, failCount=$failCount")

	}

	private void emitItem(String keyword, String proxyJson, String pageId, Queue<instagramKeywordDataVO> keywordReuseQueue) {
		def emitKey = UUID.randomUUID().toString()
		AccountMap.put(emitKey, new KeyValueItem(emitKey, keyword, proxyJson))
		++searchCount
		collector.emit(new Values(keyword, proxyJson, pageId, keywordReuseQueue), emitKey)
	}

}

