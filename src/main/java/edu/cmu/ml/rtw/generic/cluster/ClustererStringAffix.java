package edu.cmu.ml.rtw.generic.cluster;

import java.util.ArrayList;
import java.util.List;

import edu.cmu.ml.rtw.generic.data.DataTools;

/**
 * ClustererAffix clusters strings by their
 * prefixes and suffixes.
 * 
 * @author Bill McDowell
 *
 */
public class ClustererStringAffix extends ClustererString {
	private String name;
	private int maxAffixLength;
	
	public ClustererStringAffix(String name, int maxAffixLength) {
		this(name, maxAffixLength, null);
	}
	
	public ClustererStringAffix(String name, int maxAffixLength, DataTools.StringTransform cleanFn) {
		super(cleanFn);
		this.name = name;
		this.maxAffixLength = maxAffixLength;
	}
	
	@Override
	public String getName() {
		return this.name;
	}
	
	@Override
	protected List<String> getClustersHelper(String str) {
		List<String> affixes = new ArrayList<String>();
		
		if (this.cleanFn != null)
			str = this.cleanFn.transform(str);
		
		for (int i = 3; i <= this.maxAffixLength; i++) {
			if (i >= str.length())
				continue;
			
			String prefix = str.substring(0, i);
			String suffix = str.substring(str.length()-i, str.length());
			
			affixes.add("p_" + prefix);
			affixes.add("s_" + suffix);
		}
		
		return affixes;
	}
}
