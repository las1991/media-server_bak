package com.sengled.cloud.bootstrap.jdbc.device;

public class TestDeviceInfo   {
/**
	public void testDeviceTimeZone() throws ParseException {
		SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
		Date beijingTime = format.parse("2016-10-19 19:00:00");
		String losAngelesTime = "2016-10-19 04:00:00";
		
		DeviceInfo info = new DeviceInfo(0, null, null, null, "America/Los_Angeles");
		Timestamp deviceTimesamp = info.getDeviceTime(beijingTime.getTime());
		System.out.println(format.format(deviceTimesamp));

		assertEquals(format.parse(losAngelesTime).getTime(), deviceTimesamp.getTime());

		
		for (String id : TimeZone.getAvailableIDs()) {
			System.out.println(id); 
		}
		
		info = new DeviceInfo(0, null, null, null, null);
	}
	*/
}
