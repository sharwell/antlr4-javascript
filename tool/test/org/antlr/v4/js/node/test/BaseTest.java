/*
 * [The "BSD license"]
 *  Copyright (c) 2012 Terence Parr
 *  Copyright (c) 2012 Sam Harwell
 *  All rights reserved.
 *
 *  Redistribution and use in source and binary forms, with or without
 *  modification, are permitted provided that the following conditions
 *  are met:
 *
 *  1. Redistributions of source code must retain the above copyright
 *     notice, this list of conditions and the following disclaimer.
 *  2. Redistributions in binary form must reproduce the above copyright
 *     notice, this list of conditions and the following disclaimer in the
 *     documentation and/or other materials provided with the distribution.
 *  3. The name of the author may not be used to endorse or promote products
 *     derived from this software without specific prior written permission.
 *
 *  THIS SOFTWARE IS PROVIDED BY THE AUTHOR ``AS IS'' AND ANY EXPRESS OR
 *  IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES
 *  OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED.
 *  IN NO EVENT SHALL THE AUTHOR BE LIABLE FOR ANY DIRECT, INDIRECT,
 *  INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT
 *  NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE,
 *  DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY
 *  THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 *  (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF
 *  THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package org.antlr.v4.js.node.test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.antlr.v4.Tool;
import org.antlr.v4.js.test.CommonBaseTest;
import org.antlr.v4.runtime.atn.ATN;
import org.antlr.v4.runtime.atn.ATNState;
import org.antlr.v4.runtime.atn.DecisionState;
import org.antlr.v4.runtime.dfa.DFA;
import org.antlr.v4.test.ErrorQueue;
import org.antlr.v4.tool.ANTLRMessage;
import org.antlr.v4.tool.DefaultToolListener;
import org.antlr.v4.tool.Grammar;
import org.antlr.v4.tool.LexerGrammar;
import org.stringtemplate.v4.ST;

public abstract class BaseTest extends CommonBaseTest {
	public DFA createDFA(Grammar g, DecisionState s) {
//		PredictionDFAFactory conv = new PredictionDFAFactory(g, s);
//		DFA dfa = conv.createDFA();
//		conv.issueAmbiguityWarnings();
//		System.out.print("DFA="+dfa);
//		return dfa;
		return null;
	}

//	public void minimizeDFA(DFA dfa) {
//		DFAMinimizer dmin = new DFAMinimizer(dfa);
//		dfa.minimized = dmin.minimize();
//	}

	List<ANTLRMessage> checkRuleDFA(String gtext, String ruleName, String expecting)
		throws Exception
	{
		ErrorQueue equeue = new ErrorQueue();
		Grammar g = new Grammar(gtext, equeue);
		ATN atn = createATN(g, false);
		ATNState s = atn.ruleToStartState[g.getRule(ruleName).index];
		if ( s==null ) {
			System.err.println("no such rule: "+ruleName);
			return null;
		}
		ATNState t = s.transition(0).target;
		if ( !(t instanceof DecisionState) ) {
			System.out.println(ruleName+" has no decision");
			return null;
		}
		DecisionState blk = (DecisionState)t;
		checkRuleDFA(g, blk, expecting);
		return equeue.all;
	}

	List<ANTLRMessage> checkRuleDFA(String gtext, int decision, String expecting)
		throws Exception
	{
		ErrorQueue equeue = new ErrorQueue();
		Grammar g = new Grammar(gtext, equeue);
		ATN atn = createATN(g, false);
		DecisionState blk = atn.decisionToState.get(decision);
		checkRuleDFA(g, blk, expecting);
		return equeue.all;
	}

	void checkRuleDFA(Grammar g, DecisionState blk, String expecting)
		throws Exception
	{
		DFA dfa = createDFA(g, blk);
		String result = null;
		if ( dfa!=null ) result = dfa.toString();
		assertEquals(expecting, result);
	}

	List<ANTLRMessage> checkLexerDFA(String gtext, String expecting)
		throws Exception
	{
		return checkLexerDFA(gtext, LexerGrammar.DEFAULT_MODE_NAME, expecting);
	}

	List<ANTLRMessage> checkLexerDFA(String gtext, String modeName, String expecting)
		throws Exception
	{
		ErrorQueue equeue = new ErrorQueue();
		LexerGrammar g = new LexerGrammar(gtext, equeue);
		g.atn = createATN(g, false);
//		LexerATNToDFAConverter conv = new LexerATNToDFAConverter(g);
//		DFA dfa = conv.createDFA(modeName);
//		g.setLookaheadDFA(0, dfa); // only one decision to worry about
//
//		String result = null;
//		if ( dfa!=null ) result = dfa.toString();
//		assertEquals(expecting, result);
//
//		return equeue.all;
		return null;
	}

	/** Return true if all is ok, no errors */
	@Override
	protected ErrorQueue antlr(String fileName, String grammarFileName, String grammarStr, boolean defaultListener, String... extraOptions) {
		System.out.println("dir "+tmpdir);
		mkdir(tmpdir);
		writeFile(tmpdir, fileName, grammarStr);
		final List<String> options = new ArrayList<String>();
		Collections.addAll(options, extraOptions);
		options.add("-Dlanguage=JavaScript");
		options.add("-o");
		options.add(tmpdir);
		options.add("-lib");
		options.add(tmpdir);
		options.add(new File(tmpdir,grammarFileName).toString());

		final String[] optionsA = new String[options.size()];
		options.toArray(optionsA);
		Tool antlr = newTool(optionsA);
		ErrorQueue equeue = new ErrorQueue(antlr);
		antlr.addListener(equeue);
		if (defaultListener) {
			antlr.addListener(new DefaultToolListener(antlr));
		}
		antlr.processGrammarsOnCommandLine();

		if ( !defaultListener && !equeue.errors.isEmpty() ) {
			System.err.println("antlr reports errors from "+options);
			for (int i = 0; i < equeue.errors.size(); i++) {
				ANTLRMessage msg = equeue.errors.get(i);
				System.err.println(msg);
			}
			System.out.println("!!!\ngrammar:");
			System.out.println(grammarStr);
			System.out.println("###");
		}
		if ( !defaultListener && !equeue.warnings.isEmpty() ) {
			System.err.println("antlr reports warnings from "+options);
			for (int i = 0; i < equeue.warnings.size(); i++) {
				ANTLRMessage msg = equeue.warnings.get(i);
				System.err.println(msg);
			}
		}

		return equeue;
	}

	protected String execLexer(String grammarFileName,
							   String grammarStr,
							   String lexerName,
							   String input)
	{
		return execLexer(grammarFileName, grammarStr, lexerName, input, false);
	}

	protected String execLexer(String grammarFileName,
							   String grammarStr,
							   String lexerName,
							   String input,
							   boolean showDFA)
	{
		boolean success = rawGenerateAndBuildRecognizer(grammarFileName,
									  grammarStr,
									  null,
									  lexerName,"-no-listener");
		assertTrue(success);
		writeFile(tmpdir, "input", input);
		writeLexerTestFile(lexerName, showDFA);
		String output = execModule("Test.js");
		if ( stderrDuringParse!=null && stderrDuringParse.length()>0 ) {
			System.err.println(stderrDuringParse);
		}
		return output;
	}

	protected String execParser(String grammarFileName,
								String grammarStr,
								String parserName,
								String lexerName,
								String listenerName,
								String visitorName,
								String startRuleName,
								String input, boolean debug)
	{
		boolean success = rawGenerateAndBuildRecognizer(grammarFileName,
														grammarStr,
														parserName,
														lexerName,
														"-visitor");
		assertTrue(success);
		writeFile(tmpdir, "input", input);
		rawBuildRecognizerTestFile(parserName,
								 lexerName,
								 listenerName,
								 visitorName,
								 startRuleName,
								 debug);
		return execRecognizer();
	}

	protected void rawBuildRecognizerTestFile(String parserName,
									   String lexerName,
									   String listenerName,
									   String visitorName,
									   String parserStartRuleName,
									   boolean debug)
	{
        this.stderrDuringParse = null;
		if ( parserName==null ) {
			writeLexerTestFile(lexerName, false);
		}
		else {
			writeParserTestFile(parserName,
						  lexerName,
						  listenerName,
						  visitorName,
						  parserStartRuleName,
						  debug);
		}
	}

	public String execRecognizer() {
		return execModule("Test.js");
	}

	public String execModule(String fileName) {
		String nodejsPath = locateNodeJS();
		String runtimePath = locateRuntime();
		String modulePath = new File(new File(tmpdir), fileName).getAbsolutePath();
		String inputPath = new File(new File(tmpdir), "input").getAbsolutePath();
		try {
			ProcessBuilder builder = new ProcessBuilder( nodejsPath, modulePath, inputPath );
			builder.environment().put("NODE_PATH",runtimePath + ":" + tmpdir);
			builder.directory(new File(tmpdir)); 
			Process process = builder.start();
			StreamVacuum stdoutVacuum = new StreamVacuum(process.getInputStream());
			StreamVacuum stderrVacuum = new StreamVacuum(process.getErrorStream());
			stdoutVacuum.start();
			stderrVacuum.start();
			process.waitFor();
			stdoutVacuum.join();
			stderrVacuum.join();
			String output = stdoutVacuum.toString();
			if ( stderrVacuum.toString().length()>0 ) {
				this.stderrDuringParse = stderrVacuum.toString();
				System.err.println("exec stderrVacuum: "+ stderrVacuum);
			}
			return output;
		}
		catch (Exception e) {
			System.err.println("can't exec recognizer");
			e.printStackTrace(System.err);
		}
		return null;
	}

	private String locateNodeJS() {
		// typically /usr/local/bin/node
		String propName = "antlr-javascript-nodejs";
		String prop = System.getProperty(propName);
		if(prop==null || prop.length()==0)
			throw new RuntimeException("Missing system property:" + propName);
		return prop;
	}

	protected void writeParserTestFile(String parserName,
								 String lexerName,
								 String listenerName,
								 String visitorName,
								 String parserStartRuleName,
								 boolean debug)
	{
		ST outputFileST = new ST(
			"var antlr4 = require('antlr4');\n" +
			"var <lexerName> = require('./<lexerName>');\n" +
			"var <parserName> = require('./<parserName>');\n" +
			"var <listenerName> = require('./<listenerName>').<listenerName>;\n" +
			"var <visitorName> = require('./<visitorName>').<visitorName>;\n" +
			"\n" +
			"function TreeShapeListener() {\n" +
			"	antlr4.tree.ParseTreeListener.call(this);\n" +
			"	return this;\n" +
			"}\n" +
			"\n" +
			"TreeShapeListener.prototype = Object.create(antlr4.tree.ParseTreeListener.prototype);\n" +
			"TreeShapeListener.prototype.constructor = TreeShapeListener;\n" +
			"\n" +
			"TreeShapeListener.prototype.enterEveryRule = function(ctx) {\n" +
			"	for(var i=0;i\\<ctx.getChildCount; i++) {\n" +
			"		var child = ctx.getChild(i);\n" +
			"       var parent = child.parentCtx;\n" +
			"       if(parent.getRuleContext() !== ctx || !(parent instanceof antlr4.tree.RuleNode)) {\n" +
			"           throw \"Invalid parse tree shape detected.\";\n" +
			"		}\n" +
			"	}\n" +	
			"};\n" +
			"\n" +
			"function main(argv) {\n" +
			"    var input = new antlr4.FileStream(argv[2]);\n" +
			"    var lexer = new <lexerName>.<lexerName>(input);\n" +
		    "    var stream = new antlr4.CommonTokenStream(lexer);\n" +
			"<createParser>"+
			"    parser.buildParseTrees = true;\n" +
			"    var tree = parser.<parserStartRuleName>();\n" +
			"    antlr4.tree.ParseTreeWalker.DEFAULT.walk(new TreeShapeListener(), tree);\n" +
			"}\n" +
			"\n" +
			"main(process.argv);\n" +
			"\n");
        ST createParserST = new ST("	var parser = new <parserName>.<parserName>(stream);\n");
		if ( debug ) {
			createParserST = new ST("	var parser = new <parserName>.<parserName>(stream);\n" +
						"	parser.addErrorListener(new antlr4.error.DiagnosticErrorListener());\n");
		}
		outputFileST.add("createParser", createParserST);
		outputFileST.add("parserName", parserName);
		outputFileST.add("lexerName", lexerName);
		outputFileST.add("listenerName", listenerName);
		outputFileST.add("visitorName", visitorName);
		outputFileST.add("parserStartRuleName", parserStartRuleName);
		writeFile(tmpdir, "Test.js", outputFileST.render());
	}

	protected void writeLexerTestFile(String lexerName, boolean showDFA) {
		ST outputFileST = new ST(
			"var antlr4 = require('antlr4');\n" +
			"var <lexerName> = require('./<lexerName>');\n" +
			"\n" +
			"function main(argv) {\n" +
			"    var input = new antlr4.FileStream(argv[2]);\n" +
			"    var lexer = new <lexerName>.<lexerName>(input);\n" +
		    "    var stream = new antlr4.CommonTokenStream(lexer);\n" +
			"    stream.fill();\n" +
		    "    for(var i=0; i\\<stream.tokens.length; i++) {\n" +
		    "		console.log(stream.tokens[i].toString());\n" +
			"    }\n" +
			(showDFA ? 
			"    process.stdout.write(lexer._interp.decisionToDFA[antlr4.Lexer.DEFAULT_MODE].toLexerString());\n"
				 :"") +
			"}\n" +
			"\n" +
			"main(process.argv);\n" +
			"\n");
		outputFileST.add("lexerName", lexerName);
		writeFile(tmpdir, "Test.js", outputFileST.render());
	}

	public void writeRecognizer(String parserName, String lexerName,
								String listenerName, String visitorName,
								String parserStartRuleName, boolean debug) {
		if ( parserName==null ) {
			writeLexerTestFile(lexerName, debug);
		}
		else {
			writeParserTestFile(parserName,
						  lexerName,
						  listenerName,
						  visitorName,
						  parserStartRuleName,
						  debug);
		}
	}
}
