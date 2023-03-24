//package com.tapacross.sns.crawler.instagram.keyword.service;
//
//import java.util.List;
//
//import org.springframework.beans.factory.annotation.Autowired;
//import org.springframework.dao.DataAccessException;
//import org.springframework.stereotype.Controller;
//
//import com.tapacross.sns.crawler.instagram.keyword.dao.IInstagramKeywordCrawlerDao;
//import com.tapacross.sns.crawler.instagram.keyword.mapper.InstagramKeywordMapper;
//import com.tapacross.sns.entity.TBCrawlSite;
//import com.tapacross.sns.entity.TBCrawlSite2;
//import com.tapacross.sns.entity.TBFilterKeyword;
//import com.tapacross.sns.entity.TBProxyHistory;
//import com.tapacross.sns.entity.crawl.Account;
//import com.tapacross.sns.entity.crawl.AccountHistory;
//import com.tapacross.sns.entity.filter.TBSpamArticle;
//
//@Controller
//public class InstagramKeywordCrawlerService implements
//		IInstagramKeywordCrawlerService {
//	@Autowired
//	private IInstagramKeywordCrawlerDao dao;
//
//	@Autowired
//	private InstagramKeywordMapper mapper;
//
//	@Override
//	public List<TBFilterKeyword> selectFilterKeywords(String siteType)
//			throws DataAccessException {
//		return dao.selectFilterKeywords(siteType);
//	}
//
//	@Override
//	public TBSpamArticle insertSpamArticleIfAbsent(TBSpamArticle entity)
//			throws DataAccessException {
//		if (selectSpamArticle(entity) == null)
//			return dao.insertSpamArticleIfAbsent(entity);
//
//		return null;
//	}
//
//	@Override
//	public TBSpamArticle selectSpamArticle(TBSpamArticle entity)
//			throws DataAccessException {
//		return dao.selectSpamArticle(entity);
//	}
//
//	@Override
//	public void updateAccountLastUseDate(Account entity) throws DataAccessException {
//		mapper.updateAccountLastUseDate(entity);
//	}
//
//	@Override
//	public TBCrawlSite selectSiteBySiteOldId(String userId) throws DataAccessException {
//		return mapper.selectSiteBySiteOldId(userId);
//	}
//
//	@Override
//	public List<TBCrawlSite2> selectSiteBySiteName(String siteName) throws DataAccessException {
//		return dao.selectUsersBySiteName(siteName);
//	}
//
//	@Override
//	public void updateCrawlSite(TBCrawlSite2 entity) throws DataAccessException {
//		dao.updateCrawlSite(entity);
//	}
//
//	@Override
//	public Account selectEnableAccount(Account entity) throws DataAccessException {
//		return mapper.selectEnableAccount(entity);
//	}
//
//	@Override
//	public void insertAccountHistory(AccountHistory entity) throws DataAccessException {
//		mapper.insertAccountHistory(entity);
//	}
//
//	@Override
//	public void insertProxyHistory(TBProxyHistory entity) throws DataAccessException {
//		mapper.insertProxyHistory(entity);
//	}
//}
