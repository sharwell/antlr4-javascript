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
package org.antlr.v4.js.safari.test;

import static org.junit.Assert.assertTrue;
import static org.junit.Assume.assumeTrue;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.antlr.v4.Tool;
import org.antlr.v4.js.test.CommonBaseTest;
import org.antlr.v4.test.ErrorQueue;
import org.antlr.v4.tool.ANTLRMessage;
import org.antlr.v4.tool.DefaultToolListener;
import org.eclipse.jetty.server.Handler;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.handler.DefaultHandler;
import org.eclipse.jetty.server.handler.HandlerList;
import org.eclipse.jetty.server.handler.ResourceHandler;
import org.openqa.selenium.By.ById;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.safari.SafariDriver;
import org.stringtemplate.v4.ST;

public abstract class BaseTest extends CommonBaseTest {

	@Override
	public void setUp() throws Exception {
		assumeTrue(isMac());
		super.setUp();
	}

	/** Return true if all is ok, no errors */
	@Override
	protected ErrorQueue antlr(String fileName, String grammarFileName, String grammarStr, boolean defaultListener, String... extraOptions) {
		String parserDir = tmpdir + "/parser";
		System.out.println("dir "+parserDir);
		mkdir(parserDir);
		writeFile(parserDir, fileName, grammarStr);
		final List<String> options = new ArrayList<String>();
		Collections.addAll(options, extraOptions);
		options.add("-Dlanguage=JavaScript");
		options.add("-o");
		options.add(parserDir);
		options.add("-lib");
		options.add(parserDir);
		options.add(new File(parserDir,grammarFileName).toString());

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
							   String input) throws Exception
	{
		boolean success = rawGenerateAndBuildRecognizer(grammarFileName,
									  grammarStr,
									  null,
									  lexerName,"-no-listener");
		assertTrue(success);
		writeLexerTestFile(lexerName);
		String output = execHtmlPage("Test.html", input);
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
								String input) throws Exception
	{
		boolean success = rawGenerateAndBuildRecognizer(grammarFileName,
														grammarStr,
														parserName,
														lexerName,
														"-visitor");
		assertTrue(success);
		rawBuildRecognizerTestFile(parserName,
								 lexerName,
								 listenerName,
								 visitorName,
								 startRuleName);
		return execRecognizer(input);
	}

	protected void rawBuildRecognizerTestFile(String parserName,
									   String lexerName,
									   String listenerName,
									   String visitorName,
									   String parserStartRuleName)
	{
        this.stderrDuringParse = null;
		if ( parserName==null ) {
			writeLexerTestFile(lexerName);
		}
		else {
			writeParserTestFile(parserName,
						  lexerName,
						  listenerName,
						  visitorName,
						  parserStartRuleName);
		}
	}

	public String execRecognizer(String input) throws Exception {
		return execHtmlPage("Test.html", input);
	}

	public String execHtmlPage(String fileName, String input) throws Exception {
		String runtimePath = locateRuntime();
		Server server = new Server(8080);
		try {
			// start Jetty, since 'file' protocol is not supported by Selenium Safari driver
			ResourceHandler rh1 = new ResourceHandler();
			rh1.setDirectoriesListed(false);
			rh1.setResourceBase(tmpdir);
			rh1.setWelcomeFiles(new String[] { fileName });
			ResourceHandler rh2 = new ResourceHandler();
			rh2.setDirectoriesListed(false);
			rh2.setResourceBase(runtimePath);
			HandlerList handlers = new HandlerList();
			handlers.setHandlers(new Handler[] { rh1, rh2, new DefaultHandler() });
			server.setHandler(handlers);
			server.start();
			// System.setProperty("webdriver.safari.driver", "/Users/ericvergnaud/Desktop/TestSafari/org/openqa/selenium/safari/SafariDriver.safariextz");
			System.setProperty("webdriver.safari.noinstall", "true");
			WebDriver driver = new SafariDriver();
			try {
				driver.get("http://localhost:8080/" + fileName);
				driver.findElement(new ById("input")).sendKeys(input);
				driver.findElement(new ById("submit")).click();
				String errors = driver.findElement(new ById("errors")).getText();
				if(errors!=null && errors.length()>0) {
					this.stderrDuringParse = errors;
					System.err.print(errors);
				}
				return driver.findElement(new ById("output")).getAttribute("value");
			} finally {
				driver.close();
			}
		}
		catch (Exception e) {
			System.err.println("can't exec recognizer");
			e.printStackTrace(System.err);
		} finally {
			server.stop();
		}
		return null;
	}

	protected void writeParserTestFile(String parserName,
			 String lexerName,
			 String listenerName,
			 String visitorName,
			 String parserStartRuleName) {
		String html = "<!DOCTYPE html>\r\n" +
		"<html>\r\n" +
	    "	<head>\r\n" +
        "		<script src='lib/require.js'></script>\r\n" +
        "		<script>\r\n" +
        "			antlr4 = null;\r\n" +
        "			TreeShapeListener = null;\r\n" +
        "			" + lexerName + " = null;\r\n" +
        "			" + parserName + " = null;\r\n" +
        "			" + listenerName + " = null;\r\n" +
        "			" + visitorName + " = null;\r\n" +
		"\r\n" +			
		"			loadParser = function() {\r\n" +			
        "				try {\r\n" +
        "					antlr4 = require('antlr4/index');\r\n" +
		"					" + lexerName + " = require('./parser/" + lexerName + "');\n" +
		"					" + parserName + " = require('./parser/" + parserName + "');\n" +
		"					" + listenerName + " = require('./parser/" + listenerName + "');\n" +
		"					" + visitorName + " = require('./parser/" + visitorName + "');\n" +
        "				} catch (ex) {\r\n" +
        "					document.getElementById('errors').value = ex.toString();\r\n" +
        "				}\r\n" +
		"\r\n" +			
        "				TreeShapeListener = function() {\r\n" +
        "					antlr4.tree.ParseTreeListener.call(this);\r\n" +
        "					return this;\r\n" +
        "				};\r\n" +
		"\r\n" +			
		"				TreeShapeListener.prototype = Object.create(antlr4.tree.ParseTreeListener.prototype);\r\n" +
		"				TreeShapeListener.prototype.constructor = TreeShapeListener;\r\n" +
		"\r\n" +			
		"				TreeShapeListener.prototype.enterEveryRule = function(ctx) {\r\n" +
		"					for(var i=0;i<ctx.getChildCount; i++) {\r\n" +
		"						var child = ctx.getChild(i);\r\n" +
		"						var parent = child.parentCtx;\r\n" +
		"						if(parent.getRuleContext() !== ctx || !(parent instanceof antlr4.tree.RuleNode)) {\r\n" +
		"							throw 'Invalid parse tree shape detected.';\r\n" +
		"						}\r\n" +
		"					}\r\n" +
		"				};\r\n" +
		"			}\r\n" +
		"\r\n" +			
		"			test = function() {\r\n" +
		"				document.getElementById('output').value = ''\r\n" +
		"				var input = document.getElementById('input').value;\r\n" +
		"    			var stream = new antlr4.InputStream(input);\n" +
		"    			var lexer = new " + lexerName + "." + lexerName + "(stream);\n" +
	    "    			var tokens = new antlr4.CommonTokenStream(lexer);\n" +
	    "				var parser = new " + parserName + "." + parserName + "(tokens);\n" +
	    "    			parser.buildParseTrees = true;\n" +
		"    			var tree = parser." + parserStartRuleName + "();\n" +
		"    			antlr4.tree.ParseTreeWalker.DEFAULT.walk(new TreeShapeListener(), tree);\n" +
		"			};\r\n" +
		"\r\n" +			
        "		</script>\r\n" +
	    "	</head>\r\n" +
	    "	<body>\r\n" +
	    "		<textarea id='input'></textarea><br>\r\n" +
	    "		<button id='submit' type='button' onclick='test()'>Test</button><br>\r\n" +
	    "		<textarea id='output'></textarea><br>\r\n" +
	    "		<textarea id='errors'></textarea><br>\r\n" +
	    "		<script>loadParser();</script>\r\n" +	
	    "	</body>\r\n" +
	    "</html>\r\n";
		writeFile(tmpdir, "Test.html", html);	
	};
	
	
	protected void writeLexerTestFile(String lexerName) {
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
			"}\n" +
			"\n" +
			"main(process.argv);\n" +
			"\n");
		outputFileST.add("lexerName", lexerName);
		writeFile(tmpdir, "Test.js", outputFileST.render());
	}

	public void writeRecognizer(String parserName, String lexerName,
								String listenerName, String visitorName,
								String parserStartRuleName) {
		if ( parserName==null ) {
			writeLexerTestFile(lexerName);
		}
		else {
			writeParserTestFile(parserName,
						  lexerName,
						  listenerName,
						  visitorName,
						  parserStartRuleName);
		}
	}
}
