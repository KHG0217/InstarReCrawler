package com.tapacross.sns.crawler.instagram.keyword

import org.junit.Test

class testInstagramkeywordRecrawler {
	
	static InstagramkeywordRecrawler instagrameClass = new InstagramkeywordRecrawler();
		
	@Test
	def void readKeywordList() {
		def a = instagrameClass.readKeywordList();
		println(a.size())
	}	
}
