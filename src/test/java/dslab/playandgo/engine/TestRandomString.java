package dslab.playandgo.engine;

import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoField;
import java.util.Date;

import org.apache.commons.lang3.RandomStringUtils;
import org.junit.Test;

public class TestRandomString {
	
	@Test
	public void generateString() {
	    int length = 10;
	    boolean useLetters = true;
	    boolean useNumbers = true;
	    String generatedString = RandomStringUtils.random(length, useLetters, useNumbers);
	    System.out.println(generatedString);
	}
	
	@Test
	public void weekOfYear() {
	    DateTimeFormatter dtf = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
	    DateTimeFormatter WEEK_PATTERN = DateTimeFormatter.ofPattern("YYYY-ww");
	    
	    //2022-01-01 00:00 UTC
	    Date date = new Date(1640995200000L);
	    ZonedDateTime zonedDateTime = ZonedDateTime.ofInstant(date.toInstant(), ZoneId.of("Europe/Rome"));
	    System.out.println("date:" + dtf.format(zonedDateTime));
	    System.out.println("weekOfYear:" + zonedDateTime.get(ChronoField.ALIGNED_WEEK_OF_YEAR));
	    System.out.println("weekOfYear:" + WEEK_PATTERN.format(zonedDateTime));
	    
        //2022-01-08 07:00 UTC
        date = new Date(1641625200000L);
        zonedDateTime = ZonedDateTime.ofInstant(date.toInstant(), ZoneId.of("Europe/Rome"));
        System.out.println("date:" + dtf.format(zonedDateTime));
        System.out.println("weekOfYear:" + zonedDateTime.get(ChronoField.ALIGNED_WEEK_OF_YEAR));
        System.out.println("weekOfYear:" + WEEK_PATTERN.format(zonedDateTime));
        
	    //2022-10-26 08:00 UTC
	    date = new Date(1666771200000L);
	    zonedDateTime = ZonedDateTime.ofInstant(date.toInstant(), ZoneId.of("Europe/Rome"));
	    System.out.println("date:" + dtf.format(zonedDateTime));
	    System.out.println("weekOfYear:" + zonedDateTime.get(ChronoField.ALIGNED_WEEK_OF_YEAR));
	    System.out.println("weekOfYear:" + WEEK_PATTERN.format(zonedDateTime));
	}
}
