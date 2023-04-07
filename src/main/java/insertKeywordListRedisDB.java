import com.tapacross.sns.service.RedisService;
import org.springframework.context.support.GenericApplicationContext;
import org.springframework.context.support.GenericXmlApplicationContext;

import java.io.BufferedReader;
import java.io.FileReader;
import java.util.ArrayList;
import java.util.List;

public class insertKeywordListRedisDB {
    private static GenericXmlApplicationContext applicationContext = new GenericXmlApplicationContext(
            "classpath:spring/application-context.xml");
    private static RedisService redisService = applicationContext.getBean(RedisService.class);
    public static void main(String[] args) {
        deleteRedisDB();
        insertRedisDB();
//        selectRedisDB();
//        popRedisDB();




    }

    private static void  insertRedisDB(){
        List<String> keywordList = new ArrayList<String>();

        try {
            BufferedReader reader = new BufferedReader(
                    new FileReader("../instagram-keyword-recrawler/src/main/resources/data/keywordList_3.txt"), // Read file
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
//        try {
//            BufferedReader reader = new BufferedReader(
//                    new FileReader("../instagram-keyword-recrawler/src/main/resources/data/keywordList_1.txt"), // Read file
//                    16 * 1024
//            );
//            String str;
//            while((str = reader.readLine()) != null) {
//                redisService.removeRedisValue(str);
//                System.out.println(str);
//            }
//        }catch (Exception e) {
//            e.printStackTrace();
//        }
        redisService.removeRedisValue("keywordListTest");
    }

    private static void popRedisDB(){
        int count = 0;
        Long listSize = redisService.redisQueueSize("keywordList");

        System.out.println("ListSize : " + listSize);
        for(int i = 0 ; i < listSize; i++){
            count ++;
            System.out.println(redisService.popRedisQueue("keywordList"));
        }
        System.out.println("count : " + count);


    }

}
