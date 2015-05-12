package edu.cmu.ml.rtw.generic.data.annotation.nlp;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Stack;
import edu.cmu.ml.rtw.generic.util.Pair;
import edu.cmu.ml.rtw.generic.util.StringSerializable;

/**
 * Constituency parse represents a constituency parse for 
 * a sentence 
 * (http://en.wikipedia.org/wiki/Parse_tree#Constituency-based_parse_trees).
 * 
 * @author Bill McDowell
 * 
 */
public class ConstituencyParse implements StringSerializable {
	public class Constituent {
		private Constituent parent;
		private TokenSpan tokenSpan;
		private Constituent[] children;
		private String label;
		
		public Constituent(String label, Constituent[] children) {
			this.tokenSpan = null;
			this.children = children;
			this.label = label;
			
			for (int i = 0; i < children.length; i++)
				children[i].parent = this;
		}
		
		public Constituent(String label, TokenSpan tokenSpan) {
			this.tokenSpan = tokenSpan;
			this.children = null;
			this.label = label;
		}
		
		public Constituent getParent() {
			return this.parent;
		}
		
		public String getLabel() {
			return this.label;
		}
		
		public Constituent[] getChildren() {
			return this.children;
		}
		
		public boolean isLeaf() {
			return this.children == null || this.children.length == 0;
		}
		
		public TokenSpan getTokenSpan() {
			if (this.tokenSpan != null)
				return new TokenSpan(document, sentenceIndex, this.tokenSpan.getStartTokenIndex(), this.tokenSpan.getEndTokenIndex());
			
			if (this.children.length == 0)
				return null;
			
			Constituent leftChild = this;
			while (!leftChild.isLeaf()) {
				if (leftChild.getChildren() == null || leftChild.getChildren().length == 0)
					return null;
				leftChild = leftChild.getChildren()[0];
			}
			
			Constituent rightChild = this;
			while (!rightChild.isLeaf()) {
				if (rightChild.getChildren() == null || rightChild.getChildren().length == 0)
					return null;
				rightChild = rightChild.getChildren()[rightChild.getChildren().length - 1];
			}
			
			return new TokenSpan(document, 
								 sentenceIndex, 
								 leftChild.tokenSpan.getStartTokenIndex(), 
								 rightChild.tokenSpan.getEndTokenIndex());
		}
	}
	
	/**
	 * ConstituentPath represents a path through a ConstituencyParse
	 * 
	 * @author Bill McDowell
	 */
	public class ConstituentPath {
		private List<Constituent> constituents;
		
		ConstituentPath(List<Constituent> constituents) {
			this.constituents = constituents;
		}
		
		public Constituent getConstituent(int index) {
			return this.constituents.get(index);
		}
		
		public boolean isBelowPrevious(int index) {
			if (index == 0)
				return false;
			return this.constituents.get(index).getParent().equals(index - 1);
		}
		
		public boolean isAbovePrevious(int index) {
			if (index == 0)
				return false;
			return !this.constituents.get(index).getParent().equals(index - 1);
		}
		
		public int getLength() {
			return this.constituents.size();
		}
		
		public String toString() {
			return toString(true);
		}
		
		public String toString(boolean includeTypes) {
			StringBuilder str = new StringBuilder();
			for (int i = 0; i < this.constituents.size(); i++) {
				String direction = "N";
				if (isAbovePrevious(i))
					direction = "U";
				else if (isBelowPrevious(i))
					direction = "D";
				
				if (includeTypes)
					str = str.append(this.constituents.get(i).getLabel()).append("-");
				str = str.append(direction).append("/");
			}
			if (str.length() > 0)
				str = str.delete(str.length() - 1, str.length());
			return str.toString();
		}
	}

	private DocumentNLP document;
	private int sentenceIndex;
	private Constituent root;
	
	public ConstituencyParse(DocumentNLP document, int sentenceIndex, Constituent root) {
		this.document = document;
		this.sentenceIndex = sentenceIndex;
		this.root = root;
	}
	
	public ConstituencyParse(DocumentNLP document, int sentenceIndex) {
		this(document, sentenceIndex, null);
	}
	
	public DocumentNLP getDocument() {
		return this.document;
	}
	
	public int getSentenceIndex() {
		return this.sentenceIndex;
	}
	
	public Constituent getRoot() {
		return this.root;
	}
	
	/**
	 * @param tokenIndex
	 * @return the constituent immediately containing the token
	 * at tokenIndex
	 */
	public Constituent getTokenConstituent(int tokenIndex) {
		Stack<Constituent> toVisit = new Stack<Constituent>();
		toVisit.add(this.root);
		while (!toVisit.isEmpty()) {
			Constituent current = toVisit.pop();
			if (current.isLeaf()) {
				TokenSpan tokenSpan = current.getTokenSpan();
				if (tokenSpan.getStartTokenIndex() == tokenIndex)
					return current;
			} else {
				Constituent[] children = current.getChildren();
				for (Constituent child : children)
					toVisit.push(child);
			}
		}
		
		return null;
		
	}
	
	public ConstituentPath getPath(int sourceTokenIndex, int targetTokenIndex) {
		return getPath(getTokenConstituent(sourceTokenIndex), getTokenConstituent(targetTokenIndex));
	}
	
	public ConstituentPath getPath(Constituent source, Constituent target) {
		if (source == null || target == null)
			return null;
		
		Stack<Constituent> toVisit = new Stack<Constituent>();
		toVisit.push(target);
		Map<Constituent, Constituent> paths = new HashMap<Constituent, Constituent>();
		paths.put(target, null);
		while (!toVisit.isEmpty()) {
			Constituent current = toVisit.pop();
			if (current.equals(source)) {
				List<Constituent> path = new ArrayList<Constituent>();
				Constituent pathCurrent = current;
				while (pathCurrent != null) {
					path.add(pathCurrent);
					pathCurrent = paths.get(pathCurrent);
				}
				
				ConstituentPath constituentPath = new ConstituentPath(path);
				return constituentPath;
			}
			
			if (current.getParent() != null && !paths.containsKey(current.getParent())) {
				toVisit.push(current.getParent());
				paths.put(current.getParent(), current);
			}
			if (!current.isLeaf()) {
				Constituent[] children = current.getChildren();
				for (Constituent child : children) {
					if (paths.containsKey(child))
						continue;
					toVisit.push(child);
					paths.put(child, current);
				}
			}
		}
		
		return null;
	}
	
	public boolean isAbove(Constituent source, Constituent target) {
		if (source == null || target == null)
			return false;
		
		if (source.equals(target))
			return false;
		
		ConstituentPath constituentPath = getPath(source, target);
		if (constituentPath == null)
			return false;
		
		for (int i = 1; i < constituentPath.getLength(); i++) {
			if (!constituentPath.getConstituent(i).getParent().equals(constituentPath.getConstituent(i - 1)))
				return false;
		}
		
		return true;
	}
	
	public boolean isBelow(Constituent source, Constituent target) {
		if (source == null || target == null)
			return false;
		
		if (source.equals(target))
			return false;
		
		ConstituentPath constituentPath = getPath(source, target);
		if (constituentPath == null)
			return false;
		
		for (int i = 1; i < constituentPath.getLength(); i++) {
			if (constituentPath.getConstituent(i).getParent().equals(constituentPath.getConstituent(i - 1)))
				return false;
		}
		
		return true;
	}

	public String toString() {
		if (this.root == null)
			return "";
			
		StringBuilder str = new StringBuilder();
		Stack<Pair<Integer, Constituent>> toOutput = new Stack<Pair<Integer,Constituent>>();
		toOutput.push(new Pair<Integer, Constituent>(0, root));
		int previousLevel = -1;
		while (!toOutput.isEmpty()) {
			Pair<Integer, Constituent> current = toOutput.pop();
			Constituent constituent = current.getSecond();
			
			for (int i = current.getFirst(); i <= previousLevel; i++)
				str.append(")");
			
			if (constituent.isLeaf()) {
				str.append(" (").append(constituent.getLabel()).append(" ");
				TokenSpan leafSpan = new TokenSpan(this.document, this.sentenceIndex, constituent.getTokenSpan().getStartTokenIndex(), constituent.getTokenSpan().getEndTokenIndex());
				if (leafSpan != null)
					str.append(leafSpan.toString().replace("(", "-LRB-").replace(")", "-RRB-"));
			} else {
				str.append(" (").append(constituent.getLabel()).append(" ");
				
				Constituent[] children= constituent.getChildren();
				for (int i = children.length - 1; i >= 0; i--) {
					toOutput.push(new Pair<Integer, Constituent>(current.getFirst()+1, children[i]));
				}
			}
			
			previousLevel = current.getFirst();
		}
		
		for (int i = 0; i <= previousLevel; i++)
			str.append(")");
		
		return str.toString();
	}
	
	@Override
	public boolean fromString(String str) {
		if (str.trim().equals(""))
			return true;
		
		// NOTE: This code is pretty illegible...  but it's written in such a way that
		// only one pass over the input string is necessary
		Stack<Pair<String, List<Constituent>>> constituents = new Stack<Pair<String, List<Constituent>>>();
		
		// Number of non-paren/whitespace terms read in row.  
		// Keeps track of whether type or token
		int leafStartTokenIndex = 0;
		int leafEndTokenIndex = 0;
		boolean readLabel = false;
		StringBuilder currentPiece = new StringBuilder();
		for (int i = 0; i < str.length(); i++) {
			char c = str.charAt(i);
			if (c == '\t' || c == ' ' || c == '\n' || c == '\r') {
				if (currentPiece.length() != 0) { 
					if (!readLabel) {
						constituents.push(new Pair<String, List<Constituent>>(currentPiece.toString(), new LinkedList<Constituent>()));	
						readLabel = true;
					} else {
						leafEndTokenIndex++;
					}
					currentPiece = new StringBuilder();
				}
			} else if (c == '(') {
				currentPiece = new StringBuilder();
				readLabel = false;
			} else if (c == ')') {
				if (currentPiece.length() != 0) {
					leafEndTokenIndex++;
					currentPiece = new StringBuilder();
				}
				Pair<String, List<Constituent>> constituentParts = constituents.pop();
				Constituent constituent = null;
				if (leafStartTokenIndex != leafEndTokenIndex) {
					TokenSpan leafTokenSpan = new TokenSpan(document, sentenceIndex, leafStartTokenIndex, leafEndTokenIndex);
					constituent = this.new Constituent(constituentParts.getFirst(), leafTokenSpan);			
					leafStartTokenIndex = leafEndTokenIndex;
				} else {
					constituent = this.new Constituent(constituentParts.getFirst(), constituentParts.getSecond().toArray(new Constituent[0]));
				}
				
				if (constituents.isEmpty())
					this.root = constituent;
				else
					constituents.peek().getSecond().add(constituent);
			} else {
				currentPiece.append(str.charAt(i));	
			}
		}
		
		return true;
	}
	
	public static ConstituencyParse fromString(String str, DocumentNLP document, int sentenceIndex) {
		ConstituencyParse parse = new ConstituencyParse(document, sentenceIndex);
		if (!parse.fromString(str))
			return null;
		return parse;
	}
	
	public ConstituencyParse clone(DocumentNLP document) {
		return new ConstituencyParse(document, this.sentenceIndex, this.root);
	}
}
