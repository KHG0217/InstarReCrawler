package com.tapacross.sns.crawler.instagram.keyword.service;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataAccessException;
import org.springframework.stereotype.Controller;

import com.tapacross.sns.crawler.instagram.keyword.dao.IInstagramKeywordReCrawlerDao;
import com.tapacross.sns.crawler.instagram.keyword.mapper.InstagramReKeywordMapper;
import com.tapacross.sns.entity.TBCrawlSite;
import com.tapacross.sns.entity.TBCrawlSite2;
import com.tapacross.sns.entity.TBFilterKeyword;
import com.tapacross.sns.entity.TBProxyHistory;
import com.tapacross.sns.entity.crawl.Account;
import com.tapacross.sns.entity.crawl.AccountHistory;
import com.tapacross.sns.entity.filter.TBSpamArticle;

@Controller
public class InstagramKeywordReCrawlerService implements
        IInstagramKeywordReCrawlerService {
	@Autowired
	private IInstagramKeywordReCrawlerDao dao;

	@Autowired
	private InstagramReKeywordMapper mapper;

	@Override
	public List<TBFilterKeyword> selectFilterKeywords(String siteType)
			throws DataAccessException {
		return dao.selectFilterKeywords(siteType);
	}

	@Override
	public TBSpamArticle insertSpamArticleIfAbsent(TBSpamArticle entity)
			throws DataAccessException {
		if (selectSpamArticle(entity) == null)
			return dao.insertSpamArticleIfAbsent(entity);

		return null;
	}

	@Override
	public TBSpamArticle selectSpamArticle(TBSpamArticle entity)
			throws DataAccessException {
		return dao.selectSpamArticle(entity);
	}

	@Override
	public TBCrawlSite selectSiteBySiteOldId(String userId) throws DataAccessException {
		return mapper.selectSiteBySiteOldId(userId);
	}

}
