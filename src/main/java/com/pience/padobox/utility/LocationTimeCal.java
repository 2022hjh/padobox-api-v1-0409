package com.pience.padobox.utility;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.Instant;
import java.time.LocalDateTime;
//import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Calendar;
import java.util.Date;

public class LocationTimeCal {
	
	public static String CalDatePlus(String date_val, Integer cal_time) {
		String return_date = "";
		SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'");
		format.setTimeZone(java.util.TimeZone.getTimeZone("UTC"));
		Date date = new Date();
		try {
			date = format.parse(date_val);
		} catch (ParseException e) {
			e.printStackTrace();
		}
		Calendar cal = Calendar.getInstance();
		cal.setTime(date);
		cal.add(Calendar.HOUR, -cal_time);
		return_date = format.format(cal.getTime());
		return return_date;
	}
	
	public static String SetDateCal(Integer cal_type, Integer plus_day) {
		String return_date = "";
		SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd");
		format.setTimeZone(java.util.TimeZone.getTimeZone("UTC"));
		Date date = new Date();
		Calendar cal = Calendar.getInstance();
		cal.setTime(date);
		cal.add(Calendar.DAY_OF_MONTH, +plus_day);
		return_date = format.format(cal.getTime());
		return return_date;
	}
	
	public static String SetDateStart() {
		String return_date = "";
		SimpleDateFormat format = new SimpleDateFormat("yyyy-MM");
		format.setTimeZone(java.util.TimeZone.getTimeZone("UTC"));
		Date date = new Date();
		Calendar cal = Calendar.getInstance();
		cal.setTime(date);
		return_date = format.format(cal.getTime());
		return return_date;
	}	
	
	public static String SetToDay() {
		String return_date = "";
		SimpleDateFormat format = new SimpleDateFormat("yyyyMMdd");
		format.setTimeZone(java.util.TimeZone.getTimeZone("UTC"));
		Date date = new Date();
		Calendar cal = Calendar.getInstance();
		cal.setTime(date);
		return_date = format.format(cal.getTime());
		return return_date;
	}	
	
	
	public static String TimeStamptoDate(Long unixTime) {
		String return_date = "";
        Date date = new Date(unixTime);
        SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'");
        format.setTimeZone(java.util.TimeZone.getTimeZone("UTC"));
        Calendar cal = Calendar.getInstance();
		cal.setTime(date);
        return_date= format.format(cal.getTime());
        return return_date;
    }
	
	
	public static Long DatetoTimeStamp(String date_val) {
		Long unixTime = 0L;
		SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'");
		format.setTimeZone(java.util.TimeZone.getTimeZone("UTC"));
        try {
            unixTime = format.parse(date_val).getTime();
            unixTime = unixTime / 1;
        } catch (ParseException e) {
            e.printStackTrace();
        }
        return unixTime;
    }
	
	public static Long UnixTimeStampCal(int cal_type, String cal_timestamp) {
		LocalDateTime now = LocalDateTime.now();
		if(EmptyUtils.isEmpty(cal_timestamp)==false) {
			DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'");
			now = 	LocalDateTime.parse(cal_timestamp, formatter);
		}
		if(cal_type==1) {
			now = now.plusDays(-7);
		}else if(cal_type==2) {
			now = now.plusDays(7);
		}else if(cal_type==3) {
			now = now.plusDays(-140);
		}
		Long TodayTimeStamp = now.toInstant(ZoneOffset.of("+09:00")).toEpochMilli();
		return TodayTimeStamp;
	}
	
	public static Long UnixTimeStampCalAccountList(int cal_type, String cal_timestamp) {
		LocalDateTime now = LocalDateTime.now(ZoneOffset.UTC);
		if(EmptyUtils.isEmpty(cal_timestamp)==false) {
			DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'");
			now = 	LocalDateTime.parse(cal_timestamp, formatter);
		}
		if(cal_type==0) {
			now = now.plusHours(-1);
		}else if(cal_type==1) {
			now = now.plusDays(-7);
		}else if(cal_type==2) {
			now = now.plusDays(7);
		}else if(cal_type==3) {
			now = now.plusDays(-70);
		}
		Long TodayTimeStamp = toEpochMilliUTC(now);
		return TodayTimeStamp;
	}

	public static Long toEpochMilliUTC(LocalDateTime localDateTime) {
        ZonedDateTime zonedDateTime = localDateTime.atZone(ZoneOffset.UTC);
        Instant instant = zonedDateTime.toInstant();
        return instant.toEpochMilli();
    }
	
	public static String GetNowDateTime() {
		
		String nowDateTime = "";
		Date date = new Date();
		SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'");
		format.setTimeZone(java.util.TimeZone.getTimeZone("Asia/Seoul"));
    	Calendar cal = Calendar.getInstance();
		cal.setTime(date);
		nowDateTime= format.format(cal.getTime());
        return nowDateTime;
    }

}
