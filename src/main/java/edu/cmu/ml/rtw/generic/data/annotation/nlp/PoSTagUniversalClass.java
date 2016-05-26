package edu.cmu.ml.rtw.generic.data.annotation.nlp;


public class PoSTagUniversalClass {
	public static final PoSTag[] CONJ = { PoSTag.CC };
	public static final PoSTag[] NUM = { PoSTag.CD };
	public static final PoSTag[] DET = { PoSTag.DT, PoSTag.EX, PoSTag.WDT, PoSTag.PDT };
	public static final PoSTag[] ADP = { PoSTag.IN };
	public static final PoSTag[] ADJ = { PoSTag.JJ, PoSTag.JJR, PoSTag.JJS };
	public static final PoSTag[] VERB = { PoSTag.MD, PoSTag.VB, PoSTag.VBD, PoSTag.VBG, PoSTag.VBN, PoSTag.VBP, PoSTag.VBZ };
	public static final PoSTag[] NOUN = { PoSTag.NN, PoSTag.NNP, PoSTag.NNPS, PoSTag.NNS };
	public static final PoSTag[] ADV = { PoSTag.RB, PoSTag.RBR, PoSTag.RBS, PoSTag.WRB };
	public static final PoSTag[] PRT = { PoSTag.POS, PoSTag.RP, PoSTag.TO };
	public static final PoSTag[] PRON = { PoSTag.PRP, PoSTag.PRP$, PoSTag.WP, PoSTag.WP$ };
	public static final PoSTag[] PUNC = { PoSTag.SYM };
	public static final PoSTag[] X = { PoSTag.FW, PoSTag.LS, PoSTag.UH, PoSTag.Other };
	
	public static PoSTag[] fromString(String str) {
		if (str.equals("CONJ"))
			return PoSTagUniversalClass.CONJ;
		else if (str.equals("NUM"))
			return PoSTagUniversalClass.NUM;
		else if (str.equals("DET"))
			return PoSTagUniversalClass.DET;
		else if (str.equals("ADP"))
			return PoSTagUniversalClass.ADP;
		else if (str.equals("ADJ"))
			return PoSTagUniversalClass.ADJ;
		else if (str.equals("VERB"))
			return PoSTagUniversalClass.VERB;
		else if (str.equals("NOUN"))
			return PoSTagUniversalClass.NOUN;
		else if (str.equals("ADV"))
			return PoSTagUniversalClass.ADV;
		else if (str.equals("PRT"))
			return PoSTagUniversalClass.PRT;
		else if (str.equals("PRON"))
			return PoSTagUniversalClass.PRON;
		else if (str.equals("PUNC"))
			return PoSTagUniversalClass.PUNC;
		else if (str.equals("X"))
			return PoSTagUniversalClass.X;
		else
			return null;
	}
	
	public static boolean classContains(PoSTag[] tagClass, PoSTag tag) {
		for (PoSTag t : tagClass)
			if (tag == t)
				return true;
		return false;
	}
	
	public static PoSTag[] getTagClass(PoSTag tag) {
		if (tag == PoSTag.CC)
			return CONJ;
		else if (tag == PoSTag.CD)
			return NUM;
		else if (tag == PoSTag.DT)
			return DET;
		else if (tag == PoSTag.EX)
			return DET;
		else if (tag == PoSTag.WDT)
			return DET;
		else if (tag == PoSTag.PDT)
			return DET;
		else if (tag == PoSTag.IN)
			return ADP;
		else if (tag == PoSTag.JJ)
			return ADJ;
		else if (tag == PoSTag.JJR)
			return ADJ;
		else if (tag == PoSTag.JJS)
			return ADJ;
		else if (tag == PoSTag.MD)
			return VERB;
		else if (tag == PoSTag.VB)
			return VERB;
		else if (tag == PoSTag.VBD)
			return VERB;
		else if (tag == PoSTag.VBG)
			return VERB;
		else if (tag == PoSTag.VBN)
			return VERB;
		else if (tag == PoSTag.VBP)
			return VERB;
		else if (tag == PoSTag.VBZ)
			return VERB;
		else if (tag == PoSTag.NN)
			return NOUN;
		else if (tag == PoSTag.NNP)
			return NOUN;
		else if (tag == PoSTag.NNPS)
			return NOUN;
		else if (tag == PoSTag.NNS)
			return NOUN;
		else if (tag == PoSTag.RB)
			return ADV;
		else if (tag == PoSTag.RBR)
			return ADV;
		else if (tag == PoSTag.RBS)
			return ADV;
		else if (tag == PoSTag.WRB)
			return ADV;
		else if (tag == PoSTag.POS)
			return PRT;
		else if (tag == PoSTag.RP)
			return PRT;
		else if (tag == PoSTag.TO)
			return PRT;
		else if (tag == PoSTag.PRP)
			return PRON;
		else if (tag == PoSTag.PRP$)
			return PRON;
		else if (tag == PoSTag.WP)
			return PRON;
		else if (tag == PoSTag.WP$)
			return PRON;
		else if (tag == PoSTag.SYM)
			return PUNC;
		else
			return X;
	}
	
	public static String getTagClassString(PoSTag tag) {
		if (tag == PoSTag.CC)
			return "CONJ";
		else if (tag == PoSTag.CD)
			return "NUM";
		else if (tag == PoSTag.DT)
			return "DET";
		else if (tag == PoSTag.EX)
			return "DET";
		else if (tag == PoSTag.WDT)
			return "DET";
		else if (tag == PoSTag.PDT)
			return "DET";
		else if (tag == PoSTag.IN)
			return "ADP";
		else if (tag == PoSTag.JJ)
			return "ADJ";
		else if (tag == PoSTag.JJR)
			return "ADJ";
		else if (tag == PoSTag.JJS)
			return "ADJ";
		else if (tag == PoSTag.MD)
			return "VERB";
		else if (tag == PoSTag.VB)
			return "VERB";
		else if (tag == PoSTag.VBD)
			return "VERB";
		else if (tag == PoSTag.VBG)
			return "VERB";
		else if (tag == PoSTag.VBN)
			return "VERB";
		else if (tag == PoSTag.VBP)
			return "VERB";
		else if (tag == PoSTag.VBZ)
			return "VERB";
		else if (tag == PoSTag.NN)
			return "NOUN";
		else if (tag == PoSTag.NNP)
			return "NOUN";
		else if (tag == PoSTag.NNPS)
			return "NOUN";
		else if (tag == PoSTag.NNS)
			return "NOUN";
		else if (tag == PoSTag.RB)
			return "ADV";
		else if (tag == PoSTag.RBR)
			return "ADV";
		else if (tag == PoSTag.RBS)
			return "ADV";
		else if (tag == PoSTag.WRB)
			return "ADV";
		else if (tag == PoSTag.POS)
			return "PRT";
		else if (tag == PoSTag.RP)
			return "PRT";
		else if (tag == PoSTag.TO)
			return "PRT";
		else if (tag == PoSTag.PRP)
			return "PRON";
		else if (tag == PoSTag.PRP$)
			return "PRON";
		else if (tag == PoSTag.WP)
			return "PRON";
		else if (tag == PoSTag.WP$)
			return "PRON";
		else if (tag == PoSTag.SYM)
			return "PUNC";
		else
			return "X";
	}
}
