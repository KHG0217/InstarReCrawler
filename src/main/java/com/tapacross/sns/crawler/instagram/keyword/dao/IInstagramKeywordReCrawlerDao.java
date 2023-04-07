package com.tapacross.sns.crawler.instagram.keyword.dao;

import java.util.List;

import org.springframework.dao.DataAccessException;

import com.tapacross.sns.entity.TBCrawlSite2;
import com.tapacross.sns.entity.TBFilterKeyword;
import com.tapacross.sns.entity.filter.TBSpamArticle;

public interface IInstagramKeywordReCrawlerDao {

	List<TBFilterKeyword> selectFilterKeywords(String siteType)
			throws DataAccessException;
	TBSpamArticle insertSpamArticleIfAbsent(TBSpamArticle entity)
			throws DataAccessException;
	TBSpamArticle selectSpamArticle(TBSpamArticle entity)
			throws DataAccessException;
	
}
