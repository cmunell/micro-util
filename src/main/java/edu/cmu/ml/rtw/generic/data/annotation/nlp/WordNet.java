/**
 * Copyright 2014 Bill McDowell 
 *
 * This file is part of theMess (https://github.com/forkunited/theMess)
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy 
 * of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software 
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT 
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the 
 * License for the specific language governing permissions and limitations 
 * under the License.
 */

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
