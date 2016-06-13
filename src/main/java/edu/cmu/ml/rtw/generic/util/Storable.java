package edu.cmu.ml.rtw.generic.util;

import edu.cmu.ml.rtw.generic.data.store.StoreReference;

public interface Storable {
	public Storable makeInstance(StoreReference reference);
	public StoreReference getStoreReference();
	public String getId();
}
