package com.tapacross.sns.crawler.instagram.keyword

class InstagramkeywordRecrawler {
	
	

	// KeywordList 를 읽는다.
	private List<String> readKeywordList(){
		List<String> keywordList = new ArrayList<>();
		try {
			BufferedReader reader = new BufferedReader(				
					 new FileReader("../instagram-keyword-recrawler/src/main/resources/data/keywordList_1.txt"), // Read file
					 16 * 1024
			 );
			
			String str;
			 while((str = reader.readLine()) != null) {
				 keywordList.add(str);
			 }		
			 return keywordList;
		}catch (Exception e) {
			e.printStackTrace();
		}

		return keywordList
	}
}
