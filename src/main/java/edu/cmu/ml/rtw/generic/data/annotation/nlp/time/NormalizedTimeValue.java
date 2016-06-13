package edu.cmu.ml.rtw.generic.data.annotation.nlp.time;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import edu.cmu.ml.rtw.generic.data.DataTools;
import edu.cmu.ml.rtw.generic.data.store.StoreReference;
import edu.cmu.ml.rtw.generic.util.Pair;
import edu.cmu.ml.rtw.generic.util.StoredJSONSerializable;

/**
 * 
 * Parses out normalized TimeML times into ranges of dates representing
 * grounded time intervals. 
 * 
 * Dates based on weeks in time expressions are currently only roughly estimated, 
 * although it's possible
 * to represent them precisely.  Possibly fix this in the future.
 * 
 * @author Bill McDowell
 * 
 */
public class NormalizedTimeValue implements StoredJSONSerializable {
	public enum Reference {
		FUTURE,
		PRESENT,
		PAST,
		NONE
	}
	
	public enum Type {
		DATE,
		TIME,
		WEEK_DATE,
		WEEK_TIME,
		SEASON,
		PART_OF_YEAR,
		REFERENCE
	}
	
	/**
	 * TimePattern represents a regex that is used
	 * to parse time expressions.  The TimePattern has a 
	 * regex and a mapping from groups in that regex to
	 * parts of the time (year, month, day, hour, etc). 
	 *
	 * @author Bill McDowell
	 * 
	 */
	protected class TimePattern {
		private Pattern pattern; // regex
		
		// Regex groups for each part of date
		private int yearGroup;
		private int monthGroup;
		private int dayGroup;
		private int hourGroup;
		private int minuteGroup;
		private int secondGroup;
		private int yearPartGroup;
		private int seasonGroup;
		private int weekGroup;
		private int timeOfDayGroup;
		private int weekDayGroup;
		
		public TimePattern(String pattern, 
						 int yearGroup, 
						 int monthGroup, 
						 int dayGroup,
						 int hourGroup,
						 int minuteGroup,
						 int secondGroup,
						 int yearPartGroup,
						 int seasonGroup,
						 int weekGroup,
						 int timeOfDayGroup,
						 int weekDayGroup) {
			this.pattern = Pattern.compile(pattern);
			this.yearGroup = yearGroup; 
			this.monthGroup = monthGroup;
			this.dayGroup = dayGroup;
			this.hourGroup = hourGroup;
			this.minuteGroup = minuteGroup;
			this.secondGroup = secondGroup;
			this.yearPartGroup = yearPartGroup;
			this.seasonGroup = seasonGroup;
			this.weekGroup = weekGroup;
			this.timeOfDayGroup = timeOfDayGroup;
			this.weekDayGroup = weekDayGroup;
		}
		
		public int yearGroup() { return this.yearGroup; }
		public int monthGroup() { return this.monthGroup; }
		public int dayGroup() { return this.dayGroup; }
		public int hourGroup() { return this.hourGroup; }
		public int minuteGroup() { return this.minuteGroup; }
		public int secondGroup() { return this.secondGroup; }
		public int yearPartGroup() { return this.yearPartGroup; }
		public int seasonGroup() { return this.seasonGroup; }
		public int weekGroup() { return this.weekGroup; }
		public int timeOfDayGroup() { return this.timeOfDayGroup; }
		public int weekDayGroup() { return this.weekDayGroup; }
		
		public String getPatternString() {
			return this.pattern.toString();
		}
		
		public Matcher getMatcher(String text) {
			return this.pattern.matcher(text);
		}
		
	}

	/**
	 * Possible time expression regexes with groups mapped
	 * to time parts by their indices.
	 * 
	 * These regexes come from http://www.timeml.org/timeMLdocs/TimeML.xsd which
	 * is linked from http://timeml.org/site/publications/timeMLdocs/timeml_1.2.1.html.
	 *  
	 */
	protected TimePattern DATE_PATTERN = new TimePattern(
  		"([0-9X]{1,4})(-([0-9X]{1,2})(-([0-9X]{1,2}))?)?",
  				1,3,5,0,0,0,0,0,0,0,0);
	protected TimePattern TIME_PATTERN = new TimePattern(
  		"(([0-9X]{1,4})-([0-9X]{1,2})-([0-9X]{1,2}))?T((([0-9]{2})(:([0-9]{2})(:([0-9]{2}))?)?)|(MO|MI|AF|EV|NI|DT))",
  				2,3,4,7,9,11,0,0,0,12,0);
	protected TimePattern WEEK_DATE_PATTERN = new TimePattern(
  		"([0-9X]{1,4})-W([0-9X]{1,2})(-([1-7X]|WE))?",
  				1,0,0,0,0,0,0,0,2,0,4);
	protected TimePattern WEEK_TIME_PATTERN = new TimePattern(
  		"([0-9X]{1,4})-W([0-9X]{1,2})-([1-7X])T((([0-9]{2})(:([0-9]{2})(:([0-9]{2}))?)?)|(MO|MI|AF|EV|NI|DT))",
  				1,0,0,6,8,10,0,0,2,11,3);
	protected TimePattern SEASON_PATTERN = new TimePattern(
  		"([0-9X]{1,4})-(SP|SU|WI|FA)",
  				1,0,0,0,0,0,0,2,0,0,0);
	protected TimePattern PART_OF_YEAR_PATTERN = new TimePattern(
  		"([0-9X]{1,4})-(H[1-2X]|Q[1-4X])",
  				1,0,0,0,0,0,2,0,0,0,0);
	protected TimePattern REFERENCE_PATTERN = new TimePattern("PAST_REF|PRESENT_REF|FUTURE_REF",
			0,0,0,0,0,0,0,0,0,0,0);
	
	protected String id;
	protected String value; // regex string
	protected TimePattern pattern; 
	protected Matcher matcher;
  
	protected StoreReference reference;
	protected DataTools dataTools;
	
	protected List<StoreReference> someExpressionReferences;
	
	public NormalizedTimeValue(DataTools dataTools) {
		this.dataTools = dataTools;
	}
	
	public NormalizedTimeValue(DataTools dataTools, StoreReference reference) {
		this.dataTools = dataTools;
		this.reference = reference;
	}
	
	public NormalizedTimeValue(DataTools dataTools, StoreReference reference, String id, String value, List<StoreReference> someExpressionReferences) {
		this.dataTools = dataTools;
		this.reference = reference;
		this.id = id;
		this.value = value;
		this.someExpressionReferences = someExpressionReferences;
	
		initPattern();
	}
	
	protected void initPattern() {
		if (this.value == null)
			return;
		
		Matcher dateMatcher = this.DATE_PATTERN.getMatcher(this.value);
		Matcher timeMatcher = this.TIME_PATTERN.getMatcher(this.value);
		Matcher weekDateMatcher = this.WEEK_DATE_PATTERN.getMatcher(this.value);
		Matcher weekTimeMatcher = this.WEEK_TIME_PATTERN.getMatcher(this.value);
		Matcher seasonMatcher = this.SEASON_PATTERN.getMatcher(this.value);
		Matcher partOfYearMatcher = this.PART_OF_YEAR_PATTERN.getMatcher(this.value);
		Matcher referenceMatcher = this.REFERENCE_PATTERN.getMatcher(this.value);
		
		/**
		 * Determine the format of the given time expression
		 */
		if (dateMatcher.matches()) {
			this.pattern = this.DATE_PATTERN;
			this.matcher = dateMatcher;			
		} else if (timeMatcher.matches()) {
			this.pattern = this.TIME_PATTERN;
			this.matcher = timeMatcher;				
		} else if (weekDateMatcher.matches()) {
			this.pattern = this.WEEK_DATE_PATTERN;
			this.matcher = weekDateMatcher;	
		} else if (weekTimeMatcher.matches()) {
			this.pattern = this.WEEK_TIME_PATTERN;
			this.matcher = weekTimeMatcher;	
		} else if (seasonMatcher.matches()) {
			this.pattern = this.SEASON_PATTERN;
			this.matcher = seasonMatcher;	
		} else if (partOfYearMatcher.matches()) {
			this.pattern = this.PART_OF_YEAR_PATTERN;
			this.matcher = partOfYearMatcher;	
		} else if (referenceMatcher.matches()) {
			this.pattern = this.REFERENCE_PATTERN;
			this.matcher = referenceMatcher;
		}
	}

	public Type getType() {
		if (this.pattern == null) {
			return null;
		} else if (this.pattern.equals(this.DATE_PATTERN)) {
			return Type.DATE;
		} else if (this.pattern.equals(this.TIME_PATTERN)) {
			return Type.TIME;
		} else if (this.pattern.equals(this.WEEK_DATE_PATTERN)) {
			return Type.WEEK_DATE;
		} else if (this.pattern.equals(this.WEEK_TIME_PATTERN)) {
			return Type.WEEK_TIME;
		} else if (this.pattern.equals(this.SEASON_PATTERN)) {
			return Type.SEASON;
		} else if (this.pattern.equals(this.PART_OF_YEAR_PATTERN)) {
			return Type.PART_OF_YEAR;
		} else if (this.pattern.equals(this.REFERENCE_PATTERN)) {
			return Type.REFERENCE;
		} else {
			return null;
		}
	}
	
	/**
	 * @return a range (start year, end year) referenced by the time
	 * value
	 */
	public Pair<Integer, Integer> getYears() { 
		if (this.pattern == null || this.pattern.yearGroup() == 0)
			return null;

		String yearStr = this.matcher.group(this.pattern.yearGroup());
		if (!isInteger(yearStr))
			return null;
		
		StringBuilder minYear = new StringBuilder().append(yearStr);
		StringBuilder maxYear = new StringBuilder().append(yearStr);
		
		for (int i = yearStr.length(); i < 4; i++) {
			minYear.append("0");
			maxYear.append("9");
		}
		
		return new Pair<Integer,Integer>(Integer.valueOf(minYear.toString()), Integer.valueOf(maxYear.toString()));
	}
	
	/**
	 * @return a range (start month, end month) referenced by the time
	 * value
	 */
	public Pair<Integer, Integer> getMonths() { 
		if (this.pattern == null)
			return null;
		
		if (this.pattern.monthGroup() != 0) {
			Integer month = extractMatcherInteger(this.pattern.monthGroup());
			if (month != null)
				return new Pair<Integer, Integer>(month-1, month-1);
		} else if (this.pattern.weekGroup() != 0) {
			Integer week = extractMatcherInteger(this.pattern.weekGroup());
			if (week != null)
				return new Pair<Integer, Integer>(weekToMonth(week)-1, weekToMonth(week)-1);
		} else if (this.pattern.seasonGroup() != 0) {
			String season = this.matcher.group(this.pattern.seasonGroup());
			if (season.equals("SP")) {
				return new Pair<Integer, Integer>(2, 5);
			} else if (season.equals("SU")) {
				return new Pair<Integer, Integer>(5, 8);			
			} else if (season.equals("FA")) {
				return new Pair<Integer, Integer>(8, 11);			
			} else if (season.equals("WI")) {
				return new Pair<Integer, Integer>(11, 2);			
			}
		} else if (this.pattern.yearPartGroup() != 0) {
			String yearPart = this.matcher.group(this.pattern.yearPartGroup());
			if (yearPart.equals("H1")) {
				return new Pair<Integer, Integer>(0, 5);
			} else if (yearPart.equals("H2")) {
				return new Pair<Integer, Integer>(6, 11);
			} else if (yearPart.equals("Q1")) {
				return new Pair<Integer, Integer>(0, 2);
			} else if (yearPart.equals("Q2")) {
				return new Pair<Integer, Integer>(3, 5);				
			} else if (yearPart.equals("Q3")) {
				return new Pair<Integer, Integer>(6, 8);			
			} else if (yearPart.equals("Q4")) {
				return new Pair<Integer, Integer>(9, 11);			
			}
		}
		
		return null;
	}
	
	/**
	 * @return a range (start day, end day) referenced by the time
	 * value
	 */
	public Pair<Integer, Integer> getDays() { 
		/* FIXME: Add support to find day based on week and week day */
		if (this.pattern == null)
			return null;
		
		if (this.pattern.dayGroup() != 0) {
			Integer day = extractMatcherInteger(this.pattern.dayGroup());
			if (day != null)
				return new Pair<Integer, Integer>(day, day);
		} else if (this.pattern.seasonGroup() != 0) {
			String season = this.matcher.group(this.pattern.seasonGroup());
			if (season.equals("SP")) {
				return new Pair<Integer, Integer>(20,20); /* March 20 - June 20 */
			} else if (season.equals("SU")) {
				return new Pair<Integer, Integer>(21,21); /* June 21 - September 21 */
			} else if (season.equals("FA")) {
				return new Pair<Integer, Integer>(22,20); /* September 22 - December 20 */			
			} else if (season.equals("WI")) {
				return new Pair<Integer, Integer>(21,19); /* December 21 - March 19 */			
			}
		}
		
		return null;
	}

	/**
	 * @return a range (start hour, end hour) referenced by the time
	 * value
	 */
	public Pair<Integer, Integer> getHours() {
		if (this.pattern == null)
			return null;
		
		if (this.pattern.hourGroup() != 0) {
			Integer hour = extractMatcherInteger(this.pattern.hourGroup());
			if (hour != null)
				return new Pair<Integer, Integer>(hour, hour);
		} 
		
		if (this.pattern.timeOfDayGroup() != 0) {
			String timeOfDay = this.matcher.group(this.pattern.timeOfDayGroup());
			if (timeOfDay.equals("MO")) {
				return new Pair<Integer, Integer>(4,7);
			} else if (timeOfDay.equals("MI")) {
				return new Pair<Integer, Integer>(8,11);				
			} else if (timeOfDay.equals("AF")) {
				return new Pair<Integer, Integer>(12,15);				
			} else if (timeOfDay.equals("EV")) {
				return new Pair<Integer, Integer>(16,19);				
			} else if (timeOfDay.equals("NI")) {
				return new Pair<Integer, Integer>(20,23);				
			} else if (timeOfDay.equals("DT")) {
				return new Pair<Integer, Integer>(0,3);
			}
		}
		
		return null;
	}
	
	/**
	 * @return a range (start minute, end minute) referenced by the time
	 * value
	 */
	public Pair<Integer, Integer> getMinutes() {
		if (this.pattern == null)
			return null;
		
		if (this.pattern.minuteGroup() != 0) {
			Integer minute = extractMatcherInteger(this.pattern.minuteGroup());
			if (minute != null)
				return new Pair<Integer, Integer>(minute, minute);
		}
		
		return null;
	}
	
	/**
	 * @return a range (start second, end second) referenced by the time
	 * value
	 */
	public Pair<Integer, Integer> getSeconds() { 
		if (this.pattern == null)
			return null;
		
		if (this.pattern.secondGroup() != 0) {
			Integer second = extractMatcherInteger(this.pattern.secondGroup());
			if (second != null)
				return new Pair<Integer, Integer>(second, second);
		}
		
		return null;
	}
	
	/**
	 * @return a start and end time for the interval referenced by the
	 * time expression
	 */
	public Pair<Calendar, Calendar> getRange() {

		Pair<Integer, Integer> years = getYears();
		Pair<Integer, Integer> months = getMonths();
		Pair<Integer, Integer> days = getDays();
		Pair<Integer, Integer> hours = getHours();
		Pair<Integer, Integer> minutes = getMinutes();
		Pair<Integer, Integer> seconds = getSeconds();
		

		/* DEBUG: System.out.println("Value: " + this.value + " Years: " + years + 
				" Months: " + months + 
				" Days: " + days + 
				" Hours: " + hours + 
				" Minutes: " + minutes +
				" Seconds: " + seconds +
				" Pattern: " + this.pattern.getPatternString());*/
		
		if (years == null) {
			return null;
		}
		
		Calendar minTime = Calendar.getInstance();
		minTime.set(Calendar.MILLISECOND, 0);
		minTime.set(
						years.getFirst(), 
						months == null ? 1 : months.getFirst(), 
						days == null ? 1 : days.getFirst(), 
						hours == null ? 0 : hours.getFirst(), 
						minutes == null ? 0 : minutes.getFirst(), 
						seconds == null ? 0 : seconds.getFirst());
		
		Calendar maxTime = Calendar.getInstance();
		maxTime.set(Calendar.MILLISECOND, 0);
		maxTime.set(
				years.getSecond(), 
				months == null ? maxTime.getLeastMaximum(Calendar.MONTH) : months.getSecond(), 
				days == null ?  maxTime.getLeastMaximum(Calendar.DAY_OF_MONTH) : days.getSecond(), 
				hours == null ?  maxTime.getLeastMaximum(Calendar.HOUR_OF_DAY) : hours.getSecond(), 
				minutes == null ?  maxTime.getLeastMaximum(Calendar.MINUTE) : minutes.getSecond(), 
				seconds == null ?  maxTime.getLeastMaximum(Calendar.SECOND) : seconds.getSecond());

		return new Pair<Calendar, Calendar>(minTime, maxTime);
	}
	
	public Reference getReference() {
		if (this.value == null)
			return Reference.NONE;
		else if (this.value.equals("FUTURE_REF"))
			return Reference.FUTURE;
		else if (this.value.equals("PRESENT_REF"))
			return Reference.PRESENT;
		else if (this.value.equals("PAST_REF"))
			return Reference.PAST;
		else
			return Reference.NONE;
	}
	
	public NormalizedTimeValue toDate() {			
  		if (this.value == null)
  			return this; // FIXME?
		if(this.value.matches("^\\d\\d\\d\\d-\\d\\d-\\d\\d.+") ) {
  			return new NormalizedTimeValue(this.dataTools, this.reference, this.getId(), this.value.substring(0, 10), this.someExpressionReferences);
  		} else {
  			return null;
  		}
	}
	
	public String toString() {
		return this.value;
	}
	
	protected int weekToMonth(int week) {
		return (int)Math.ceil(Math.min(12, week/4.33)); // FIXME: This is just a rough estimate
	}
	
	protected Integer extractMatcherInteger(int group) {
		String str = this.matcher.group(group);
		if (str == null || !isInteger(str))
			return null;
		else
			return Integer.valueOf(str);
	}
	
	protected boolean isInteger(String str) {
		if (str == null)
			return false;
		
		for (int i = 0; i < str.length(); i++)
			if (!Character.isDigit(str.charAt(i)))
				return false;
		return true;
	}

	@Override
	public StoredJSONSerializable makeInstance(StoreReference reference) {
		return new NormalizedTimeValue(this.dataTools, reference);
	}

	@Override
	public StoreReference getStoreReference() {
		return this.reference;
	}

	@Override
	public JSONObject toJSON() {
		JSONObject json = new JSONObject();
		
		try {
			if (this.id != null)
				json.put("id", this.id);
			if (this.value != null)
				json.put("name", this.value);
			if (this.someExpressionReferences != null) {
				JSONArray expRefs = new JSONArray();
				for (int i = 0; i < this.someExpressionReferences.size(); i++)
					expRefs.put(this.someExpressionReferences.get(i).toJSON());
				json.put("someExprs", expRefs);
			}
		} catch (JSONException e) {
			return null;
		}
		
		return json;
	}

	@Override
	public boolean fromJSON(JSONObject json) {
		try {
			if (json.has("id"))
				this.id = json.getString("id");
			if (json.has("name"))
				this.value = json.getString("name");
			if (json.has("someExprs")) {
				this.someExpressionReferences = new ArrayList<>();
				JSONArray expRefs = json.getJSONArray("someExprs");
				for (int i = 0; i < expRefs.length(); i++)
					this.someExpressionReferences.add(StoreReference.makeFromJSON(expRefs.getJSONObject(i)));
			}
		} catch (JSONException e) {
			return false;
		}
		
		initPattern();
		
		return true;
	}

	@Override
	public String getId() {
		return this.id;
	}
	
	public String getValue() {
		return this.value;
	}
	
	public int getSomeExpressionCount() {
		return this.someExpressionReferences.size();
	}
	
	public TimeExpression getSomeExpression(int index) {
		return this.dataTools.getStoredItemSetManager().resolveStoreReference(this.someExpressionReferences.get(index), true);
	}
}

