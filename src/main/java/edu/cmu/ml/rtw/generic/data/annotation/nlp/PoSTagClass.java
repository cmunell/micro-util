package edu.cmu.ml.rtw.generic.data.annotation.nlp;

/**
 * PoSTagClass represents sets of PoSTags that
 * are commonly referred to.
 * 
 * @author Bill McDowell
 *
 */
public class PoSTagClass {
	public static final PoSTag[] JJ = { PoSTag.JJ, PoSTag.JJR, PoSTag.JJS };
	public static final PoSTag[] VB = { PoSTag.VBD, PoSTag.VBZ, PoSTag.VBP, PoSTag.VBN, PoSTag.VBG, PoSTag.VB };
	public static final PoSTag[] NNP = { PoSTag.NNP, PoSTag.NNPS };
	public static final PoSTag[] NN = { PoSTag.NN, PoSTag.NNS };
	public static final PoSTag[] FN = { PoSTag.IN, PoSTag.DT, PoSTag.CC, PoSTag.POS };
	public static final PoSTag[] PRP = { PoSTag.PRP };
	public static final PoSTag[] RB = { PoSTag.RB, PoSTag.RBR, PoSTag.RBS };
	public static final PoSTag[] CD = { PoSTag.CD };

	public static PoSTag[] fromString(String str) {
		if (str.equals("JJ"))
			return PoSTagClass.JJ;
		else if (str.equals("VB"))
			return PoSTagClass.VB;
		else if (str.equals("NNP"))
			return PoSTagClass.NNP;
		else if (str.equals("NN"))
			return PoSTagClass.NN;
		else if (str.equals("FN"))
			return PoSTagClass.FN;
		else if (str.equals("PRP"))
			return PoSTagClass.PRP;
		else if (str.equals("RB"))
			return PoSTagClass.RB;
		else if (str.equals("CD"))
			return PoSTagClass.CD;
		else
			return null;
	}
	
	public static boolean classContains(PoSTag[] tagClass, PoSTag tag) {
		for (PoSTag t : tagClass)
			if (tag == t)
				return true;
		return false;
	}
}
