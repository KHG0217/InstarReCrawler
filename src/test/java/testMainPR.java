import com.tapacross.sns.crawler.instagram.keyword.instagramKeywordDataVO;
import com.tapacross.sns.entity.TBFilterKeyword;
import org.junit.Test;

import java.util.Queue;
import java.util.concurrent.LinkedBlockingQueue;

public class testMainPR {

    @Test
    public void testVO() {
        Queue<instagramKeywordDataVO> keywordReuseQueue = new LinkedBlockingQueue<instagramKeywordDataVO>();
        instagramKeywordDataVO test = new instagramKeywordDataVO();

        test.setKeyword("test");
        test.setProxyJson("123.456.789");
        test.setPageId("rjfhwiufhweuifwh");

        keywordReuseQueue.offer(test);

        System.out.println(keywordReuseQueue.poll().getKeyword());
    }

//    @Test
//    public void testFilterKeyword(){
//        String
//        for(TBFilterKeyword keyword : filterKeywords) {
//
//        }
//    }

}
