package com.tapacross.sns.crawler.instagram.keyword

import com.tapacross.sns.crawler.StringUtil
import com.tapacross.sns.crawler.instagram.parser.InstagramKeywordParser
import com.tapacross.sns.entity.TBProxy
import com.tapacross.sns.parser.JSoupFactory
import com.tapacross.sns.util.ThreadUtil
import org.json.JSONObject
import org.jsoup.HttpStatusException
import org.slf4j.Logger
import org.slf4j.LoggerFactory


class FocusedInstagramKeywordCrawler {

    private static final String INSTAGRAM_KEYWORD_CRAWLER = "InstagramKeywordCrawler"
    Logger logger = LoggerFactory.getLogger(getClass())
    private pageId ="";

    private void name(String hashtag, TBProxy proxy){

        // 쿠키 값
        def cookies = createCookies()
        def exitFlag = false // 반복문 실행할 변수

        // 특정 기간부터 페이지 읽기
        pageId ="QVFBY3hTa3lhZHhIY2hhdzFXRWpnRV9DcnFnNWNqWXNMcUo2Uktjb3ROcnd6WUljU05IZzZoaXAzbEF1V21JOFdnTkw4bWsxS3o4MFl5UFF5Nzg1OFJlRA=="
        def existPageDate ="20220501000000" // 끝날시간 설정

        while (!exitFlag){
            if(proxy == null){
                logger.info(INSTAGRAM_KEYWORD_CRAWLER, "proxy is null")
                ThreadUtil.sleepSec(1)
                continue
            }
            def parser = new InstagramKeywordParser()
            def res = null

            try{
                res = parseExplorerHashtag(hashtag, cookies, proxy.ip, proxy.port, pageId )

            }catch (Exception e){
                logger.info(INSTAGRAM_KEYWORD_CRAWLER,e.message)
                continue
            }

            if(StringUtil.isEmpty(res)) { // api 결과가 비어있을 경우 정상으로 통과
                logger.info(INSTAGRAM_KEYWORD_CRAWLER, "parse res is . value=$hashtag, result=$res")
                return
            }

            // org.json.JSONException: Unterminated string at 에러 발생하여 줄바꿈 특수태그를 치환
            def jsonObj = new JSONObject(res.replace("\n", " "))
            if (jsonObj.toString().size() < 100) { // ex {"data":{"user":null},"status":"ok"}
                logger.info(INSTAGRAM_KEYWORD_CRAWLER, "parse result length size is short. value=$hashtag, result=$jsonObj")
                return
            }

            def articelList = parser.getArticleList(jsonObj.getJSONObject("graphql"))

            if(articelList == null || articelList.length() == 0){
                logger.info(INSTAGRAM_KEYWORD_CRAWLER, "parse article list is null. ${jsonObj.toString()}")
                return
            }

            def pageInfo = jsonObj.getJSONObject("graphql").getJSONObject("hashtag").getJSONObject("edge_hashtag_to_media").getJSONObject("page_info")
            def hasNextPage = pageInfo.getBoolean("has_next_page")
            def endCursor = pageInfo.optString("end_cursor", "")
            logger.info(INSTAGRAM_KEYWORD_CRAWLER, "endCursor=$endCursor")

            if(hasNextPage == false) {
                logger.info(INSTAGRAM_KEYWORD_CRAWLER, "page search end")
                exitFlag = true
            }else {
                pageId = endCursor
            }

            for(int i=0; i < articelList.length(); i++){
                def articleContent = parser.parseArticleFromJson(articelList.getJSONObject(i))
                if(null == articleContent)
                    continue

                logger.info(INSTAGRAM_KEYWORD_CRAWLER, articleContent.createDate + ", " + articleContent.content)

                if(articleContent.crawlDate < existPageDate){
                    logger.info(INSTAGRAM_KEYWORD_CRAWLER, "old article found. exit loop.")
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
                                         String host, int port, String paageId) throws HttpStatusException {

        def encodeedHashtag = URLEncoder.encode(hashtag, "UTF-8")
        def url = "https://www.instagram.com/explore/tags/$encodeedHashtag/?__a=1&__d=dis&max_id=$paageId"

        logger.info(INSTAGRAM_KEYWORD_CRAWLER,"search keyword=$hashtag, url=$url")

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
        logger.info(INSTAGRAM_KEYWORD_CRAWLER, "jsoup status:${res.statusCode()}, cookies:${res.cookies()}");

        if (res.statusCode() != 200)
            throw new HttpStatusException("response status is not OK", res.statusCode(), res.body())
        return res.body()
    }
}
