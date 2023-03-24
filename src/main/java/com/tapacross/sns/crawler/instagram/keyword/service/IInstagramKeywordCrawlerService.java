//package com.tapacross.sns.crawler.instagram.keyword.service;
//
//import java.util.List;
//
//import org.springframework.dao.DataAccessException;
//
//import com.tapacross.sns.entity.TBCrawlSite;
//import com.tapacross.sns.entity.TBCrawlSite2;
//import com.tapacross.sns.entity.TBFilterKeyword;
//import com.tapacross.sns.entity.TBProxyHistory;
//import com.tapacross.sns.entity.crawl.Account;
//import com.tapacross.sns.entity.crawl.AccountHistory;
//import com.tapacross.sns.entity.filter.TBSpamArticle;
//
//public interface IInstagramKeywordCrawlerService {
//	List<TBFilterKeyword> selectFilterKeywords(String siteType) throws DataAccessException;
//
//	TBSpamArticle insertSpamArticleIfAbsent(TBSpamArticle entity) throws DataAccessException;
//
//	TBSpamArticle selectSpamArticle(TBSpamArticle entity) throws DataAccessException;
//
//	void updateAccountLastUseDate(Account entity) throws DataAccessException;
//
//	/**
//	 * ownerid가 같지만 userName이 바뀐 수집원이 존재할 수 있기에 수집원중에서 방문일자가 최근인 수집원만 가져온다.
//	 * @param userId
//	 * @return
//	 * @throws DataAccessException
//	 */
//	TBCrawlSite selectSiteBySiteOldId(String siteIdOld) throws DataAccessException;
//
//	List<TBCrawlSite2> selectSiteBySiteName(String siteName) throws DataAccessException;
//
//	void updateCrawlSite(TBCrawlSite2 entity) throws DataAccessException;
//
//	Account selectEnableAccount(Account entity) throws DataAccessException;
//
//	void insertAccountHistory(AccountHistory entity) throws DataAccessException;
//
//	/**
//	 * 프록시 사용 정보를 저정한다.
//	 * @param entity
//	 * @throws DataAccessException
//	 */
//	void insertProxyHistory(TBProxyHistory entity) throws DataAccessException;
//}
