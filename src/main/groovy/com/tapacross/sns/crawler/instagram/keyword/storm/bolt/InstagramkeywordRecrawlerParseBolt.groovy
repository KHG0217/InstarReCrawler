package com.tapacross.sns.crawler.instagram.keyword.storm.bolt

import backtype.storm.task.OutputCollector
import backtype.storm.task.TopologyContext
import backtype.storm.topology.OutputFieldsDeclarer
import backtype.storm.topology.base.BaseRichBolt
import backtype.storm.tuple.Fields
import backtype.storm.tuple.Tuple
import backtype.storm.tuple.Values
import com.google.gson.GsonBuilder
import com.tapacross.sns.crawler.StringUtil
import com.tapacross.sns.crawler.instagram.keyword.instagramKeywordDataVO
import com.tapacross.sns.crawler.instagram.keyword.storm.ConstantOutputField
import com.tapacross.sns.crawler.instagram.parser.InstagramKeywordParser
import com.tapacross.sns.entity.TBProxy
import com.tapacross.sns.parser.JSoupFactory
import com.tapacross.sns.thrift.SNSContent
import com.tapacross.sns.util.ThreadUtil
import org.json.JSONObject
import org.jsoup.HttpStatusException
import org.slf4j.Logger
import org.slf4j.LoggerFactory

import java.util.concurrent.LinkedBlockingQueue

/**
 * @author hgkim
 * 스파우트에서 받아온 키워드와 프록시IP를 받은 후 데이터를 가공하여(스팸키워드 필터적용, 영문글 미수집 등) -> insert bolt에 보낸다.
 * 날짜에 해당(2205 ~ 2211) 하는 DB에 적재한다.
 *
 */
class InstagramkeywordRecrawlerParseBolt extends BaseRichBolt {

    private static final String INSTAGRAM_KEYWORD_RE_CRAWLER = "InstagramKeywordReCrawler"
    private final int CRAWL_DELAY_SECS = 3
    private final int EXCEPTION_DELAY_SECS = 5
    private final Logger logger = LoggerFactory.getLogger(getClass())

    private OutputCollector collector
    private TopologyContext context
    private volatile Queue<instagramKeywordDataVO> keywordReuseQueue = new LinkedBlockingQueue<instagramKeywordDataVO>()

    @Override
    void prepare(Map stormConf, TopologyContext context, OutputCollector collector) {
        this.collector = collector
        this.context = context
    }

    @Override
    void execute(Tuple input) {
        def iKeywordVO = new instagramKeywordDataVO()
        def hashtag = input.getStringByField(ConstantOutputField.KEYWORD_FIELD).replaceAll(" ", "")
        def pageId = input.getStringByField(ConstantOutputField.PAGE_ID)
        def proxyJson = input.getStringByField(ConstantOutputField.PROXY_FIELD)
        def proxy = new GsonBuilder().create().fromJson(proxyJson, TBProxy.class)

        keywordReuseQueue = input.getValueByField(ConstantOutputField.QUEUE_FILED) as Queue<instagramKeywordDataVO>

        try{
            // 쿠키 값
            def cookies = createCookies()

            def existPageDate = "20220500000000" // 끝날시간 설정
            def articelContentList = new ArrayList() // data 담을 List

                def res = parseExplorerHashtag(hashtag, cookies, proxy.ip, proxy.port, pageId )

                def parser = new InstagramKeywordParser()

                if(StringUtil.isEmpty(res)) { // api 결과가 비어있을 경우 정상으로 통과
                    logger.info(INSTAGRAM_KEYWORD_RE_CRAWLER + "parse res is . value=$hashtag, result=$res")
                    return
                }

                // org.json.JSONException: Unterminated string at 에러 발생하여 줄바꿈 특수태그를 치환
                def jsonObj = new JSONObject(res.replace("\n", " "))
                if (jsonObj.toString().size() < 100) { // ex {"data":{"user":null},"status":"ok"}
                    logger.info(INSTAGRAM_KEYWORD_RE_CRAWLER + "parse result length size is short. value=$hashtag, result=$jsonObj" )
                    return
                }

                def articelList = parser.getArticleList(jsonObj.getJSONObject("graphql"))

                if(articelList == null || articelList.length() == 0){
                    logger.info(INSTAGRAM_KEYWORD_RE_CRAWLER + " parse article list is null. ${jsonObj.toString()}")
                    return
                }

                def pageInfo = jsonObj.getJSONObject("graphql").getJSONObject("hashtag").getJSONObject("edge_hashtag_to_media").getJSONObject("page_info")
                def hasNextPage = pageInfo.getBoolean("has_next_page")
                def endCursor = pageInfo.optString("end_cursor", "")
                logger.info(INSTAGRAM_KEYWORD_RE_CRAWLER + " endCursor=$endCursor" )

                // 더이상 페이지존재 x
                if(!hasNextPage) {
                    logger.info(INSTAGRAM_KEYWORD_RE_CRAWLER + "page search end" )
                    pageId =  "QVFBY3hTa3lhZHhIY2hhdzFXRWpnRV9DcnFnNWNqWXNMcUo2Uktjb3ROcnd6WUljU05IZzZoaXAzbEF1V21JOFdnTkw4bWsxS3o4MFl5UFF5Nzg1OFJlRA=="
                    logger.info(INSTAGRAM_KEYWORD_RE_CRAWLER + "iKeywordVO.setContiune(false)" )
                    iKeywordVO.setContiune(false)
                    collector.ack(input)

                }else {
                    pageId = endCursor
                    iKeywordVO.setKeyword(hashtag)
                    iKeywordVO.setPageId(pageId)
                    iKeywordVO.setProxyJson(proxyJson)
                    iKeywordVO.setContiune(true)
                    keywordReuseQueue.offer(iKeywordVO)

                    sleep(CRAWL_DELAY_SECS * 1000)

                }

                for(int i=0; i < articelList.length(); i++){
                    def articleContent = parser.parseArticleFromJson(articelList.getJSONObject(i))

                    if(null == articleContent){
                        logger.info(INSTAGRAM_KEYWORD_RE_CRAWLER + "articleContent is NULL!" )
                        continue
                    }

                    if(articleContent.createDate == null){
                        logger.info(INSTAGRAM_KEYWORD_RE_CRAWLER + "articleContent createDate is NULL!" )
                        continue
                    }

                    if(articleContent.createDate < existPageDate){
                        logger.info(INSTAGRAM_KEYWORD_RE_CRAWLER + " old article found. exit loop." )
                        pageId =  "QVFBY3hTa3lhZHhIY2hhdzFXRWpnRV9DcnFnNWNqWXNMcUo2Uktjb3ROcnd6WUljU05IZzZoaXAzbEF1V21JOFdnTkw4bWsxS3o4MFl5UFF5Nzg1OFJlRA=="
                        logger.info(INSTAGRAM_KEYWORD_RE_CRAWLER + "iKeywordVO.setContiune(false)" )
                        iKeywordVO.setContiune(false)

                    }
                    else{
                        articelContentList.add(articleContent)
                    }

                }
            logger.info(INSTAGRAM_KEYWORD_RE_CRAWLER + " emit Parse Bolt -> Extract Bolt. Start value " )
            collector.emit(input, new Values(articelContentList, hashtag))
            keywordReuseQueue
            collector.ack(input)
            logger.info(INSTAGRAM_KEYWORD_RE_CRAWLER + " emit Parse Bolt -> Extract Bolt. Success! value ")

        } catch (HttpStatusException e) {
            // 해당 키워드가 수집되다가 401 에러시 -> 해당키워드를 반환하지않고 다음 키워드 반환 문제 발생
            logger.error(INSTAGRAM_KEYWORD_RE_CRAWLER + e)
            if (e.statusCode == 429 || e.statusCode == 401) { // (401)몇 분 후에 다시 시도해주세요(로그인 풀림)
                def delay = EXCEPTION_DELAY_SECS
                logger.error(INSTAGRAM_KEYWORD_RE_CRAWLER + " 401 or 429 error sleep $delay secs")
                ThreadUtil.sleepSec(delay)
                logger.error(INSTAGRAM_KEYWORD_RE_CRAWLER + " input Keyword :${input.getStringByField(ConstantOutputField.KEYWORD_FIELD)}")

                iKeywordVO.setKeyword(hashtag)
                iKeywordVO.setPageId(pageId)
                iKeywordVO.setContiune(true)

                keywordReuseQueue.offer(iKeywordVO)

                collector.fail(input)

            } else if (e.statusCode == 400) { //

                def delay = EXCEPTION_DELAY_SECS
                logger.error(INSTAGRAM_KEYWORD_RE_CRAWLER +  " 400 error sleep $delay secs")
                ThreadUtil.sleepSec(delay)

                logger.info(INSTAGRAM_KEYWORD_RE_CRAWLER + "iKeywordVO.setContiune(false)" )
                iKeywordVO.setContiune(false)
                collector.ack(input)

            } else if (e.statusCode == 404) { // page result empty, 해시태그로 검색시 "사용할 없는 페이지인 경우"
                collector.ack(input) // 성공으로 처리
            } else if (e.statusCode == 500) { // not tested
                def delay = EXCEPTION_DELAY_SECS
                logger.error(INSTAGRAM_KEYWORD_RE_CRAWLER + " 500 error sleep $delay secs")
                ThreadUtil.sleepSec(delay)

                logger.info(INSTAGRAM_KEYWORD_RE_CRAWLER + "iKeywordVO.setContiune(false)" )
                iKeywordVO.setContiune(false)
                collector.ack(input)

            } else if (e.statusCode == 560 || e.statusCode == 502) {// known error
                def delay = EXCEPTION_DELAY_SECS
                logger.error(INSTAGRAM_KEYWORD_RE_CRAWLER, " 560 or 502 error sleep $delay secs")
                ThreadUtil.sleepSec(delay)

                logger.info(INSTAGRAM_KEYWORD_RE_CRAWLER + "iKeywordVO.setContiune(false)" )
                iKeywordVO.setContiune(false)
                collector.ack(input)

            } else {
                def delay = EXCEPTION_DELAY_SECS
                logger.error(INSTAGRAM_KEYWORD_RE_CRAWLER, " unknown error : ${e.printStackTrace()}")

                logger.info(INSTAGRAM_KEYWORD_RE_CRAWLER + "iKeywordVO.setContiune(false)" )
                iKeywordVO.setContiune(false)
                collector.ack(input)

            }
        } catch (SocketException e) { // java.net.ConnectException: Connection timed out: connect
            logger.error(INSTAGRAM_KEYWORD_RE_CRAWLER, e);

            iKeywordVO.setKeyword(hashtag)
            iKeywordVO.setPageId(pageId)
            iKeywordVO.setContiune(true)

            keywordReuseQueue.offer(iKeywordVO)

            collector.fail(input)
        } catch (UncheckedIOException e) {
            // org.jsoup.UncheckedIOException: java.net.SocketException: Connection reset 계정 정상
            logger.error(INSTAGRAM_KEYWORD_RE_CRAWLER, e);

            iKeywordVO.setKeyword(hashtag)
            iKeywordVO.setPageId(pageId)
            iKeywordVO.setContiune(true)

            keywordReuseQueue.offer(iKeywordVO)

            collector.fail(input)
        } catch (IOException e) { // proxy client가 같은 서버에서 로그인되지 않았을 경우 HTTP 403 에러가 발생하므로 수집중에는 로그인이 되어있어야 한다.
            logger.error(INSTAGRAM_KEYWORD_RE_CRAWLER, e);
            // java.io.IOException: Unable to tunnel through proxy. Proxy returns "HTTP/1.1 403 Forbidden"
            // java.io.IOException: Unable to tunnel through proxy. Proxy returns "HTTP/1.1 400 Bad Request"

            iKeywordVO.setKeyword(hashtag)
            iKeywordVO.setPageId(pageId)
            iKeywordVO.setContiune(true)

            keywordReuseQueue.offer(iKeywordVO)

            collector.fail(input)
        } catch (Exception e) { // 그외 예외는 예상치 못한 예외로 판단하여 수집을 재시도한다.
            logger.error(INSTAGRAM_KEYWORD_RE_CRAWLER, e);
            def delay = EXCEPTION_DELAY_SECS
            logger.error(INSTAGRAM_KEYWORD_RE_CRAWLER, "sleep $delay secs")
            ThreadUtil.sleepSec(delay)

            iKeywordVO.setKeyword(hashtag)
            iKeywordVO.setPageId(pageId)
            iKeywordVO.setContiune(true)

            keywordReuseQueue.offer(iKeywordVO)

            collector.fail(input)

        } finally {
            logger.info(INSTAGRAM_KEYWORD_RE_CRAWLER, "sleep $CRAWL_DELAY_SECS. value=$hashtag, proxy=$proxy")
            sleep(CRAWL_DELAY_SECS * 1000)
        }
    }

    @Override
    void declareOutputFields(OutputFieldsDeclarer declarer) {
        declarer.declare(new Fields(ConstantOutputField.PARSE_DATA_LIST_FIELD, ConstantOutputField.KEYWORD_FIELD))
    }


    private static Map<String, String> createCookies() throws Exception {
        def currCookies = new HashMap<String, String>()
        currCookies["mid"] = "Y37bWgALAAHUpBZW1LP8VirZ6ffF"
        currCookies["ig_did"] = "7296932B-F8A2-481D-BEE4-FCAEAA740B6B"
        currCookies["ig_nrcb"] = "1"
        currCookies["dpr"] = "0.8999999761581421"
        currCookies["datr"] = "AyZ_Y9H4OAzWdp3lKv8EvwYG"
        currCookies["rur"] = "EAG\05456219263445\0541701129734:01f743ad0ca931feab4462fe45bce3ada8c5d31e1dd2b9f58499ed119d3a98a9f5117567"
        currCookies["csrftoken"] = "WuL1lDgtH8gQkX4fExSJH9uBE7xXIxIB"

        return currCookies
    }

    private  String parseExplorerHashtag(String hashtag, Map<String, String> cookies,
                                         String host, int port, String paageId) throws HttpStatusException {

        def encodeedHashtag = URLEncoder.encode(hashtag, "UTF-8")
        def url = "https://www.instagram.com/explore/tags/$encodeedHashtag/?__a=1&__d=dis&max_id=$paageId"

        logger.info(INSTAGRAM_KEYWORD_RE_CRAWLER + " search keyword=$hashtag, url=$url")

        def res = JSoupFactory.getJsoupConnection(url).timeout(30 * 1000).proxy(host, port)
                .ignoreContentType(true)
                .ignoreHttpErrors(true)
                .header(JSoupFactory.ACCEPT, "*/*")
                .header(JSoupFactory.ACCEPT_ENCODING, "gzip, deflate, br")
                .header("sec-ch-ua", "\"Opera\";v=\"89\", \"Chromium\";v=\"103\", \"_Not:A-Brand\";v=\"24\"")
                .header("sec-ch-ua-mobile", "?0")
                .header("sec-ch-ua-platform", "\"Windows\"")
                .header("sec-fetch-dest", "empty")
                .header("sec-fetch-mode", "cors")
                .header("sec-fetch-site", "same-origin")
                .header("x-asbd-id", "198387")
                .header("x-csrftoken", cookies.get("csrftoken"))
                .header("x-ig-app-id", "936619743392459")
                .cookies(cookies).execute()

        logger.info(INSTAGRAM_KEYWORD_RE_CRAWLER + "jsoup status:${res.statusCode()}, cookies:${res.cookies()}")
        sleep(CRAWL_DELAY_SECS * 1000)

        if (res.statusCode() != 200){
            logger.info("response status is not OK " + res.statusCode() + res.body() )
            throw new HttpStatusException("response status is not OK", res.statusCode(), res.body())
        }
        return res.body()
    }

}
