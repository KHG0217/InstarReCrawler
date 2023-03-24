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
//        insertRedisDB();
//        deleteRedisDB();
        selectRedisDB();
        popRedisDB();




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
                redisService.pushRedisQueue("keywordList",str);
                System.out.println(str);
            }
        }catch (Exception e) {
            e.printStackTrace();
        }

    }

    private static void selectRedisDB(){
        System.out.println(redisService.redisQueueSize("keywordList"));

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
        redisService.removeRedisValue("keywordList");
    }

    private static void popRedisDB(){
        for(int i = 0 ; i < redisService.redisQueueSize("keywordList"); i++){
            System.out.println(redisService.popRedisQueue("keywordList"));
        }


    }

}
