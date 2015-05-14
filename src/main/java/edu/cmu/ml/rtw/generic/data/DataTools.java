package edu.cmu.ml.rtw.generic.data;

import java.util.Collection;
import java.util.Map;
import java.util.HashMap;
import java.util.Random;

import edu.cmu.ml.rtw.generic.util.OutputWriter;
import edu.cmu.ml.rtw.generic.util.StringUtil;
import edu.cmu.ml.rtw.generic.util.Timer;
import edu.cmu.ml.rtw.generic.cluster.Clusterer;
import edu.cmu.ml.rtw.generic.cluster.ClustererString;
import edu.cmu.ml.rtw.generic.cluster.ClustererTokenSpanPoSTag;
import edu.cmu.ml.rtw.generic.data.Gazetteer;
import edu.cmu.ml.rtw.generic.data.annotation.nlp.AnnotationTypeNLP;
import edu.cmu.ml.rtw.generic.data.annotation.nlp.TokenSpan;

/**
 * 
 * DataTools loads gazetteers, brown clusterers, string cleaning 
 * functions, and other tools used in various 
 * models and experiments.  
 * 
 * A DataSet (in edu.cmu.ml.rtw.generic.data.annotation) has
 * access to a Tools object (defined in edu.cmu.ml.rtw.generic.data.annotation.Datum),
 * and that object contains a pointer to a DataTools object, so
 * any place in the code that has access to a DataSet also has access
 * to DataTools.  The difference between DataTools and Datum.Tools is
 * that Datum.Tools contains tools specific to a particular kind of
 * datum (e.g. a document datum in text classification or a tlink datum
 * in temporal ordering), whereas DataTools contains generic tools
 * that can be useful when working with many kinds of Datums.  This
 * split between generic tools and datum-specific tools allows the generic
 * tools to be loaded into memory only once even if you're working with
 * many kinds of datums at the same time.
 * 
 * Currently, for convenience, DataTools just loads everything into 
 * memory upon construction.  If memory conservation becomes particularly
 * important, then possibly this class should be rewritten to only keep 
 * things in memory when they are needed.
 * 
 * @author Bill McDowell
 *
 */
public class DataTools {
	/**
	 * Interface for a function that maps a string to another string--for
	 * example, for cleaning out garbage text before processing by features
	 * or models.
	 *
	 */
	public interface StringTransform {
		String transform(String str);
		// Return constant name for this transformation (used for deserializing features)
		String toString(); 
	}
	
	/**
	 * Interface for a function that maps a pair of strings to a real number--
	 * for example, as a measure of their similarity.
	 *
	 */
	public interface StringPairMeasure {
		double compute(String str1, String str2);
	}
	
	/**
	 * Interface for a function that maps a string to a collection of strings--
	 * for example, to compute a collection of prefixes or suffixes for a string.
	 *
	 */
	public interface StringCollectionTransform {
		Collection<String> transform(String str);
		String toString();
	}
	
	/**
	 * Represents a named file path.  It's useful for file paths to have
	 * names so that they can be referenced in experiment configuration files
	 * without machine specific file locations.
	 *
	 */
	public class Path {
		private String name;
		private String value;
		
		public Path(String name, String value) {
			this.name = name;
			this.value = value;
		}
		
		public String getValue() {
			return this.value;
		}
		
		public String getName() {
			return this.name;
		}
	}
	
	protected Map<String, Gazetteer> gazetteers;
	protected Map<String, DataTools.StringTransform> cleanFns;
	protected Map<String, DataTools.StringCollectionTransform> collectionFns;
	protected Map<String, Clusterer<String>> stringClusterers;
	protected Map<String, Clusterer<TokenSpan>> tokenSpanClusterers;
	protected Map<String, Path> paths;

	protected Map<String, AnnotationTypeNLP<?>> annotationTypesNLP;
	
	protected long randomSeed;
	protected Random globalRandom;
	protected OutputWriter outputWriter;
	protected Timer timer;
	
	public DataTools() {
		this(new OutputWriter());
	}
	
	public DataTools(OutputWriter outputWriter) {
		this.gazetteers = new HashMap<String, Gazetteer>();
		this.cleanFns = new HashMap<String, DataTools.StringTransform>();
		this.collectionFns = new HashMap<String, DataTools.StringCollectionTransform>();
		this.stringClusterers = new HashMap<String, Clusterer<String>>();
		this.tokenSpanClusterers = new HashMap<String, Clusterer<TokenSpan>>();
		this.paths = new HashMap<String, Path>();
		this.annotationTypesNLP = new HashMap<String, AnnotationTypeNLP<?>>();
		
		this.outputWriter = outputWriter;
		
		this.cleanFns.put("DefaultCleanFn", new DataTools.StringTransform() {
			public String toString() {
				return "DefaultCleanFn";
			}
			
			public String transform(String str) {
				return StringUtil.clean(str);
			}
		});
		
		this.collectionFns.put("Prefixes", new DataTools.StringCollectionTransform() {
			public String toString() {
				return "Prefixes";
			}
			
			public Collection<String> transform(String str) {
				return StringUtil.prefixes(str);
			}
		});
		
		this.addTokenSpanClusterer(new ClustererTokenSpanPoSTag());
		
		this.collectionFns.put("None", null);
		this.stringClusterers.put("None", null);
		this.tokenSpanClusterers.put("None", null);
		this.globalRandom = new Random();
		this.timer = new Timer();
		
		this.addAnnotationTypeNLP(AnnotationTypeNLP.ORIGINAL_TEXT);
		this.addAnnotationTypeNLP(AnnotationTypeNLP.LANGUAGE);
		this.addAnnotationTypeNLP(AnnotationTypeNLP.TOKEN);
		this.addAnnotationTypeNLP(AnnotationTypeNLP.SENTENCE);
		this.addAnnotationTypeNLP(AnnotationTypeNLP.POS);
		this.addAnnotationTypeNLP(AnnotationTypeNLP.NER);
		this.addAnnotationTypeNLP(AnnotationTypeNLP.COREF);
		this.addAnnotationTypeNLP(AnnotationTypeNLP.DEPENDENCY_PARSE);
		this.addAnnotationTypeNLP(AnnotationTypeNLP.CONSTITUENCY_PARSE);
	}
	
	public Gazetteer getGazetteer(String name) {
		return this.gazetteers.get(name);
	}
	
	public DataTools.StringTransform getCleanFn(String name) {
		return this.cleanFns.get(name);
	}
	
	public DataTools.StringCollectionTransform getCollectionFn(String name) {
		return this.collectionFns.get(name);
	}
	
	public Clusterer<String> getStringClusterer(String name) {
		return this.stringClusterers.get(name);
	}
	
	public Clusterer<TokenSpan> getTokenSpanClusterer(String name) {
		return this.tokenSpanClusterers.get(name);
	}
	
	public Path getPath(String name) {
		return this.paths.get(name);
	}

	public AnnotationTypeNLP<?> getAnnotationTypeNLP(String type) {
		return this.annotationTypesNLP.get(type);
	}
	
	public Collection<AnnotationTypeNLP<?>> getAnnotationTypesNLP() {
		return this.annotationTypesNLP.values();
	}
	
	public OutputWriter getOutputWriter() {
		return this.outputWriter;
	}
	
	/**
	 * @return a Random object instance that was instantiated when the DataTools
	 * object was instantiated. 
	 */
	public Random getGlobalRandom() {
		return this.globalRandom;
	}
	
	/**
	 * @return a Random object instance that is instantiated when makeLocalRandom 
	 * is called.  This is useful when there are multiple threads that require
	 * their own Random instances in order to preserve determinism with respect
	 * to a single Random seed.  Otherwise, if threads share the same Random 
	 * instance, then the order in which they interleave execution will determine 
	 * the behavior of the program.
	 * 
	 */
	public Random makeLocalRandom() {
		return new Random(this.randomSeed); 
	}
	
	public Timer getTimer() {
		return this.timer;
	}
	
	public boolean addGazetteer(Gazetteer gazetteer) {
		this.gazetteers.put(gazetteer.getName(), gazetteer);
		return true;
	}
	
	public boolean addCleanFn(DataTools.StringTransform cleanFn) {
		this.cleanFns.put(cleanFn.toString(), cleanFn);
		return true;
	}
	
	public boolean addStopWordsCleanFn(final Gazetteer stopWords) {
		this.cleanFns.put("StopWordsCleanFn_" + stopWords.getName(), 
			new DataTools.StringTransform() {
				public String toString() {
					return "StopWordsCleanFn_" + stopWords.getName();
				}
				
				public String transform(String str) {
					str = StringUtil.clean(str);
					String stoppedStr = stopWords.removeTerms(str);
					if (stoppedStr.length() > 0)
						return stoppedStr;
					else 
						return str;
				}
			}
		);
		
		return true;
	}
	
	public boolean addCollectionFn(DataTools.StringCollectionTransform collectionFn) {
		this.collectionFns.put(collectionFn.toString(), collectionFn);
		return true;
	}
	
	public boolean addStringClusterer(ClustererString clusterer) {
		this.stringClusterers.put(clusterer.getName(), clusterer);
		return true;
	}
	
	public boolean addTokenSpanClusterer(Clusterer<TokenSpan> clusterer) {
		this.tokenSpanClusterers.put(clusterer.getName(), clusterer);
		return true;
	}
	
	public boolean addPath(String name, Path path) {
		this.paths.put(name, path);
		return true;
	}

	public boolean addAnnotationTypeNLP(AnnotationTypeNLP<?> type) {
		this.annotationTypesNLP.put(type.getType(), type);
		return true;
	}
	
	public boolean setRandomSeed(long seed) {
		this.randomSeed = seed;
		this.globalRandom.setSeed(this.randomSeed);
		return true;
	}
}
