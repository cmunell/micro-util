package edu.cmu.ml.rtw.generic.data.annotation.nlp.time;

import org.json.JSONException;
import org.json.JSONObject;

import edu.cmu.ml.rtw.generic.data.DataTools;
import edu.cmu.ml.rtw.generic.data.annotation.nlp.TokenSpan;
import edu.cmu.ml.rtw.generic.data.annotation.nlp.TokenSpan.SerializationType;
import edu.cmu.ml.rtw.generic.data.store.StoreReference;
import edu.cmu.ml.rtw.generic.util.StoredJSONSerializable;

/**
 * 
 * TimeExpression represents a TimeML Timex (temporal expression).
 * 
 * See http://timeml.org/site/index.html for details.
 * 
 * @author Bill McDowell
 * 
 */
public class TimeExpression implements StoredJSONSerializable {
	protected static boolean FORCE_DATE_DCT = true;
	
	public enum TimeMLType {
		DATE,
		TIME,
		DURATION,
		SET
	}
	
	public enum TimeMLDocumentFunction {
		CREATION_TIME,
		EXPIRATION_TIME,
		MODIFICATION_TIME,
		PUBLICATION_TIME,
		RELEASE_TIME,
		RECEPTION_TIME,
		NONE
	}
	
	public enum TimeMLMod {
		BEFORE,
		AFTER,
		ON_OR_BEFORE,
		ON_OR_AFTER,
		LESS_THAN,
		MORE_THAN,
		EQUAL_OR_LESS,
		EQUAL_OR_MORE,
		START,
		MID,
		END,
		APPROX
	}
	
	protected TokenSpan tokenSpan;
	
	protected String id;
	protected String sourceId;
	protected TimeMLType timeMLType;
	protected StoreReference startTimeReference;
	protected StoreReference endTimeReference;
	protected String quant;
	protected String freq;
	protected StoreReference valueReference;
	protected TimeMLDocumentFunction timeMLDocumentFunction = TimeMLDocumentFunction.NONE;
	protected boolean temporalFunction;
	protected StoreReference anchorTimeReference;
	protected StoreReference valueFromFunctionReference;
	protected TimeMLMod timeMLMod;
	
	protected StoreReference reference;
	protected DataTools dataTools;
	
	public TimeExpression(DataTools dataTools) {
		this.dataTools = dataTools;
	}
	
	public TimeExpression(DataTools dataTools, StoreReference reference) {
		this.dataTools = dataTools;
		this.reference = reference;
	}
	
	public TimeExpression(DataTools dataTools, 
						  StoreReference reference,
						  TokenSpan tokenSpan,
						  String id,
						  String sourceId,
						  TimeMLType timeMLType,
						  StoreReference startTimeReference,
						  StoreReference endTimeReference,
						  String quant,
						  String freq,
						  StoreReference valueReference,
						  TimeMLDocumentFunction timeMLDocumentFunction,
						  boolean temporalFunction,
						  StoreReference anchorTimeReference,
						  StoreReference valueFromFunctionReference,
						  TimeMLMod timeMLMod) {
		this.dataTools = dataTools;
		this.reference = reference;
		this.tokenSpan = tokenSpan;
		this.id = id;
		this.sourceId = sourceId;
		this.timeMLType = timeMLType;
		this.startTimeReference = startTimeReference;
		this.endTimeReference = endTimeReference;
		this.quant = quant;
		this.freq = freq;
		this.timeMLDocumentFunction = timeMLDocumentFunction;
		this.temporalFunction = temporalFunction;
		this.anchorTimeReference = anchorTimeReference;
		this.valueFromFunctionReference = valueFromFunctionReference;
		this.timeMLMod = timeMLMod;
		this.valueReference = valueReference;
	}
	
	public TokenSpan getTokenSpan() {
		return this.tokenSpan;
	}
	
	public String getId() {
		return this.id;
	}
	
	public String getSourceId() {
		return this.sourceId;
	}
	
	public TimeMLType getTimeMLType() {
		return this.timeMLType;
	}
	
	public TimeExpression getStartTime() {
		return this.dataTools.getStoredItemSetManager().resolveStoreReference(this.startTimeReference, true);
	}
	
	public TimeExpression getEndTime() {
		return this.dataTools.getStoredItemSetManager().resolveStoreReference(this.endTimeReference, true);
	}
	
	public String getQuant() {
		return this.quant;
	}
	
	public String getFreq() {
		return this.freq;
	}
	
	/**
	 * @return a NormalizedTimeValue representing the 
	 * grounded time-interval referenced by the Time
	 * expression
	 */
	public NormalizedTimeValue getValue() {
		NormalizedTimeValue value = this.dataTools.getStoredItemSetManager().resolveStoreReference(this.valueReference, true);
		
		if (this.timeMLDocumentFunction == TimeMLDocumentFunction.CREATION_TIME) {
			if (FORCE_DATE_DCT) {
				NormalizedTimeValue dateValue = value.toDate();
				if (dateValue != null)
					value = dateValue;
			}
		} 
		
		return value;
	}
	
	public TimeMLDocumentFunction getTimeMLDocumentFunction() {
		return this.timeMLDocumentFunction;
	}
	
	public boolean getTemporalFunction() {
		return this.temporalFunction;
	}
	
	public TimeExpression getAnchorTime() {
		return this.dataTools.getStoredItemSetManager().resolveStoreReference(this.anchorTimeReference, true);
	}
	
	public TimeExpression getValueFromFunction() {
		return this.dataTools.getStoredItemSetManager().resolveStoreReference(this.valueFromFunctionReference, true);
	}
	
	public TimeMLMod getTimeMLMod() {
		return this.timeMLMod;
	}
	
	@Override
	public JSONObject toJSON() {
		JSONObject json = new JSONObject();
		
		try {
			if (this.id != null)
				json.put("id", this.id);
			if (this.sourceId != null)
				json.put("sourceId", this.sourceId);
			if (this.tokenSpan != null)
				json.put("tokenSpan", this.tokenSpan.toJSON(SerializationType.STORE_REFERENCE));
			if (this.timeMLType != null)
				json.put("timeMLType", this.timeMLType.toString());
			if (this.startTimeReference != null)
				json.put("startTime", this.startTimeReference.toJSON());
			if (this.endTimeReference != null)
				json.put("endTime", this.endTimeReference.toJSON());	
			if (this.freq != null)
				json.put("freq", this.freq);
			if (this.valueReference != null)
				json.put("valueRef", this.valueReference.toJSON());
			if (this.quant != null)
				json.put("quant", this.quant);
			if (this.timeMLDocumentFunction != null)
				json.put("timeMLDocumentFunction", this.timeMLDocumentFunction.toString());
			
			json.put("temporalFunction", this.temporalFunction);
			
			if (this.anchorTimeReference != null)
				json.put("anchorTime", this.anchorTimeReference.toJSON());
			if (this.valueFromFunctionReference != null)
				json.put("valueFromFunction", this.valueFromFunctionReference.toJSON());
			if (this.timeMLMod != null)
				json.put("timeMLMod", this.timeMLMod.toString());
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
			if (json.has("sourceId"))
				this.sourceId = json.getString("sourceId");
			if (json.has("tokenSpan"))
				this.tokenSpan = TokenSpan.fromJSON(json.getJSONObject("tokenSpan"), this.dataTools.getStoredItemSetManager());
			if (json.has("timeMLType"))
				this.timeMLType = TimeMLType.valueOf(json.getString("timeMLType"));
			if (json.has("startTime"))
				this.startTimeReference = StoreReference.makeFromJSON(json.getJSONObject("startTime"));
			if (json.has("endTime"))
				this.endTimeReference = StoreReference.makeFromJSON(json.getJSONObject("endTime"));
			if (json.has("freq"))
				this.freq = json.getString("freq");
			if (json.has("quant"))
				this.quant = json.getString("quant");
			if (json.has("timeMLDocumentFunction"))
				this.timeMLDocumentFunction = TimeMLDocumentFunction.valueOf(json.getString("timeMLDocumentFunction"));
			if (json.has("temporalFunction"))
				this.temporalFunction = json.getBoolean("temporalFunction");
			if (json.has("anchorTime"))
				this.anchorTimeReference = StoreReference.makeFromJSON(json.getJSONObject("anchorTime"));
			if (json.has("valueFromFunction"))
				this.valueFromFunctionReference = StoreReference.makeFromJSON(json.getJSONObject("valueFromFunction"));
			if (json.has("timeMLMod"))
				this.timeMLMod = TimeMLMod.valueOf(json.getString("timeMLMod"));
			if (json.has("valueRef"))
				this.valueReference = StoreReference.makeFromJSON(json.getJSONObject("valueRef"));
		} catch (JSONException e) {
			return false;
		}
		
		return true;
	}

	@Override
	public StoredJSONSerializable makeInstance(StoreReference reference) {
		return new TimeExpression(this.dataTools, reference);
	}

	@Override
	public StoreReference getStoreReference() {
		return this.reference;
	}
}