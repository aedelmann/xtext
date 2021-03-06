/*
 * generated by Xtext
 */
package org.eclipse.xtext.parser.epatch.parser.antlr;

import com.google.inject.Inject;

import org.eclipse.xtext.parser.antlr.XtextTokenStream;
import org.eclipse.xtext.parser.epatch.services.EpatchTestLanguageGrammarAccess;

public class EpatchTestLanguageParser extends org.eclipse.xtext.parser.antlr.AbstractAntlrParser {
	
	@Inject
	private EpatchTestLanguageGrammarAccess grammarAccess;
	
	@Override
	protected void setInitialHiddenTokens(XtextTokenStream tokenStream) {
		tokenStream.setInitialHiddenTokens("RULE_WS", "RULE_ML_COMMENT", "RULE_SL_COMMENT");
	}
	
	@Override
	protected org.eclipse.xtext.parser.epatch.parser.antlr.internal.InternalEpatchTestLanguageParser createParser(XtextTokenStream stream) {
		return new org.eclipse.xtext.parser.epatch.parser.antlr.internal.InternalEpatchTestLanguageParser(stream, getGrammarAccess());
	}
	
	@Override 
	protected String getDefaultRuleName() {
		return "EPatch";
	}
	
	public EpatchTestLanguageGrammarAccess getGrammarAccess() {
		return this.grammarAccess;
	}
	
	public void setGrammarAccess(EpatchTestLanguageGrammarAccess grammarAccess) {
		this.grammarAccess = grammarAccess;
	}
	
}
