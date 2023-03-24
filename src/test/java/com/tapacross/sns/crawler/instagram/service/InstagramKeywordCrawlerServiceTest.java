package com.tapacross.sns.crawler.instagram.service;

import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import com.tapacross.sns.crawler.instagram.keyword.service.IInstagramKeywordCrawlerService;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration("classpath:spring/application-context.xml")
public class InstagramKeywordCrawlerServiceTest {
	@Autowired
	private IInstagramKeywordCrawlerService service;

}
