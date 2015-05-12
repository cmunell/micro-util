package edu.cmu.ml.rtw.generic.data.annotation.nlp;

/**
 * WordNet represents various aspects of English WordNet
 * (http://wordnet.princeton.edu/) or other non-English 
 * WordNets (http://www.illc.uva.nl/EuroWordNet/).  The class
 * is necessary to abstract away from these particular 
 * versions so that they can all be used in the same way.
 * 
 * @author Bill McDowell
 *
 */
public class WordNet {
	public enum Hypernym {
		Condition, 
		Software, 
		Furniture, 
		Building, 
		Communication, 
		Static, 
		Property, 
		Group, 
		LanguageRepresentation, 
		Mental,  
		Garment, 
		Covering, 
		Modal, 
		Representation, 
		FirstOrderEntity, 
		Vehicle, 
		Phenomenal, 
		Existence, 
		Artifact, 
		Recreation, 
		Creature, 
		Dynamic, 
		Agentive, 
		Purpose, 
		Living, 
		Solid, 
		ThirdOrderEntity, 
		SituationType, 
		Comestible, 
		Human, 
		Liquid, 
		Natural, 
		Animal, 
		Substance, 
		MoneyRepresentation, 
		Social, 
		Usage, 
		Experience, 
		Time, 
		Occupation, 
		Part, 
		Object, 
		Container, 
		Place, 
		Relation, 
		Plant, 
		Quantity, 
		Tops, 
		ImageRepresentation, 
		Function, 
		Location, 
		Manner, 
		Gas, 
		Physical, 
		UnboundedEvent, 
		Possession, 
		Instrument, 
		BoundedEvent, 
		Cause
	}
	
	public static Hypernym hypernymFromString(String str) {
		str = str.replace("3rd", "Third");
		str = str.replace("1st", "First");
		str = Character.toUpperCase(str.charAt(0)) + str.substring(1);
		
		return Hypernym.valueOf(str);
	}
}
