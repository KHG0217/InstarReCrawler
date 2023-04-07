package com.tapacross.sns.crawler.instagram.keyword.storm.bolt

import backtype.storm.task.OutputCollector
import backtype.storm.task.TopologyContext
import backtype.storm.topology.OutputFieldsDeclarer
import backtype.storm.topology.base.BaseRichBolt
import backtype.storm.tuple.Tuple
import backtype.storm.tuple.Values
import com.tapacross.sns.alarm.SiteType
import com.tapacross.sns.crawler.instagram.keyword.instagramKeywordDataVO
import com.tapacross.sns.crawler.instagram.keyword.service.IInstagramKeywordReCrawlerService
import com.tapacross.sns.crawler.instagram.keyword.service.InstagramKeywordReCrawlerService
import com.tapacross.sns.crawler.instagram.keyword.storm.ConstantOutputField

import com.tapacross.sns.entity.TBCrawlSite
import com.tapacross.sns.entity.TBFilterKeyword

import com.tapacross.sns.entity.filter.TBSpamArticle
import com.tapacross.sns.json.SNSInfoHelper

import com.tapacross.sns.service.IRedisService
import com.tapacross.sns.service.RedisService
import com.tapacross.sns.thrift.SNSContent
import com.tapacross.sns.thrift.SNSInfo
import com.tapacross.sns.util.CRCUtil
import com.tapacross.sns.util.DateFormatUtil
import com.tapacross.sns.util.KeyUtil

import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.context.support.GenericXmlApplicationContext
import org.springframework.dao.DataAccessException

class InstagramkeywordRecrawlerExtractBolt extends BaseRichBolt {
    private static final String INSTAGRAM_KEYWORD_RE_CRAWLER = "InstagramKeywordReCrawler"
    private final String INSTAGRAM_CRAWL_URL_KEY_PREFIX = "crawlurl:instagram:"
    private final String INSTAGRAM_URL = "https://instagram.com";
    private final Logger logger = LoggerFactory.getLogger(getClass())

    private IRedisService redisService
    private IInstagramKeywordReCrawlerService instagramKeywordService
    private OutputCollector collector
    private TopologyContext context
    private List<TBFilterKeyword> filterKeywords
    // 저장 게시물큐
    private final String REDIS_INSTAGRAM_KEYWORD_CRAWL_SNSINFO_QUEUE_PREFIX = "crawl:keyword:snsinfo:instagram";
    private int priority

    @Override
    void prepare(Map stormConf, TopologyContext context, OutputCollector collector) {
        this.collector = collector
        this.context = context
        def applicationContext = new GenericXmlApplicationContext("classpath:spring/application-context.xml")
        this.instagramKeywordService = applicationContext.getBean(InstagramKeywordReCrawlerService.class)
        // instagram 키워드 서비스 만들기
        filterKeywords = instagramKeywordService.selectFilterKeywords("I")
        this.redisService = applicationContext.getBean(RedisService.class)

    }

    @Override
    void execute(Tuple input) {
        def parseContentList = input.getValueByField(ConstantOutputField.PARSE_DATA_LIST_FIELD) as ArrayList
        def keyword = input.getStringByField(ConstantOutputField.KEYWORD_FIELD)
        def filteredContents = new ArrayList<SNSInfo>()

        parseContentList.forEach {it  ->
            def parseContent = it as SNSContent
            logger.info(INSTAGRAM_KEYWORD_RE_CRAWLER+" :"  + parseContent.createDate + ", " + parseContent.content)

            logger.info(INSTAGRAM_KEYWORD_RE_CRAWLER + " parsedata : $parseContent")

            if (existRedis(parseContent.url)) {
                return
            }

            // instagrame selectSitebyOldId service 생성
            TBCrawlSite crawlSite = instagramKeywordService.selectSiteBySiteOldId(parseContent.getVia())

            // DB에 존재하지 않는 수집원일 경우 PASS
            if(crawlSite == null){
                logger.info(INSTAGRAM_KEYWORD_RE_CRAWLER + " CrawlSite Id is null PASS. $parseContent.url")
                return
            }

            parseContent.setSiteName(crawlSite.getSiteName())
            parseContent.setWriterId(crawlSite.getSiteName());
            parseContent.setScreenName(crawlSite.getSiteName());
            parseContent.setSiteName(crawlSite.getSiteName());
            parseContent.setSiteId(crawlSite.getSiteId())

            // filter 키워드가 들어간 Content PASS
            if(filteringKeyword(parseContent)){
                logger.info(INSTAGRAM_KEYWORD_RE_CRAWLER + " FilterKeyword include. $parseContent.url")
                return
            }

            SNSInfo info = new SNSInfo()
            info.setPicture(""); // rquired to db insert.
            info.setSiteName(parseContent.getSiteName())
            // insta user 고유 ID
            info.setLocation(crawlSite.getSiteIdOld())
            info.setBio(null)
            info.setWeb(null)

            info.setSiteType("I")

            info.setUrl(INSTAGRAM_URL + "/" + parseContent.getSiteName());
            info.setSiteSubCate(null);
            info.setSiteCategory(null);
            info.setFollower(crawlSite.getFollower())

            info.setPriority(0)
            List<SNSContent> snsContentList = new ArrayList()
            snsContentList.add(parseContent)
            info.setSnsContent(snsContentList)

            // 유저당 하나의 게시물 만으로 외국계정인지 판별이 불가하여 아래의 is valid lang 로직은 사용중지하고 강제로 Y 설정
            info.setIsValidLang("Y")

            filteredContents.add(info)
        }

        for(int i=0; i<filteredContents.size(); i++) {
            def info = filteredContents.get(i)

            info.getSnsContent().each {
                addRedisValue(it.getUrl())
                setSNSInfo(info)
                logger.info("emit extract bolt. $keyword, $it.createDate, $it.url")
            }
        }
        collector.emit(input, new Values(keyword)) // 종료
        collector.ack(input)
    }

    @Override
    void declareOutputFields(OutputFieldsDeclarer declarer) {

    }

    private boolean existRedis(String url) {
        def redisCrawlUrlKey = INSTAGRAM_CRAWL_URL_KEY_PREFIX + CRCUtil.getCRC32(url)
        if (redisService.getRedisValue(redisCrawlUrlKey) != null) {
            logger.info(INSTAGRAM_KEYWORD_RE_CRAWLER + " exist article url. $url, $redisCrawlUrlKey")
            return true
        }
        return false
    }


    private boolean filteringKeyword(SNSContent snsContent) {
        String content = snsContent.content
        String url = snsContent.url
        for(TBFilterKeyword keyword : filterKeywords) {
            if (content.contains(keyword.getKeyword().trim())) {
                logger.info(INSTAGRAM_KEYWORD_RE_CRAWLER + " Filtered Keyword=" + keyword.getKeyword() + ", url=" + url)
                // 인서트 하기 위해서는 site_id가 필요함
                insertSpamArticle(snsContent, keyword.filterCode, keyword.keyword)
                return true
            }
        }
        return false
    }

    def void insertSpamArticle(SNSContent content, int spamCode,
                               String keyword = null) {
        def articleId = KeyUtil.makeArticleIdFromSNSContent(content)
        content.articleId = articleId
        logger.warn(INSTAGRAM_KEYWORD_RE_CRAWLER + " Too many links. articleId=$content.articleId, url=$content.url")
        def spamArticle = new TBSpamArticle(spamCode, content.articleId,
                SiteType.INSTAGRAM.value)
        spamArticle.siteId = content.siteName
        spamArticle.crawlDate = DateFormatUtil.getDateString(
                Calendar.instance, 0, "yyyyMMddHHmmss")
        spamArticle.collectedBy = "K"
        spamArticle.createDate = content.createDate
        spamArticle.siteName = content.siteName
        spamArticle.url = content.url
        spamArticle.keyword = keyword
        instagramKeywordService.insertSpamArticleIfAbsent(spamArticle)
    }

    private void addRedisValue(String url) {
        def redisCrawlUrlKey = INSTAGRAM_CRAWL_URL_KEY_PREFIX + CRCUtil.getCRC32(url)
        try {
            redisService.addRedisValue(redisCrawlUrlKey, "1", 7)
            logger.info(INSTAGRAM_KEYWORD_RE_CRAWLER + " REDIS KEYWORD ADD REDISVALUE : " + redisCrawlUrlKey)
        } catch (DataAccessException e) {
            e.printStackTrace()
            logger.info(INSTAGRAM_KEYWORD_RE_CRAWLER + " REDIS EXCEPTION : " + url)
        }
    }

    private void setSNSInfo(SNSInfo snsInfo) {
        def json = SNSInfoHelper.snsInfoToJson(snsInfo)
        redisService.pushFixedSkipQueue(REDIS_INSTAGRAM_KEYWORD_CRAWL_SNSINFO_QUEUE_PREFIX + ":" + priority, json, 1000)
        logger.info(INSTAGRAM_KEYWORD_RE_CRAWLER + " REDIS KEYWORD PUSH : " + snsInfo)
    }
}
