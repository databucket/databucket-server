package pl.databucket;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import pl.databucket.service.DatabucketServiceIm;

@SpringBootApplication
public class DatabucketApplication {
	
	public static void main(String[] args) {
		Logger logger = LoggerFactory.getLogger(DatabucketServiceIm.class);
		try {			
			SpringApplication.run(DatabucketApplication.class, args);
		} catch (Exception e) {
			logger.info("\n\n\n--------------------------------------------------------------------");
			logger.error("Problem with starting Databucket application. Check if the database server is started.");
			logger.info("--------------------------------------------------------------------");
		}
	}
}
