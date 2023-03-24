package com.tapacross.sns.crawler.instagram.keyword.storm

import org.json.JSONObject
import org.jsoup.HttpStatusException
import org.junit.Test
import org.junit.runner.RunWith
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.test.context.ContextConfiguration
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner

import com.esmedia.log.LoggerWrapper
import com.google.gson.GsonBuilder
import com.tapacross.sns.crawler.StringUtil
import com.tapacross.sns.crawler.instagram.parser.InstagramKeywordParser
import com.tapacross.sns.entity.TBProxy
import com.tapacross.sns.parser.JSoupFactory
import com.tapacross.sns.service.RedisService
import com.tapacross.sns.util.ThreadUtil

import groovy.transform.TypeChecked

/**
 *  키워드를 이용하여 게시물 검색 구현을 위한 샘플 소스코드
 *  
 *  주요기능
 *  -요청결과 게시물보다 과거 게시물이 존재할 경우 과거 게시물 반복 수집
 *  -더이상 검색할 게시물이 없을 경우 프로그램 종료
 *  -게시물 작성일자가 지정된 기간보다 오래된 경우 프로그램 종료
 *  
 * @author jkko
 *
 */
@TypeChecked
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration("classpath:spring/application-context.xml")
class FocusedInstagramKeywordCrawlerTest {
	private static final String INSTAGRAM_KEYWORD_CRAWLER = "InstagramKeywordCrawler"
	
	/**
	 * proxy ip pop quque[gson.toJson(it, TBProxy.class)]
	 * format = proxy:채널명:프록시 제공업체(haiproxy, youngip, haiip)
	 */
	private final String INSTAGRAM_PROXY_IP_QUEUE_PREFIX = "proxy:instagram"

	@Autowired
	private RedisService redisService
	
	private String manyAriticleKeyword = "음식"
	private String smallAriticleKeyword = "하둡"
	
	@Test
	def void test() {
		def hashtag = manyAriticleKeyword
		/*
		 * 쿠키는 고정 값
		 */
		def cookies = createCookies()
		def exitFlag = false
		/*
		 * nextPageId는 특정시간대를 검색할 수 있는 커서를 의미하며 모든 게시물에 적용 가능
		 */
		def nextPageId = "QVFBY3hTa3lhZHhIY2hhdzFXRWpnRV9DcnFnNWNqWXNMcUo2Uktjb3ROcnd6WUljU05IZzZoaXAzbEF1V21JOFdnTkw4bWsxS3o4MFl5UFF5Nzg1OFJlRA=="
		def exitStartDate = "20220500000"
		
		while (!exitFlag) {
			def proxyJson = redisService.popRedisQueue(INSTAGRAM_PROXY_IP_QUEUE_PREFIX + ":" + "haiip")
			if (proxyJson == null) {
				LoggerWrapper.info(INSTAGRAM_KEYWORD_CRAWLER, "proxyJson is null.")
				ThreadUtil.sleepSec(1)
				continue
			}
			def proxy = new GsonBuilder().create().fromJson(proxyJson, TBProxy.class)
			
			
			def parser = new InstagramKeywordParser()
			def res = null
			try {
				res = parseExplorerHashtag(hashtag, cookies, proxy.ip, proxy.port, nextPageId)
			} catch (Exception e) {
				LoggerWrapper.info(INSTAGRAM_KEYWORD_CRAWLER, e.message)
				continue
			}
			
			if(StringUtil.isEmpty(res)) { // api 결과가 비어있을 경우 정상으로 통과
				LoggerWrapper.info(INSTAGRAM_KEYWORD_CRAWLER, "parse failed. value=$hashtag, result=$res")
				return
			}
			
			// org.json.JSONException: Unterminated string at 에러 발생하여 줄바꿈 특수태그를 치환
			def jsonObj = new JSONObject(res.replace("\n", " "))
			if (jsonObj.toString().size() < 100) { // ex {"data":{"user":null},"status":"ok"}
				LoggerWrapper.info(INSTAGRAM_KEYWORD_CRAWLER, "parse result length size is short. value=$hashtag, result=$jsonObj")
				return
			}

			
			def articleList = parser.getArticleList(jsonObj.getJSONObject("graphql"))


			if (articleList == null || articleList.length() == 0) { // 최근 글 목록이 없을 경우
				LoggerWrapper.info(INSTAGRAM_KEYWORD_CRAWLER, "parse article list is null. ${jsonObj.toString()}")
				return
			}
			def test = parser.parseArticleFromJson(articleList.getJSONObject(0))

			def pageInfo = jsonObj.getJSONObject("graphql").getJSONObject("hashtag").getJSONObject("edge_hashtag_to_media").getJSONObject("page_info")
			def hasNextPage = pageInfo.getBoolean("has_next_page") 
			def endCursor = pageInfo.optString("end_cursor", "")
			LoggerWrapper.info(INSTAGRAM_KEYWORD_CRAWLER, "endCursor=$endCursor")
			
			def date = '20221131000000'
			if(test.createDate > date) {
				println("-----------------articel pass !!!!! : " + test.createDate)
				nextPageId = endCursor
				continue
			}
			
			// 더이상 검색할 게시물이 없을 경우 hasNextPage는 false를 리턴
			if (hasNextPage == false) {
				LoggerWrapper.info(INSTAGRAM_KEYWORD_CRAWLER, "page search end")
				exitFlag = true
			} else {
				nextPageId = endCursor
			}
			
			
			for(int i=0; i<articleList.length(); i++) {
				def articleContent = parser.parseArticleFromJson(articleList.getJSONObject(i))
				if(null == articleContent)
					continue
					
				LoggerWrapper.info(INSTAGRAM_KEYWORD_CRAWLER, articleContent.createDate + ", " + articleContent.content)
				if (articleContent.createDate < exitStartDate) {
					LoggerWrapper.info(INSTAGRAM_KEYWORD_CRAWLER, "old article found. exit loop.")
					exitFlag = true
					break
				}
			}
		} 
		
	}
	
	private Map<String, String> createCookies() {
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
		String host, int port, String nextPageId) throws HttpStatusException {
		def encodeedHashtag = URLEncoder.encode(hashtag, "UTF-8")
		def url = "https://www.instagram.com/explore/tags/$encodeedHashtag/?__a=1&__d=dis&max_id=$nextPageId"
		LoggerWrapper.info(INSTAGRAM_KEYWORD_CRAWLER, "search keyword=$hashtag, url=$url");
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
		LoggerWrapper.info(INSTAGRAM_KEYWORD_CRAWLER, "jsoup status:${res.statusCode()}, cookies:${res.cookies()}");
		
		if (res.statusCode() != 200)
			throw new HttpStatusException("response status is not OK", res.statusCode(), res.body())
		return res.body()
	}
}
