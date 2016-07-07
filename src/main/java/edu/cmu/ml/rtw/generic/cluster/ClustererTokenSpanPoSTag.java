package edu.cmu.ml.rtw.generic.cluster;

import java.util.ArrayList;
import java.util.List;

import edu.cmu.ml.rtw.generic.data.annotation.nlp.DocumentNLP;
import edu.cmu.ml.rtw.generic.data.annotation.nlp.PoSTag;
import edu.cmu.ml.rtw.generic.data.annotation.nlp.TokenSpan;

/**
 * ClustererTokenSpanPoSTag clusters token spans 
 * by the PoS tag sequences assigned to their tokens.
 * 
 * @author Bill McDowell
 *
 */
public class ClustererTokenSpanPoSTag extends Clusterer<TokenSpan> {
	public ClustererTokenSpanPoSTag() {
		
	}
	
	@Override
	public List<String> getClusters(TokenSpan tokenSpan) {
		return getClusters(tokenSpan, false);
	}
	
	public List<String> getClusters(TokenSpan tokenSpan, boolean literalSymbols) {
		List<String> clusters = new ArrayList<String>();
		if (tokenSpan.getSentenceIndex() < 0)
			return clusters;
		StringBuilder compoundCluster = new StringBuilder();
		DocumentNLP document = tokenSpan.getDocument();
		for (int i = tokenSpan.getStartTokenIndex(); i < tokenSpan.getEndTokenIndex(); i++) {
			if (i < 0) 
				compoundCluster.append("PRE-" + (int)Math.abs(i + 1)).append("_");
			else if (i >= document.getSentenceTokenCount(tokenSpan.getSentenceIndex()))
				compoundCluster.append("POST-" + (i - document.getSentenceTokenCount(tokenSpan.getSentenceIndex()))).append("_");
			else {
				PoSTag tag = document.getPoSTag(tokenSpan.getSentenceIndex(), i);
				String tagStr = null;
				
				if (literalSymbols && tag == PoSTag.SYM)
					tagStr = document.getTokenStr(tokenSpan.getSentenceIndex(), i);
				else
					tagStr = tag.toString();
				
				compoundCluster.append(tagStr).append("_");
			}
		}
	
		if (compoundCluster.length() == 0)
			return clusters;
		
		compoundCluster.delete(compoundCluster.length() - 1, compoundCluster.length());
		clusters.add(compoundCluster.toString());
		return clusters;
	}

	@Override
	public String getName() {
		return "PoSTag";
	}
}
