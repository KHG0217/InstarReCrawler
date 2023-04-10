package com.tapacross.sns.crawler.instagram.keyword.dao;

import java.util.List;

import javax.annotation.Resource;

import org.hibernate.Session;
import org.hibernate.criterion.Restrictions;
import org.springframework.dao.DataAccessException;
import org.springframework.orm.hibernate3.HibernateTemplate;
import org.springframework.stereotype.Repository;

import com.tapacross.sns.entity.TBFilterKeyword;
import com.tapacross.sns.entity.filter.TBSpamArticle;

@Repository
public class InstagramKeywordReCrawlerDao implements IInstagramKeywordReCrawlerDao {
	@Resource(name = "hibernateTemplate")
	private HibernateTemplate hibernateTemplate;


	@SuppressWarnings("unchecked")
	@Override
	public List<TBFilterKeyword> selectFilterKeywords(String siteType)
			throws DataAccessException {
		Session session = hibernateTemplate.getSessionFactory().openSession();
		try {
			return session.createCriteria(TBFilterKeyword.class)
					.add(Restrictions.eq("siteType", siteType))
					.add(Restrictions.eq("use", "Y")).list();
		} finally {
			session.close();
		}
	}

	@Override
	public TBSpamArticle insertSpamArticleIfAbsent(TBSpamArticle entity)
			throws DataAccessException {
		return (TBSpamArticle) hibernateTemplate.save(entity);
	}

	@Override
	public TBSpamArticle selectSpamArticle(TBSpamArticle entity)
			throws DataAccessException {
		Session session = hibernateTemplate.getSessionFactory().openSession();
		try {
			return (TBSpamArticle) session.createCriteria(TBSpamArticle.class)
					.add(Restrictions.eq("spamType", entity.getSpamType()))
					.add(Restrictions.eq("articleId", entity.getArticleId()))
					.add(Restrictions.eq("siteType", entity.getSiteType()))
					.uniqueResult();
		} finally {
			session.close();
		}
	}
}
