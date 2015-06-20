package edu.cmu.ml.rtw.generic.cluster;

import java.util.ArrayList;
import java.util.List;

import edu.cmu.ml.rtw.generic.data.annotation.nlp.TokenSpan;

/**
 * ClustererTokenSpanString clusters token spans
 * by the strings that occur within the spans.
 * 
 * @author Bill McDowell
 *
 */
public class ClustererTokenSpanString extends Clusterer<TokenSpan> {
	private Clusterer<String> stringClusterer;
	
	public ClustererTokenSpanString(Clusterer<String> stringClusterer) {
		this.stringClusterer = stringClusterer;
	}
	
	@Override
	public List<String> getClusters(TokenSpan tokenSpan) {
		// FIXME For now, this just returns first clusters given by
		// string clusterer if token span length is greater than 1.
		// Want to do all combinations...?
		
		if (tokenSpan.getLength() == 0)
			return new ArrayList<String>();
		else if (tokenSpan.getLength() == 1)
			return this.stringClusterer.getClusters(tokenSpan.getDocument().getTokenStr(tokenSpan.getSentenceIndex(), tokenSpan.getStartTokenIndex()));
		
		StringBuilder compoundCluster = new StringBuilder();
		for (int i = tokenSpan.getStartTokenIndex(); i < tokenSpan.getEndTokenIndex(); i++) {
			List<String> clusters = this.stringClusterer.getClusters(tokenSpan.getDocument().getTokenStr(tokenSpan.getSentenceIndex(), i));
			if (clusters != null && clusters.size() == 0) {
				String cluster = clusters.get(0);
				compoundCluster.append(cluster);
			}
			compoundCluster.append("_");
		}
		
		List<String> clusters = new ArrayList<String>();
		if (compoundCluster.length() == 0)
			return clusters;
		compoundCluster.delete(compoundCluster.length() - 1, compoundCluster.length());
		clusters.add(compoundCluster.toString());
		return clusters;
	}

	@Override
	public String getName() {
		return this.stringClusterer.getName();
	}
}
