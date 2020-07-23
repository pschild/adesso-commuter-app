package de.pschild.adessocommutingnotifier;

import static org.junit.Assert.assertEquals;

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
    // schedule alarm to 2:00
    PowerMockito.stub(PowerMockito.method(Scheduler.class, "now")).toReturn(mockNow(0, 0));
    assertEquals("0:00 => 2:00", "02:00:00", new SimpleDateFormat("HH:mm:ss").format(Scheduler.calculateNextAlarm().getTime()));

    PowerMockito.stub(PowerMockito.method(Scheduler.class, "now")).toReturn(mockNow(1, 59));
    assertEquals("1:59 => 2:00", "02:00:00", new SimpleDateFormat("HH:mm:ss").format(Scheduler.calculateNextAlarm().getTime()));

    PowerMockito.stub(PowerMockito.method(Scheduler.class, "now")).toReturn(mockNow(15, 0));
    assertEquals("15:00 => 2:00", "02:00:00", new SimpleDateFormat("HH:mm:ss").format(Scheduler.calculateNextAlarm().getTime()));

    PowerMockito.stub(PowerMockito.method(Scheduler.class, "now")).toReturn(mockNow(15, 45));
    assertEquals("15:45 => 2:00", "02:00:00", new SimpleDateFormat("HH:mm:ss").format(Scheduler.calculateNextAlarm().getTime()));

    PowerMockito.stub(PowerMockito.method(Scheduler.class, "now")).toReturn(mockNow(22, 0));
    assertEquals("22:00 => 2:00", "02:00:00", new SimpleDateFormat("HH:mm:ss").format(Scheduler.calculateNextAlarm().getTime()));

    // schedule alarm to 5:30
    PowerMockito.stub(PowerMockito.method(Scheduler.class, "now")).toReturn(mockNow(2, 0));
    assertEquals("2:00 => 5:30", "05:30:00", new SimpleDateFormat("HH:mm:ss").format(Scheduler.calculateNextAlarm().getTime()));

    PowerMockito.stub(PowerMockito.method(Scheduler.class, "now")).toReturn(mockNow(2, 30));
    assertEquals("2:30 => 5:30", "05:30:00", new SimpleDateFormat("HH:mm:ss").format(Scheduler.calculateNextAlarm().getTime()));

    PowerMockito.stub(PowerMockito.method(Scheduler.class, "now")).toReturn(mockNow(5, 0));
    assertEquals("5:00 => 5:30", "05:30:00", new SimpleDateFormat("HH:mm:ss").format(Scheduler.calculateNextAlarm().getTime()));

    PowerMockito.stub(PowerMockito.method(Scheduler.class, "now")).toReturn(mockNow(5, 29));
    assertEquals("5:29 => 5:30", "05:30:00", new SimpleDateFormat("HH:mm:ss").format(Scheduler.calculateNextAlarm().getTime()));

    // schedule alarm to 15:00
    PowerMockito.stub(PowerMockito.method(Scheduler.class, "now")).toReturn(mockNow(11, 0));
    assertEquals("11:00 => 15:00", "15:00:00", new SimpleDateFormat("HH:mm:ss").format(Scheduler.calculateNextAlarm().getTime()));

    PowerMockito.stub(PowerMockito.method(Scheduler.class, "now")).toReturn(mockNow(14, 59));
    assertEquals("14:59 => 15:00", "15:00:00", new SimpleDateFormat("HH:mm:ss").format(Scheduler.calculateNextAlarm().getTime()));

    PowerMockito.stub(PowerMockito.method(Scheduler.class, "now")).toReturn(mockNow(5, 30));
    assertEquals("05:30 => 15:00", "15:00:00", new SimpleDateFormat("HH:mm:ss").format(Scheduler.calculateNextAlarm().getTime()));

    PowerMockito.stub(PowerMockito.method(Scheduler.class, "now")).toReturn(mockNow(5, 45));
    assertEquals("05:45 => 15:00", "15:00:00", new SimpleDateFormat("HH:mm:ss").format(Scheduler.calculateNextAlarm().getTime()));
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
