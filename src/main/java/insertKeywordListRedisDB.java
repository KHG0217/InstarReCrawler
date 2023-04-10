import com.tapacross.sns.service.RedisService;
import org.springframework.context.support.GenericApplicationContext;
import org.springframework.context.support.GenericXmlApplicationContext;

import java.io.BufferedReader;
import java.io.FileReader;
import java.util.ArrayList;
import java.util.List;

/**
 * @author hgkim
 * 레디스DB에 재수집할 키워드를 등록,삭제하고 등록된사이즈 수를 확인한다.
 *
 * 재수집할 키워드
 *  resources -> data
 *      keywordList_1.txt [ 예상 수집데이터가 적은 키워드 (다양한 키워드로 수집하기 위해 우선 실행)
 *      keywotdList_2.txt [ 예상 수집데이터가 많은 키워드 ]
 *      keywotdList_3.txt [ TEST 키워드 ]
 */
public class insertKeywordListRedisDB {
    private static GenericXmlApplicationContext applicationContext = new GenericXmlApplicationContext(
            "classpath:spring/application-context.xml");
    private static RedisService redisService = applicationContext.getBean(RedisService.class);
    public static void main(String[] args) {
        deleteRedisDB();
        insertRedisDB();
//        selectRedisDB();

    }

    private static void  insertRedisDB(){
        List<String> keywordList = new ArrayList<String>();

        try {
            BufferedReader reader = new BufferedReader(
                    new FileReader("../instagram-keyword-recrawler/src/main/resources/data/keywordList_1.txt"), // Read file
                    16 * 1024
            );
            String str;
            while((str = reader.readLine()) != null) {
                redisService.pushRedisQueue("keywordListTest",str);
                System.out.println(str);
            }
        }catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static void selectRedisDB(){
        System.out.println(redisService.redisQueueSize("keywordListTest"));
        System.out.println("redisService.redisQueueSize(keywordListTest) : " + redisService.redisQueueSize("keywordListTest"));

    }

    private static void deleteRedisDB(){
        redisService.removeRedisValue("keywordListTest");
    }


}
