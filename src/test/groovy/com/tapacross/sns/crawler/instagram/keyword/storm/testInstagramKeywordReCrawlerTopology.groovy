package com.tapacross.sns.crawler.instagram.keyword.storm

import org.junit.Test

class testInstagramKeywordReCrawlerTopology {
    @Test
    public void  test() {
        def args = new String[5]
        args[0] = "0" // runmode
        args[1] = "1" // num of worker
        args[2] = "2" // num of bolt
        args[3] = "haiip"
        new InstagramKeywordReCrawlerTopology().main(args)
    }
}
