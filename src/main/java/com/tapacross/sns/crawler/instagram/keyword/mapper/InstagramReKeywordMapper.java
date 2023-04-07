package com.tapacross.sns.crawler.instagram.keyword.mapper;

import com.tapacross.sns.entity.TBCrawlSite;
import com.tapacross.sns.entity.TBProxyHistory;
import com.tapacross.sns.entity.crawl.Account;
import com.tapacross.sns.entity.crawl.AccountHistory;

public interface InstagramReKeywordMapper {
	public TBCrawlSite selectSiteBySiteOldId(String userId);

}
