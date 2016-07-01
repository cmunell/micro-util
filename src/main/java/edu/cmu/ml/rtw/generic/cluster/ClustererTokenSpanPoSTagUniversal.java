package edu.cmu.ml.rtw.generic.cluster;

import java.util.ArrayList;
import java.util.List;

import edu.cmu.ml.rtw.generic.data.annotation.nlp.DocumentNLP;
import edu.cmu.ml.rtw.generic.data.annotation.nlp.PoSTagUniversalClass;
import edu.cmu.ml.rtw.generic.data.annotation.nlp.TokenSpan;


public class ClustererTokenSpanPoSTagUniversal extends Clusterer<TokenSpan> {
	public ClustererTokenSpanPoSTagUniversal() {
		
	}
	
	@Override
	public List<String> getClusters(TokenSpan tokenSpan) {
		List<String> clusters = new ArrayList<String>();
		if (tokenSpan.getSentenceIndex() < 0 || tokenSpan.getStartTokenIndex() < 0)
			return clusters;
		
		StringBuilder compoundCluster = new StringBuilder();
		DocumentNLP document = tokenSpan.getDocument();
		for (int i = tokenSpan.getStartTokenIndex(); i < tokenSpan.getEndTokenIndex(); i++) {
			if (i < 0) 
				compoundCluster.append("PRE-" + (i - 1)).append("_");
			else if (i >= document.getSentenceTokenCount(tokenSpan.getSentenceIndex()))
				compoundCluster.append("POST-" + (i - document.getSentenceTokenCount(tokenSpan.getSentenceIndex()))).append("_");
			else
				compoundCluster.append(PoSTagUniversalClass.getTagClassString(
						document.getPoSTag(tokenSpan.getSentenceIndex(), i))).append("_");
		}
	
		if (compoundCluster.length() == 0)
			return clusters;
		
		compoundCluster.delete(compoundCluster.length() - 1, compoundCluster.length());
		clusters.add(compoundCluster.toString());
		return clusters;
	}

	@Override
	public String getName() {
		return "PoSTagUniversal";
	}
}
