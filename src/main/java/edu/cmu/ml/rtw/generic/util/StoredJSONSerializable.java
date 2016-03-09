package edu.cmu.ml.rtw.generic.util;

import edu.cmu.ml.rtw.generic.data.store.StoreReference;

public interface StoredJSONSerializable extends JSONSerializable {
	public StoredJSONSerializable makeInstance(StoreReference reference);
	public StoreReference getStoreReference();
	public String getId();
}
