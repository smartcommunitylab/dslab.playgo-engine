package dslab.playandgo.engine;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.temporal.TemporalAdjusters;

import org.junit.Test;

public class TestDayOfWeek {

	@Test
	public void testDayOfWeek() {
		LocalDate day = LocalDate.parse("2022-03-24");
		LocalDate dayOfWeek = day.with(TemporalAdjusters.previousOrSame(DayOfWeek.of(1)));
		System.out.println(dayOfWeek);
		dayOfWeek = day.with(TemporalAdjusters.previousOrSame(DayOfWeek.of(5)));
		System.out.println(dayOfWeek);
	}
}
