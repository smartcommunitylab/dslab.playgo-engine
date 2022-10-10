package dslab.playandgo.engine;

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
}
