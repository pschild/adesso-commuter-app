package de.pschild.adessocommutingnotifier;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

@RunWith(PowerMockRunner.class)
@PrepareForTest(Scheduler.class)
public class SchedulerTest {

  @Test
  public void calculateNextAlarm() {
    PowerMockito.stub(PowerMockito.method(Scheduler.class, "getTriggerTimes")).toReturn(new String[]{"15:00", "05:30"});

    PowerMockito.stub(PowerMockito.method(Scheduler.class, "now")).toReturn(mockNow(2, 0));
    assertEquals("2:00 => 5:30", "05:30:00", new SimpleDateFormat("HH:mm:ss").format(Scheduler.calculateNextAlarm().getTime()));
    assertEquals("same day", mockNow(2, 0).get(Calendar.DAY_OF_WEEK), Scheduler.calculateNextAlarm().get(Calendar.DAY_OF_WEEK));

    PowerMockito.stub(PowerMockito.method(Scheduler.class, "now")).toReturn(mockNow(5, 0));
    assertEquals("5:00 => 5:30", "05:30:00", new SimpleDateFormat("HH:mm:ss").format(Scheduler.calculateNextAlarm().getTime()));
    assertEquals("same day", mockNow(5, 0).get(Calendar.DAY_OF_WEEK), Scheduler.calculateNextAlarm().get(Calendar.DAY_OF_WEEK));

    PowerMockito.stub(PowerMockito.method(Scheduler.class, "now")).toReturn(mockNow(5, 10));
    assertEquals("5:10 => 5:30", "05:30:00", new SimpleDateFormat("HH:mm:ss").format(Scheduler.calculateNextAlarm().getTime()));
    assertEquals("same day", mockNow(5, 10).get(Calendar.DAY_OF_WEEK), Scheduler.calculateNextAlarm().get(Calendar.DAY_OF_WEEK));

    PowerMockito.stub(PowerMockito.method(Scheduler.class, "now")).toReturn(mockNow(5, 30));
    assertEquals("5:30 => 15:00", "15:00:00", new SimpleDateFormat("HH:mm:ss").format(Scheduler.calculateNextAlarm().getTime()));
    assertEquals("same day", mockNow(5, 30).get(Calendar.DAY_OF_WEEK), Scheduler.calculateNextAlarm().get(Calendar.DAY_OF_WEEK));

    PowerMockito.stub(PowerMockito.method(Scheduler.class, "now")).toReturn(mockNow(5, 35));
    assertEquals("5:35 => 15:00", "15:00:00", new SimpleDateFormat("HH:mm:ss").format(Scheduler.calculateNextAlarm().getTime()));
    assertEquals("same day", mockNow(5, 35).get(Calendar.DAY_OF_WEEK), Scheduler.calculateNextAlarm().get(Calendar.DAY_OF_WEEK));

    PowerMockito.stub(PowerMockito.method(Scheduler.class, "now")).toReturn(mockNow(12, 34));
    assertEquals("12:34 => 15:00", "15:00:00", new SimpleDateFormat("HH:mm:ss").format(Scheduler.calculateNextAlarm().getTime()));
    assertEquals("same day", mockNow(12, 34).get(Calendar.DAY_OF_WEEK), Scheduler.calculateNextAlarm().get(Calendar.DAY_OF_WEEK));

    PowerMockito.stub(PowerMockito.method(Scheduler.class, "now")).toReturn(mockNow(15, 0));
    assertEquals("15:00 => 5:30", "05:30:00", new SimpleDateFormat("HH:mm:ss").format(Scheduler.calculateNextAlarm().getTime()));
    assertNotEquals("next day", mockNow(15, 0).get(Calendar.DAY_OF_WEEK), Scheduler.calculateNextAlarm().get(Calendar.DAY_OF_WEEK));

    PowerMockito.stub(PowerMockito.method(Scheduler.class, "now")).toReturn(mockNow(22, 22));
    assertEquals("22:22 => 5:30", "05:30:00", new SimpleDateFormat("HH:mm:ss").format(Scheduler.calculateNextAlarm().getTime()));
    assertNotEquals("next day", mockNow(22, 22).get(Calendar.DAY_OF_WEEK), Scheduler.calculateNextAlarm().get(Calendar.DAY_OF_WEEK));
  }

  private Calendar mockNow(int hour, int minute) {
    Calendar fakeNow = Calendar.getInstance();
    fakeNow.set(Calendar.HOUR_OF_DAY, hour);
    fakeNow.set(Calendar.MINUTE, minute);
    fakeNow.set(Calendar.SECOND, 0);
    fakeNow.set(Calendar.MILLISECOND, 0);
    return fakeNow;
  }
}
