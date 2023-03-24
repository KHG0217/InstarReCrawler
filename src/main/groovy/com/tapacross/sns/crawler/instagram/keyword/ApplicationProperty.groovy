package com.tapacross.sns.crawler.instagram.keyword

import groovy.transform.TypeChecked

import org.springframework.beans.factory.annotation.Autowired

/**
 * 스프링에서 불러온 설정파일에 접근하여 속성값을 가져오는 클래스
 * @author cuckoo03
 *
 */
@TypeChecked
class ApplicationProperty {
	@Autowired
	private Properties applicationProperties

	public String get(String key) {
		return applicationProperties.getProperty(key)
	}
}
