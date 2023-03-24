import java.io.BufferedReader;
import java.io.FileReader;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Date;
import java.util.Locale;


public class tester {

	public static void main(String[] args) {
		try {
			BufferedReader reader = new BufferedReader(
					
					 new FileReader("../instagram-keywotd-recrawler/src/main/resources/data/keywordList_1.txt"), // Read file
					 16 * 1024				 
			 );
			
			String str;
			 while((str = reader.readLine()) != null) {
				 
			 }
		}catch (Exception e) {
			e.printStackTrace();
		}


	}

}
