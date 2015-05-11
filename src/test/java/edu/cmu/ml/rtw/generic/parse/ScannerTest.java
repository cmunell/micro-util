package edu.cmu.ml.rtw.generic.parse;

import java_cup.runtime.Symbol;

import org.junit.Assert;
import org.junit.Test;

import edu.cmu.ml.rtw.generic.util.StringUtil;

public class ScannerTest {
	@Test
	public void testScannerValueAssignment() {
		testScannerByTokens(new String[] { "value", "x", "=", "1", ";" });
	}
	
	@Test
	public void testScannerMultipleAssignments() {
		testScannerByTokens(new String[] { "value", "x", "=", "1", ";", "array", "y", "=", "(", "2", ",", "3", ")", ";"});
	}
	
	@Test
	public void testScannerValueQuotedAssignment() {
		testScannerByTokens(new String[] { "value", "x", "=", "\"1\"", ";" });
	}
	
	@Test
	public void testScannerValueQuotedEscapedAssignment() {
		testScannerByTokens(new String[] { "value", "x", "=", "\"1\\\"\"", ";" });
	}
	
	@Test
	public void testScannerFunctionAssignment() {
		testScannerByTokens(new String[] { "feature", "x", "=", "Feature", "(", "name", "=", "value", ",", "name2", "=", "value", ")", ";" });
	}
	
	private void testScannerByTokens(String[] tokens) {
		ARKScanner scanner = new ARKScanner(StringUtil.join(tokens, " "));
		int i = 0;
		try {
			Symbol symbol = scanner.next_token();
			while (symbol.sym != ARKSymbol.EOF) {
				int tokenSymbol = ARKScanner.getStringSymbol(tokens[i]);
				if (tokenSymbol >= 0) {
					//System.out.println("Successfully scanned symbol: " + tokenSymbol);
					Assert.assertEquals(tokenSymbol, symbol.sym);
				} else if (ARKScanner.isQuotedString(tokens[i])) {
					//System.out.println("Successfully scanned quoted string: " + ARKScanner.unescapeQuotedString(tokens[i]));
					Assert.assertEquals(ARKScanner.unescapeQuotedString(tokens[i]), symbol.value.toString());
				} else {
					//System.out.println("Successfully scanned string: " + tokens[i]);
					Assert.assertEquals(tokens[i], symbol.value);
				}
				
				symbol = scanner.next_token();
				i++;
			}
		} catch (Exception e) {
			e.printStackTrace();
			Assert.fail(e.getMessage());
		}
		
		Assert.assertEquals(tokens.length, i);
	}
}
