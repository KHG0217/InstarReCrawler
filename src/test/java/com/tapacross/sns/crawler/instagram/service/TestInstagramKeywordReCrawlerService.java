package com.tapacross.sns.crawler.instagram.service;

import com.tapacross.sns.alarm.SiteType;
import com.tapacross.sns.crawler.instagram.keyword.service.IInstagramKeywordReCrawlerService;
import com.tapacross.sns.entity.TBCrawlSite;
import com.tapacross.sns.entity.TBFilterKeyword;
import com.tapacross.sns.entity.filter.TBSpamArticle;
import com.tapacross.sns.util.DateFormatUtil;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import java.util.Calendar;
import java.util.List;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration("classpath:spring/application-context.xml")
public class TestInstagramKeywordReCrawlerService {

    @Autowired
    private IInstagramKeywordReCrawlerService instagramKeywordReCrawlerService;

    @Test
    public void testSelectSiteBySiteOldId(){
        TBCrawlSite data = instagramKeywordReCrawlerService.selectSiteBySiteOldId("13134923767");
        System.out.println("data : " + data);
    }

    @Test
    public void testSelectFilterKeyword(){
        List<TBFilterKeyword> filterKeyword = instagramKeywordReCrawlerService.selectFilterKeywords("I");

        System.out.println("fiterkeyword : " + filterKeyword.get(2));
    }

    @Test
    public void testInsertSpamArticleIfAbsent(){
        TBSpamArticle spamArticle = new TBSpamArticle(1002, 1234L,
                "I");
        spamArticle.setSiteId("testId");
        spamArticle.setCrawlDate(DateFormatUtil.getDateString(
                Calendar.getInstance(), 0, "yyyyMMddHHmmss"));
        spamArticle.setCollectedBy("K");
        spamArticle.setCreateDate("20230404120600");
        spamArticle.setSiteName("testSiteName");
        spamArticle.setUrl("https://www.test.test.test");
        spamArticle.setKeyword("testKeyword");
        instagramKeywordReCrawlerService.insertSpamArticleIfAbsent(spamArticle);
    }
}
