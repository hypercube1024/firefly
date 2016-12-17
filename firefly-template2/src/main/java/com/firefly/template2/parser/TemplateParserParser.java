// Generated from /Users/qiupengtao/Develop/github_project/firefly/firefly-template2/src/main/java/com/firefly/template2/parser/TemplateParser.g4 by ANTLR 4.5.3
package com.firefly.template2.parser;
import org.antlr.v4.runtime.atn.*;
import org.antlr.v4.runtime.dfa.DFA;
import org.antlr.v4.runtime.*;
import org.antlr.v4.runtime.misc.*;
import org.antlr.v4.runtime.tree.*;
import java.util.List;
import java.util.Iterator;
import java.util.ArrayList;

@SuppressWarnings({"all", "warnings", "unchecked", "unused", "cast"})
public class TemplateParserParser extends Parser {
	static { RuntimeMetaData.checkVersion("4.5.3", RuntimeMetaData.VERSION); }

	protected static final DFA[] _decisionToDFA;
	protected static final PredictionContextCache _sharedContextCache =
		new PredictionContextCache();
	public static final int
		T__0=1, T__1=2, T__2=3, T__3=4, EXTENDS=5, INCLUDE=6, SET=7, MAIN=8, FUNCTION=9, 
		IF=10, ELSE=11, THEN_IF=12, FOR=13, WHILE=14, SWITCH=15, CASE=16, BREAK=17, 
		DEFAULT=18, END=19, THIS=20, OutputString=21, OutputStringWithNewLine=22, 
		OutputNewLine=23, COMMENT=24, LINE_COMMENT=25, IntegerLiteral=26, FloatingPointLiteral=27, 
		BooleanLiteral=28, StringLiteral=29, NullLiteral=30, LPAREN=31, RPAREN=32, 
		LBRACK=33, RBRACK=34, DOT=35, ASSIGN=36, GT=37, LT=38, BANG=39, TILDE=40, 
		QUESTION=41, COLON=42, EQUAL=43, LE=44, GE=45, NOTEQUAL=46, AND=47, OR=48, 
		INC=49, DEC=50, ADD=51, SUB=52, MUL=53, DIV=54, BITAND=55, BITOR=56, CARET=57, 
		MOD=58, ARROW=59, COLONCOLON=60, LSHIFT=61, RSHIFT=62, URSHIFT=63, ADD_ASSIGN=64, 
		SUB_ASSIGN=65, MUL_ASSIGN=66, DIV_ASSIGN=67, AND_ASSIGN=68, OR_ASSIGN=69, 
		XOR_ASSIGN=70, MOD_ASSIGN=71, LSHIFT_ASSIGN=72, RSHIFT_ASSIGN=73, URSHIFT_ASSIGN=74, 
		Identifier=75, WS=76;
	public static final int
		RULE_program = 0, RULE_templateBody = 1, RULE_extends = 2, RULE_include = 3, 
		RULE_set = 4, RULE_templatePath = 5, RULE_mainFunction = 6, RULE_functionDeclaration = 7, 
		RULE_functionParameters = 8, RULE_selection = 9, RULE_switch = 10, RULE_whileLoop = 11, 
		RULE_forLoop = 12, RULE_expression = 13, RULE_beanAccess = 14, RULE_objectAccess = 15, 
		RULE_propertyAccess = 16, RULE_arrayAccess = 17, RULE_mapAccess = 18, 
		RULE_methodCall = 19, RULE_callMethodParameters = 20;
	public static final String[] ruleNames = {
		"program", "templateBody", "extends", "include", "set", "templatePath", 
		"mainFunction", "functionDeclaration", "functionParameters", "selection", 
		"switch", "whileLoop", "forLoop", "expression", "beanAccess", "objectAccess", 
		"propertyAccess", "arrayAccess", "mapAccess", "methodCall", "callMethodParameters"
	};

	private static final String[] _LITERAL_NAMES = {
		null, "';'", "','", "'${'", "'}'", "'#extends'", "'#include'", "'#set'", 
		"'#main'", "'#function'", "'#if'", "'#else'", "'if'", "'#for'", "'#while'", 
		"'#switch'", "'#case'", "'#break'", "'#default'", "'#end'", "'this'", 
		null, null, "'&nl;'", null, null, null, null, null, null, "'null'", "'('", 
		"')'", "'['", "']'", "'.'", "'='", "'>'", "'<'", "'!'", "'~'", "'?'", 
		"':'", "'=='", "'<='", "'>='", "'!='", "'&&'", "'||'", "'++'", "'--'", 
		"'+'", "'-'", "'*'", "'/'", "'&'", "'|'", "'^'", "'%'", "'->'", "'::'", 
		"'<<'", "'>>'", "'>>>'", "'+='", "'-='", "'*='", "'/='", "'&='", "'|='", 
		"'^='", "'%='", "'<<='", "'>>='", "'>>>='"
	};
	private static final String[] _SYMBOLIC_NAMES = {
		null, null, null, null, null, "EXTENDS", "INCLUDE", "SET", "MAIN", "FUNCTION", 
		"IF", "ELSE", "THEN_IF", "FOR", "WHILE", "SWITCH", "CASE", "BREAK", "DEFAULT", 
		"END", "THIS", "OutputString", "OutputStringWithNewLine", "OutputNewLine", 
		"COMMENT", "LINE_COMMENT", "IntegerLiteral", "FloatingPointLiteral", "BooleanLiteral", 
		"StringLiteral", "NullLiteral", "LPAREN", "RPAREN", "LBRACK", "RBRACK", 
		"DOT", "ASSIGN", "GT", "LT", "BANG", "TILDE", "QUESTION", "COLON", "EQUAL", 
		"LE", "GE", "NOTEQUAL", "AND", "OR", "INC", "DEC", "ADD", "SUB", "MUL", 
		"DIV", "BITAND", "BITOR", "CARET", "MOD", "ARROW", "COLONCOLON", "LSHIFT", 
		"RSHIFT", "URSHIFT", "ADD_ASSIGN", "SUB_ASSIGN", "MUL_ASSIGN", "DIV_ASSIGN", 
		"AND_ASSIGN", "OR_ASSIGN", "XOR_ASSIGN", "MOD_ASSIGN", "LSHIFT_ASSIGN", 
		"RSHIFT_ASSIGN", "URSHIFT_ASSIGN", "Identifier", "WS"
	};
	public static final Vocabulary VOCABULARY = new VocabularyImpl(_LITERAL_NAMES, _SYMBOLIC_NAMES);

	/**
	 * @deprecated Use {@link #VOCABULARY} instead.
	 */
	@Deprecated
	public static final String[] tokenNames;
	static {
		tokenNames = new String[_SYMBOLIC_NAMES.length];
		for (int i = 0; i < tokenNames.length; i++) {
			tokenNames[i] = VOCABULARY.getLiteralName(i);
			if (tokenNames[i] == null) {
				tokenNames[i] = VOCABULARY.getSymbolicName(i);
			}

			if (tokenNames[i] == null) {
				tokenNames[i] = "<INVALID>";
			}
		}
	}

	@Override
	@Deprecated
	public String[] getTokenNames() {
		return tokenNames;
	}

	@Override

	public Vocabulary getVocabulary() {
		return VOCABULARY;
	}

	@Override
	public String getGrammarFileName() { return "TemplateParser.g4"; }

	@Override
	public String[] getRuleNames() { return ruleNames; }

	@Override
	public String getSerializedATN() { return _serializedATN; }

	@Override
	public ATN getATN() { return _ATN; }

	public TemplateParserParser(TokenStream input) {
		super(input);
		_interp = new ParserATNSimulator(this,_ATN,_decisionToDFA,_sharedContextCache);
	}
	public static class ProgramContext extends ParserRuleContext {
		public MainFunctionContext mainFunction() {
			return getRuleContext(MainFunctionContext.class,0);
		}
		public ExtendsContext extends() {
			return getRuleContext(ExtendsContext.class,0);
		}
		public List<FunctionDeclarationContext> functionDeclaration() {
			return getRuleContexts(FunctionDeclarationContext.class);
		}
		public FunctionDeclarationContext functionDeclaration(int i) {
			return getRuleContext(FunctionDeclarationContext.class,i);
		}
		public ProgramContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_program; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof TemplateParserListener ) ((TemplateParserListener)listener).enterProgram(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof TemplateParserListener ) ((TemplateParserListener)listener).exitProgram(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof TemplateParserVisitor ) return ((TemplateParserVisitor<? extends T>)visitor).visitProgram(this);
			else return visitor.visitChildren(this);
		}
	}

	public final ProgramContext program() throws RecognitionException {
		ProgramContext _localctx = new ProgramContext(_ctx, getState());
		enterRule(_localctx, 0, RULE_program);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(43);
			_la = _input.LA(1);
			if (_la==EXTENDS) {
				{
				setState(42);
				extends();
				}
			}

			setState(45);
			mainFunction();
			setState(49);
			_errHandler.sync(this);
			_la = _input.LA(1);
			while (_la==FUNCTION) {
				{
				{
				setState(46);
				functionDeclaration();
				}
				}
				setState(51);
				_errHandler.sync(this);
				_la = _input.LA(1);
			}
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	public static class TemplateBodyContext extends ParserRuleContext {
		public List<TerminalNode> OutputString() { return getTokens(TemplateParserParser.OutputString); }
		public TerminalNode OutputString(int i) {
			return getToken(TemplateParserParser.OutputString, i);
		}
		public List<TerminalNode> OutputStringWithNewLine() { return getTokens(TemplateParserParser.OutputStringWithNewLine); }
		public TerminalNode OutputStringWithNewLine(int i) {
			return getToken(TemplateParserParser.OutputStringWithNewLine, i);
		}
		public List<TerminalNode> OutputNewLine() { return getTokens(TemplateParserParser.OutputNewLine); }
		public TerminalNode OutputNewLine(int i) {
			return getToken(TemplateParserParser.OutputNewLine, i);
		}
		public List<SetContext> set() {
			return getRuleContexts(SetContext.class);
		}
		public SetContext set(int i) {
			return getRuleContext(SetContext.class,i);
		}
		public List<IncludeContext> include() {
			return getRuleContexts(IncludeContext.class);
		}
		public IncludeContext include(int i) {
			return getRuleContext(IncludeContext.class,i);
		}
		public List<SelectionContext> selection() {
			return getRuleContexts(SelectionContext.class);
		}
		public SelectionContext selection(int i) {
			return getRuleContext(SelectionContext.class,i);
		}
		public List<SwitchContext> switch() {
			return getRuleContexts(SwitchContext.class);
		}
		public SwitchContext switch(int i) {
			return getRuleContext(SwitchContext.class,i);
		}
		public List<WhileLoopContext> whileLoop() {
			return getRuleContexts(WhileLoopContext.class);
		}
		public WhileLoopContext whileLoop(int i) {
			return getRuleContext(WhileLoopContext.class,i);
		}
		public List<ForLoopContext> forLoop() {
			return getRuleContexts(ForLoopContext.class);
		}
		public ForLoopContext forLoop(int i) {
			return getRuleContext(ForLoopContext.class,i);
		}
		public List<BeanAccessContext> beanAccess() {
			return getRuleContexts(BeanAccessContext.class);
		}
		public BeanAccessContext beanAccess(int i) {
			return getRuleContext(BeanAccessContext.class,i);
		}
		public TemplateBodyContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_templateBody; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof TemplateParserListener ) ((TemplateParserListener)listener).enterTemplateBody(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof TemplateParserListener ) ((TemplateParserListener)listener).exitTemplateBody(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof TemplateParserVisitor ) return ((TemplateParserVisitor<? extends T>)visitor).visitTemplateBody(this);
			else return visitor.visitChildren(this);
		}
	}

	public final TemplateBodyContext templateBody() throws RecognitionException {
		TemplateBodyContext _localctx = new TemplateBodyContext(_ctx, getState());
		enterRule(_localctx, 2, RULE_templateBody);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(64);
			_errHandler.sync(this);
			_la = _input.LA(1);
			while ((((_la) & ~0x3f) == 0 && ((1L << _la) & ((1L << T__2) | (1L << INCLUDE) | (1L << SET) | (1L << IF) | (1L << FOR) | (1L << WHILE) | (1L << SWITCH) | (1L << OutputString) | (1L << OutputStringWithNewLine) | (1L << OutputNewLine))) != 0)) {
				{
				setState(62);
				switch (_input.LA(1)) {
				case OutputString:
					{
					setState(52);
					match(OutputString);
					}
					break;
				case OutputStringWithNewLine:
					{
					setState(53);
					match(OutputStringWithNewLine);
					}
					break;
				case OutputNewLine:
					{
					setState(54);
					match(OutputNewLine);
					}
					break;
				case SET:
					{
					setState(55);
					set();
					}
					break;
				case INCLUDE:
					{
					setState(56);
					include();
					}
					break;
				case IF:
					{
					setState(57);
					selection();
					}
					break;
				case SWITCH:
					{
					setState(58);
					switch();
					}
					break;
				case WHILE:
					{
					setState(59);
					whileLoop();
					}
					break;
				case FOR:
					{
					setState(60);
					forLoop();
					}
					break;
				case T__2:
					{
					setState(61);
					beanAccess();
					}
					break;
				default:
					throw new NoViableAltException(this);
				}
				}
				setState(66);
				_errHandler.sync(this);
				_la = _input.LA(1);
			}
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	public static class ExtendsContext extends ParserRuleContext {
		public TerminalNode EXTENDS() { return getToken(TemplateParserParser.EXTENDS, 0); }
		public TemplatePathContext templatePath() {
			return getRuleContext(TemplatePathContext.class,0);
		}
		public ExtendsContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_extends; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof TemplateParserListener ) ((TemplateParserListener)listener).enterExtends(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof TemplateParserListener ) ((TemplateParserListener)listener).exitExtends(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof TemplateParserVisitor ) return ((TemplateParserVisitor<? extends T>)visitor).visitExtends(this);
			else return visitor.visitChildren(this);
		}
	}

	public final ExtendsContext extends() throws RecognitionException {
		ExtendsContext _localctx = new ExtendsContext(_ctx, getState());
		enterRule(_localctx, 4, RULE_extends);
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(67);
			match(EXTENDS);
			setState(68);
			templatePath();
			setState(69);
			match(T__0);
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	public static class IncludeContext extends ParserRuleContext {
		public TerminalNode INCLUDE() { return getToken(TemplateParserParser.INCLUDE, 0); }
		public TerminalNode THIS() { return getToken(TemplateParserParser.THIS, 0); }
		public TemplatePathContext templatePath() {
			return getRuleContext(TemplatePathContext.class,0);
		}
		public MethodCallContext methodCall() {
			return getRuleContext(MethodCallContext.class,0);
		}
		public IncludeContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_include; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof TemplateParserListener ) ((TemplateParserListener)listener).enterInclude(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof TemplateParserListener ) ((TemplateParserListener)listener).exitInclude(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof TemplateParserVisitor ) return ((TemplateParserVisitor<? extends T>)visitor).visitInclude(this);
			else return visitor.visitChildren(this);
		}
	}

	public final IncludeContext include() throws RecognitionException {
		IncludeContext _localctx = new IncludeContext(_ctx, getState());
		enterRule(_localctx, 6, RULE_include);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(71);
			match(INCLUDE);
			setState(72);
			match(LPAREN);
			setState(75);
			switch (_input.LA(1)) {
			case THIS:
				{
				setState(73);
				match(THIS);
				}
				break;
			case Identifier:
				{
				setState(74);
				templatePath();
				}
				break;
			default:
				throw new NoViableAltException(this);
			}
			setState(78);
			_la = _input.LA(1);
			if (_la==DOT) {
				{
				setState(77);
				methodCall();
				}
			}

			setState(80);
			match(RPAREN);
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	public static class SetContext extends ParserRuleContext {
		public TerminalNode SET() { return getToken(TemplateParserParser.SET, 0); }
		public TerminalNode Identifier() { return getToken(TemplateParserParser.Identifier, 0); }
		public ExpressionContext expression() {
			return getRuleContext(ExpressionContext.class,0);
		}
		public SetContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_set; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof TemplateParserListener ) ((TemplateParserListener)listener).enterSet(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof TemplateParserListener ) ((TemplateParserListener)listener).exitSet(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof TemplateParserVisitor ) return ((TemplateParserVisitor<? extends T>)visitor).visitSet(this);
			else return visitor.visitChildren(this);
		}
	}

	public final SetContext set() throws RecognitionException {
		SetContext _localctx = new SetContext(_ctx, getState());
		enterRule(_localctx, 8, RULE_set);
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(82);
			match(SET);
			setState(83);
			match(LPAREN);
			setState(84);
			match(Identifier);
			setState(85);
			match(ASSIGN);
			setState(86);
			expression(0);
			setState(87);
			match(RPAREN);
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	public static class TemplatePathContext extends ParserRuleContext {
		public List<TerminalNode> Identifier() { return getTokens(TemplateParserParser.Identifier); }
		public TerminalNode Identifier(int i) {
			return getToken(TemplateParserParser.Identifier, i);
		}
		public TemplatePathContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_templatePath; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof TemplateParserListener ) ((TemplateParserListener)listener).enterTemplatePath(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof TemplateParserListener ) ((TemplateParserListener)listener).exitTemplatePath(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof TemplateParserVisitor ) return ((TemplateParserVisitor<? extends T>)visitor).visitTemplatePath(this);
			else return visitor.visitChildren(this);
		}
	}

	public final TemplatePathContext templatePath() throws RecognitionException {
		TemplatePathContext _localctx = new TemplatePathContext(_ctx, getState());
		enterRule(_localctx, 10, RULE_templatePath);
		try {
			int _alt;
			enterOuterAlt(_localctx, 1);
			{
			setState(89);
			match(Identifier);
			setState(94);
			_errHandler.sync(this);
			_alt = getInterpreter().adaptivePredict(_input,6,_ctx);
			while ( _alt!=2 && _alt!=org.antlr.v4.runtime.atn.ATN.INVALID_ALT_NUMBER ) {
				if ( _alt==1 ) {
					{
					{
					setState(90);
					match(DOT);
					setState(91);
					match(Identifier);
					}
					} 
				}
				setState(96);
				_errHandler.sync(this);
				_alt = getInterpreter().adaptivePredict(_input,6,_ctx);
			}
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	public static class MainFunctionContext extends ParserRuleContext {
		public TerminalNode MAIN() { return getToken(TemplateParserParser.MAIN, 0); }
		public TemplateBodyContext templateBody() {
			return getRuleContext(TemplateBodyContext.class,0);
		}
		public TerminalNode END() { return getToken(TemplateParserParser.END, 0); }
		public MainFunctionContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_mainFunction; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof TemplateParserListener ) ((TemplateParserListener)listener).enterMainFunction(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof TemplateParserListener ) ((TemplateParserListener)listener).exitMainFunction(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof TemplateParserVisitor ) return ((TemplateParserVisitor<? extends T>)visitor).visitMainFunction(this);
			else return visitor.visitChildren(this);
		}
	}

	public final MainFunctionContext mainFunction() throws RecognitionException {
		MainFunctionContext _localctx = new MainFunctionContext(_ctx, getState());
		enterRule(_localctx, 12, RULE_mainFunction);
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(97);
			match(MAIN);
			setState(98);
			templateBody();
			setState(99);
			match(END);
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	public static class FunctionDeclarationContext extends ParserRuleContext {
		public TerminalNode FUNCTION() { return getToken(TemplateParserParser.FUNCTION, 0); }
		public TerminalNode Identifier() { return getToken(TemplateParserParser.Identifier, 0); }
		public FunctionParametersContext functionParameters() {
			return getRuleContext(FunctionParametersContext.class,0);
		}
		public TemplateBodyContext templateBody() {
			return getRuleContext(TemplateBodyContext.class,0);
		}
		public TerminalNode END() { return getToken(TemplateParserParser.END, 0); }
		public FunctionDeclarationContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_functionDeclaration; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof TemplateParserListener ) ((TemplateParserListener)listener).enterFunctionDeclaration(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof TemplateParserListener ) ((TemplateParserListener)listener).exitFunctionDeclaration(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof TemplateParserVisitor ) return ((TemplateParserVisitor<? extends T>)visitor).visitFunctionDeclaration(this);
			else return visitor.visitChildren(this);
		}
	}

	public final FunctionDeclarationContext functionDeclaration() throws RecognitionException {
		FunctionDeclarationContext _localctx = new FunctionDeclarationContext(_ctx, getState());
		enterRule(_localctx, 14, RULE_functionDeclaration);
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(101);
			match(FUNCTION);
			setState(102);
			match(Identifier);
			setState(103);
			functionParameters();
			setState(104);
			templateBody();
			setState(105);
			match(END);
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	public static class FunctionParametersContext extends ParserRuleContext {
		public List<TerminalNode> Identifier() { return getTokens(TemplateParserParser.Identifier); }
		public TerminalNode Identifier(int i) {
			return getToken(TemplateParserParser.Identifier, i);
		}
		public FunctionParametersContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_functionParameters; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof TemplateParserListener ) ((TemplateParserListener)listener).enterFunctionParameters(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof TemplateParserListener ) ((TemplateParserListener)listener).exitFunctionParameters(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof TemplateParserVisitor ) return ((TemplateParserVisitor<? extends T>)visitor).visitFunctionParameters(this);
			else return visitor.visitChildren(this);
		}
	}

	public final FunctionParametersContext functionParameters() throws RecognitionException {
		FunctionParametersContext _localctx = new FunctionParametersContext(_ctx, getState());
		enterRule(_localctx, 16, RULE_functionParameters);
		int _la;
		try {
			setState(119);
			_errHandler.sync(this);
			switch ( getInterpreter().adaptivePredict(_input,8,_ctx) ) {
			case 1:
				enterOuterAlt(_localctx, 1);
				{
				setState(107);
				match(LPAREN);
				setState(108);
				match(RPAREN);
				}
				break;
			case 2:
				enterOuterAlt(_localctx, 2);
				{
				setState(109);
				match(LPAREN);
				setState(110);
				match(Identifier);
				setState(115);
				_errHandler.sync(this);
				_la = _input.LA(1);
				while (_la==T__1) {
					{
					{
					setState(111);
					match(T__1);
					setState(112);
					match(Identifier);
					}
					}
					setState(117);
					_errHandler.sync(this);
					_la = _input.LA(1);
				}
				setState(118);
				match(RPAREN);
				}
				break;
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	public static class SelectionContext extends ParserRuleContext {
		public TerminalNode IF() { return getToken(TemplateParserParser.IF, 0); }
		public List<ExpressionContext> expression() {
			return getRuleContexts(ExpressionContext.class);
		}
		public ExpressionContext expression(int i) {
			return getRuleContext(ExpressionContext.class,i);
		}
		public List<TemplateBodyContext> templateBody() {
			return getRuleContexts(TemplateBodyContext.class);
		}
		public TemplateBodyContext templateBody(int i) {
			return getRuleContext(TemplateBodyContext.class,i);
		}
		public TerminalNode END() { return getToken(TemplateParserParser.END, 0); }
		public List<TerminalNode> ELSE() { return getTokens(TemplateParserParser.ELSE); }
		public TerminalNode ELSE(int i) {
			return getToken(TemplateParserParser.ELSE, i);
		}
		public List<TerminalNode> THEN_IF() { return getTokens(TemplateParserParser.THEN_IF); }
		public TerminalNode THEN_IF(int i) {
			return getToken(TemplateParserParser.THEN_IF, i);
		}
		public SelectionContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_selection; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof TemplateParserListener ) ((TemplateParserListener)listener).enterSelection(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof TemplateParserListener ) ((TemplateParserListener)listener).exitSelection(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof TemplateParserVisitor ) return ((TemplateParserVisitor<? extends T>)visitor).visitSelection(this);
			else return visitor.visitChildren(this);
		}
	}

	public final SelectionContext selection() throws RecognitionException {
		SelectionContext _localctx = new SelectionContext(_ctx, getState());
		enterRule(_localctx, 18, RULE_selection);
		int _la;
		try {
			int _alt;
			enterOuterAlt(_localctx, 1);
			{
			setState(121);
			match(IF);
			setState(122);
			match(LPAREN);
			setState(123);
			expression(0);
			setState(124);
			match(RPAREN);
			setState(125);
			templateBody();
			setState(135);
			_errHandler.sync(this);
			_alt = getInterpreter().adaptivePredict(_input,9,_ctx);
			while ( _alt!=2 && _alt!=org.antlr.v4.runtime.atn.ATN.INVALID_ALT_NUMBER ) {
				if ( _alt==1 ) {
					{
					{
					setState(126);
					match(ELSE);
					setState(127);
					match(THEN_IF);
					setState(128);
					match(LPAREN);
					setState(129);
					expression(0);
					setState(130);
					match(RPAREN);
					setState(131);
					templateBody();
					}
					} 
				}
				setState(137);
				_errHandler.sync(this);
				_alt = getInterpreter().adaptivePredict(_input,9,_ctx);
			}
			setState(140);
			_la = _input.LA(1);
			if (_la==ELSE) {
				{
				setState(138);
				match(ELSE);
				setState(139);
				templateBody();
				}
			}

			setState(142);
			match(END);
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	public static class SwitchContext extends ParserRuleContext {
		public TerminalNode SWITCH() { return getToken(TemplateParserParser.SWITCH, 0); }
		public BeanAccessContext beanAccess() {
			return getRuleContext(BeanAccessContext.class,0);
		}
		public TerminalNode END() { return getToken(TemplateParserParser.END, 0); }
		public List<TerminalNode> CASE() { return getTokens(TemplateParserParser.CASE); }
		public TerminalNode CASE(int i) {
			return getToken(TemplateParserParser.CASE, i);
		}
		public List<TemplateBodyContext> templateBody() {
			return getRuleContexts(TemplateBodyContext.class);
		}
		public TemplateBodyContext templateBody(int i) {
			return getRuleContext(TemplateBodyContext.class,i);
		}
		public List<TerminalNode> BREAK() { return getTokens(TemplateParserParser.BREAK); }
		public TerminalNode BREAK(int i) {
			return getToken(TemplateParserParser.BREAK, i);
		}
		public TerminalNode DEFAULT() { return getToken(TemplateParserParser.DEFAULT, 0); }
		public List<TerminalNode> StringLiteral() { return getTokens(TemplateParserParser.StringLiteral); }
		public TerminalNode StringLiteral(int i) {
			return getToken(TemplateParserParser.StringLiteral, i);
		}
		public List<TerminalNode> IntegerLiteral() { return getTokens(TemplateParserParser.IntegerLiteral); }
		public TerminalNode IntegerLiteral(int i) {
			return getToken(TemplateParserParser.IntegerLiteral, i);
		}
		public SwitchContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_switch; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof TemplateParserListener ) ((TemplateParserListener)listener).enterSwitch(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof TemplateParserListener ) ((TemplateParserListener)listener).exitSwitch(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof TemplateParserVisitor ) return ((TemplateParserVisitor<? extends T>)visitor).visitSwitch(this);
			else return visitor.visitChildren(this);
		}
	}

	public final SwitchContext switch() throws RecognitionException {
		SwitchContext _localctx = new SwitchContext(_ctx, getState());
		enterRule(_localctx, 20, RULE_switch);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(144);
			match(SWITCH);
			setState(145);
			match(LPAREN);
			setState(146);
			beanAccess();
			setState(147);
			match(RPAREN);
			setState(153); 
			_errHandler.sync(this);
			_la = _input.LA(1);
			do {
				{
				{
				setState(148);
				match(CASE);
				setState(149);
				_la = _input.LA(1);
				if ( !(_la==IntegerLiteral || _la==StringLiteral) ) {
				_errHandler.recoverInline(this);
				} else {
					consume();
				}
				setState(150);
				templateBody();
				setState(151);
				match(BREAK);
				}
				}
				setState(155); 
				_errHandler.sync(this);
				_la = _input.LA(1);
			} while ( _la==CASE );
			setState(161);
			_la = _input.LA(1);
			if (_la==DEFAULT) {
				{
				setState(157);
				match(DEFAULT);
				setState(158);
				templateBody();
				setState(159);
				match(BREAK);
				}
			}

			setState(163);
			match(END);
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	public static class WhileLoopContext extends ParserRuleContext {
		public TerminalNode WHILE() { return getToken(TemplateParserParser.WHILE, 0); }
		public ExpressionContext expression() {
			return getRuleContext(ExpressionContext.class,0);
		}
		public TemplateBodyContext templateBody() {
			return getRuleContext(TemplateBodyContext.class,0);
		}
		public TerminalNode END() { return getToken(TemplateParserParser.END, 0); }
		public WhileLoopContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_whileLoop; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof TemplateParserListener ) ((TemplateParserListener)listener).enterWhileLoop(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof TemplateParserListener ) ((TemplateParserListener)listener).exitWhileLoop(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof TemplateParserVisitor ) return ((TemplateParserVisitor<? extends T>)visitor).visitWhileLoop(this);
			else return visitor.visitChildren(this);
		}
	}

	public final WhileLoopContext whileLoop() throws RecognitionException {
		WhileLoopContext _localctx = new WhileLoopContext(_ctx, getState());
		enterRule(_localctx, 22, RULE_whileLoop);
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(165);
			match(WHILE);
			setState(166);
			match(LPAREN);
			setState(167);
			expression(0);
			setState(168);
			match(RPAREN);
			setState(169);
			templateBody();
			setState(170);
			match(END);
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	public static class ForLoopContext extends ParserRuleContext {
		public TerminalNode FOR() { return getToken(TemplateParserParser.FOR, 0); }
		public TerminalNode Identifier() { return getToken(TemplateParserParser.Identifier, 0); }
		public BeanAccessContext beanAccess() {
			return getRuleContext(BeanAccessContext.class,0);
		}
		public TemplateBodyContext templateBody() {
			return getRuleContext(TemplateBodyContext.class,0);
		}
		public TerminalNode END() { return getToken(TemplateParserParser.END, 0); }
		public ForLoopContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_forLoop; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof TemplateParserListener ) ((TemplateParserListener)listener).enterForLoop(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof TemplateParserListener ) ((TemplateParserListener)listener).exitForLoop(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof TemplateParserVisitor ) return ((TemplateParserVisitor<? extends T>)visitor).visitForLoop(this);
			else return visitor.visitChildren(this);
		}
	}

	public final ForLoopContext forLoop() throws RecognitionException {
		ForLoopContext _localctx = new ForLoopContext(_ctx, getState());
		enterRule(_localctx, 24, RULE_forLoop);
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(172);
			match(FOR);
			setState(173);
			match(LPAREN);
			setState(174);
			match(Identifier);
			setState(175);
			match(COLON);
			setState(176);
			beanAccess();
			setState(177);
			match(RPAREN);
			setState(178);
			templateBody();
			setState(179);
			match(END);
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	public static class ExpressionContext extends ParserRuleContext {
		public ExpressionContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_expression; }
	 
		public ExpressionContext() { }
		public void copyFrom(ExpressionContext ctx) {
			super.copyFrom(ctx);
		}
	}
	public static class MulDivModContext extends ExpressionContext {
		public List<ExpressionContext> expression() {
			return getRuleContexts(ExpressionContext.class);
		}
		public ExpressionContext expression(int i) {
			return getRuleContext(ExpressionContext.class,i);
		}
		public MulDivModContext(ExpressionContext ctx) { copyFrom(ctx); }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof TemplateParserListener ) ((TemplateParserListener)listener).enterMulDivMod(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof TemplateParserListener ) ((TemplateParserListener)listener).exitMulDivMod(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof TemplateParserVisitor ) return ((TemplateParserVisitor<? extends T>)visitor).visitMulDivMod(this);
			else return visitor.visitChildren(this);
		}
	}
	public static class SuffixUnaryContext extends ExpressionContext {
		public ExpressionContext expression() {
			return getRuleContext(ExpressionContext.class,0);
		}
		public SuffixUnaryContext(ExpressionContext ctx) { copyFrom(ctx); }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof TemplateParserListener ) ((TemplateParserListener)listener).enterSuffixUnary(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof TemplateParserListener ) ((TemplateParserListener)listener).exitSuffixUnary(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof TemplateParserVisitor ) return ((TemplateParserVisitor<? extends T>)visitor).visitSuffixUnary(this);
			else return visitor.visitChildren(this);
		}
	}
	public static class ParensContext extends ExpressionContext {
		public ExpressionContext expression() {
			return getRuleContext(ExpressionContext.class,0);
		}
		public ParensContext(ExpressionContext ctx) { copyFrom(ctx); }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof TemplateParserListener ) ((TemplateParserListener)listener).enterParens(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof TemplateParserListener ) ((TemplateParserListener)listener).exitParens(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof TemplateParserVisitor ) return ((TemplateParserVisitor<? extends T>)visitor).visitParens(this);
			else return visitor.visitChildren(this);
		}
	}
	public static class BitOrContext extends ExpressionContext {
		public List<ExpressionContext> expression() {
			return getRuleContexts(ExpressionContext.class);
		}
		public ExpressionContext expression(int i) {
			return getRuleContext(ExpressionContext.class,i);
		}
		public BitOrContext(ExpressionContext ctx) { copyFrom(ctx); }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof TemplateParserListener ) ((TemplateParserListener)listener).enterBitOr(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof TemplateParserListener ) ((TemplateParserListener)listener).exitBitOr(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof TemplateParserVisitor ) return ((TemplateParserVisitor<? extends T>)visitor).visitBitOr(this);
			else return visitor.visitChildren(this);
		}
	}
	public static class AssignmentContext extends ExpressionContext {
		public List<ExpressionContext> expression() {
			return getRuleContexts(ExpressionContext.class);
		}
		public ExpressionContext expression(int i) {
			return getRuleContext(ExpressionContext.class,i);
		}
		public AssignmentContext(ExpressionContext ctx) { copyFrom(ctx); }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof TemplateParserListener ) ((TemplateParserListener)listener).enterAssignment(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof TemplateParserListener ) ((TemplateParserListener)listener).exitAssignment(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof TemplateParserVisitor ) return ((TemplateParserVisitor<? extends T>)visitor).visitAssignment(this);
			else return visitor.visitChildren(this);
		}
	}
	public static class ShiftContext extends ExpressionContext {
		public List<ExpressionContext> expression() {
			return getRuleContexts(ExpressionContext.class);
		}
		public ExpressionContext expression(int i) {
			return getRuleContext(ExpressionContext.class,i);
		}
		public ShiftContext(ExpressionContext ctx) { copyFrom(ctx); }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof TemplateParserListener ) ((TemplateParserListener)listener).enterShift(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof TemplateParserListener ) ((TemplateParserListener)listener).exitShift(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof TemplateParserVisitor ) return ((TemplateParserVisitor<? extends T>)visitor).visitShift(this);
			else return visitor.visitChildren(this);
		}
	}
	public static class AddSubContext extends ExpressionContext {
		public List<ExpressionContext> expression() {
			return getRuleContexts(ExpressionContext.class);
		}
		public ExpressionContext expression(int i) {
			return getRuleContext(ExpressionContext.class,i);
		}
		public AddSubContext(ExpressionContext ctx) { copyFrom(ctx); }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof TemplateParserListener ) ((TemplateParserListener)listener).enterAddSub(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof TemplateParserListener ) ((TemplateParserListener)listener).exitAddSub(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof TemplateParserVisitor ) return ((TemplateParserVisitor<? extends T>)visitor).visitAddSub(this);
			else return visitor.visitChildren(this);
		}
	}
	public static class FloatingPointLiteralContext extends ExpressionContext {
		public TerminalNode FloatingPointLiteral() { return getToken(TemplateParserParser.FloatingPointLiteral, 0); }
		public FloatingPointLiteralContext(ExpressionContext ctx) { copyFrom(ctx); }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof TemplateParserListener ) ((TemplateParserListener)listener).enterFloatingPointLiteral(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof TemplateParserListener ) ((TemplateParserListener)listener).exitFloatingPointLiteral(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof TemplateParserVisitor ) return ((TemplateParserVisitor<? extends T>)visitor).visitFloatingPointLiteral(this);
			else return visitor.visitChildren(this);
		}
	}
	public static class ShortCircuitOrContext extends ExpressionContext {
		public List<ExpressionContext> expression() {
			return getRuleContexts(ExpressionContext.class);
		}
		public ExpressionContext expression(int i) {
			return getRuleContext(ExpressionContext.class,i);
		}
		public ShortCircuitOrContext(ExpressionContext ctx) { copyFrom(ctx); }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof TemplateParserListener ) ((TemplateParserListener)listener).enterShortCircuitOr(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof TemplateParserListener ) ((TemplateParserListener)listener).exitShortCircuitOr(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof TemplateParserVisitor ) return ((TemplateParserVisitor<? extends T>)visitor).visitShortCircuitOr(this);
			else return visitor.visitChildren(this);
		}
	}
	public static class PrefixUnaryContext extends ExpressionContext {
		public ExpressionContext expression() {
			return getRuleContext(ExpressionContext.class,0);
		}
		public PrefixUnaryContext(ExpressionContext ctx) { copyFrom(ctx); }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof TemplateParserListener ) ((TemplateParserListener)listener).enterPrefixUnary(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof TemplateParserListener ) ((TemplateParserListener)listener).exitPrefixUnary(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof TemplateParserVisitor ) return ((TemplateParserVisitor<? extends T>)visitor).visitPrefixUnary(this);
			else return visitor.visitChildren(this);
		}
	}
	public static class EqualOrNotEqualContext extends ExpressionContext {
		public List<ExpressionContext> expression() {
			return getRuleContexts(ExpressionContext.class);
		}
		public ExpressionContext expression(int i) {
			return getRuleContext(ExpressionContext.class,i);
		}
		public EqualOrNotEqualContext(ExpressionContext ctx) { copyFrom(ctx); }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof TemplateParserListener ) ((TemplateParserListener)listener).enterEqualOrNotEqual(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof TemplateParserListener ) ((TemplateParserListener)listener).exitEqualOrNotEqual(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof TemplateParserVisitor ) return ((TemplateParserVisitor<? extends T>)visitor).visitEqualOrNotEqual(this);
			else return visitor.visitChildren(this);
		}
	}
	public static class BitAndContext extends ExpressionContext {
		public List<ExpressionContext> expression() {
			return getRuleContexts(ExpressionContext.class);
		}
		public ExpressionContext expression(int i) {
			return getRuleContext(ExpressionContext.class,i);
		}
		public BitAndContext(ExpressionContext ctx) { copyFrom(ctx); }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof TemplateParserListener ) ((TemplateParserListener)listener).enterBitAnd(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof TemplateParserListener ) ((TemplateParserListener)listener).exitBitAnd(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof TemplateParserVisitor ) return ((TemplateParserVisitor<? extends T>)visitor).visitBitAnd(this);
			else return visitor.visitChildren(this);
		}
	}
	public static class StringLiteralContext extends ExpressionContext {
		public TerminalNode StringLiteral() { return getToken(TemplateParserParser.StringLiteral, 0); }
		public StringLiteralContext(ExpressionContext ctx) { copyFrom(ctx); }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof TemplateParserListener ) ((TemplateParserListener)listener).enterStringLiteral(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof TemplateParserListener ) ((TemplateParserListener)listener).exitStringLiteral(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof TemplateParserVisitor ) return ((TemplateParserVisitor<? extends T>)visitor).visitStringLiteral(this);
			else return visitor.visitChildren(this);
		}
	}
	public static class IntegerLiteralContext extends ExpressionContext {
		public TerminalNode IntegerLiteral() { return getToken(TemplateParserParser.IntegerLiteral, 0); }
		public IntegerLiteralContext(ExpressionContext ctx) { copyFrom(ctx); }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof TemplateParserListener ) ((TemplateParserListener)listener).enterIntegerLiteral(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof TemplateParserListener ) ((TemplateParserListener)listener).exitIntegerLiteral(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof TemplateParserVisitor ) return ((TemplateParserVisitor<? extends T>)visitor).visitIntegerLiteral(this);
			else return visitor.visitChildren(this);
		}
	}
	public static class XorContext extends ExpressionContext {
		public List<ExpressionContext> expression() {
			return getRuleContexts(ExpressionContext.class);
		}
		public ExpressionContext expression(int i) {
			return getRuleContext(ExpressionContext.class,i);
		}
		public XorContext(ExpressionContext ctx) { copyFrom(ctx); }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof TemplateParserListener ) ((TemplateParserListener)listener).enterXor(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof TemplateParserListener ) ((TemplateParserListener)listener).exitXor(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof TemplateParserVisitor ) return ((TemplateParserVisitor<? extends T>)visitor).visitXor(this);
			else return visitor.visitChildren(this);
		}
	}
	public static class BooleanLiteralContext extends ExpressionContext {
		public TerminalNode BooleanLiteral() { return getToken(TemplateParserParser.BooleanLiteral, 0); }
		public BooleanLiteralContext(ExpressionContext ctx) { copyFrom(ctx); }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof TemplateParserListener ) ((TemplateParserListener)listener).enterBooleanLiteral(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof TemplateParserListener ) ((TemplateParserListener)listener).exitBooleanLiteral(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof TemplateParserVisitor ) return ((TemplateParserVisitor<? extends T>)visitor).visitBooleanLiteral(this);
			else return visitor.visitChildren(this);
		}
	}
	public static class TernaryContext extends ExpressionContext {
		public List<ExpressionContext> expression() {
			return getRuleContexts(ExpressionContext.class);
		}
		public ExpressionContext expression(int i) {
			return getRuleContext(ExpressionContext.class,i);
		}
		public TernaryContext(ExpressionContext ctx) { copyFrom(ctx); }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof TemplateParserListener ) ((TemplateParserListener)listener).enterTernary(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof TemplateParserListener ) ((TemplateParserListener)listener).exitTernary(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof TemplateParserVisitor ) return ((TemplateParserVisitor<? extends T>)visitor).visitTernary(this);
			else return visitor.visitChildren(this);
		}
	}
	public static class BeanContext extends ExpressionContext {
		public BeanAccessContext beanAccess() {
			return getRuleContext(BeanAccessContext.class,0);
		}
		public BeanContext(ExpressionContext ctx) { copyFrom(ctx); }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof TemplateParserListener ) ((TemplateParserListener)listener).enterBean(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof TemplateParserListener ) ((TemplateParserListener)listener).exitBean(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof TemplateParserVisitor ) return ((TemplateParserVisitor<? extends T>)visitor).visitBean(this);
			else return visitor.visitChildren(this);
		}
	}
	public static class ShortCircuitAndContext extends ExpressionContext {
		public List<ExpressionContext> expression() {
			return getRuleContexts(ExpressionContext.class);
		}
		public ExpressionContext expression(int i) {
			return getRuleContext(ExpressionContext.class,i);
		}
		public ShortCircuitAndContext(ExpressionContext ctx) { copyFrom(ctx); }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof TemplateParserListener ) ((TemplateParserListener)listener).enterShortCircuitAnd(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof TemplateParserListener ) ((TemplateParserListener)listener).exitShortCircuitAnd(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof TemplateParserVisitor ) return ((TemplateParserVisitor<? extends T>)visitor).visitShortCircuitAnd(this);
			else return visitor.visitChildren(this);
		}
	}
	public static class GreatOrLessContext extends ExpressionContext {
		public List<ExpressionContext> expression() {
			return getRuleContexts(ExpressionContext.class);
		}
		public ExpressionContext expression(int i) {
			return getRuleContext(ExpressionContext.class,i);
		}
		public GreatOrLessContext(ExpressionContext ctx) { copyFrom(ctx); }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof TemplateParserListener ) ((TemplateParserListener)listener).enterGreatOrLess(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof TemplateParserListener ) ((TemplateParserListener)listener).exitGreatOrLess(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof TemplateParserVisitor ) return ((TemplateParserVisitor<? extends T>)visitor).visitGreatOrLess(this);
			else return visitor.visitChildren(this);
		}
	}

	public final ExpressionContext expression() throws RecognitionException {
		return expression(0);
	}

	private ExpressionContext expression(int _p) throws RecognitionException {
		ParserRuleContext _parentctx = _ctx;
		int _parentState = getState();
		ExpressionContext _localctx = new ExpressionContext(_ctx, _parentState);
		ExpressionContext _prevctx = _localctx;
		int _startState = 26;
		enterRecursionRule(_localctx, 26, RULE_expression, _p);
		int _la;
		try {
			int _alt;
			enterOuterAlt(_localctx, 1);
			{
			setState(193);
			switch (_input.LA(1)) {
			case T__2:
				{
				_localctx = new BeanContext(_localctx);
				_ctx = _localctx;
				_prevctx = _localctx;

				setState(182);
				beanAccess();
				}
				break;
			case IntegerLiteral:
				{
				_localctx = new IntegerLiteralContext(_localctx);
				_ctx = _localctx;
				_prevctx = _localctx;
				setState(183);
				match(IntegerLiteral);
				}
				break;
			case FloatingPointLiteral:
				{
				_localctx = new FloatingPointLiteralContext(_localctx);
				_ctx = _localctx;
				_prevctx = _localctx;
				setState(184);
				match(FloatingPointLiteral);
				}
				break;
			case BooleanLiteral:
				{
				_localctx = new BooleanLiteralContext(_localctx);
				_ctx = _localctx;
				_prevctx = _localctx;
				setState(185);
				match(BooleanLiteral);
				}
				break;
			case StringLiteral:
				{
				_localctx = new StringLiteralContext(_localctx);
				_ctx = _localctx;
				_prevctx = _localctx;
				setState(186);
				match(StringLiteral);
				}
				break;
			case LPAREN:
				{
				_localctx = new ParensContext(_localctx);
				_ctx = _localctx;
				_prevctx = _localctx;
				setState(187);
				match(LPAREN);
				setState(188);
				expression(0);
				setState(189);
				match(RPAREN);
				}
				break;
			case BANG:
			case TILDE:
			case INC:
			case DEC:
			case ADD:
			case SUB:
				{
				_localctx = new PrefixUnaryContext(_localctx);
				_ctx = _localctx;
				_prevctx = _localctx;
				setState(191);
				_la = _input.LA(1);
				if ( !((((_la) & ~0x3f) == 0 && ((1L << _la) & ((1L << BANG) | (1L << TILDE) | (1L << INC) | (1L << DEC) | (1L << ADD) | (1L << SUB))) != 0)) ) {
				_errHandler.recoverInline(this);
				} else {
					consume();
				}
				setState(192);
				expression(14);
				}
				break;
			default:
				throw new NoViableAltException(this);
			}
			_ctx.stop = _input.LT(-1);
			setState(238);
			_errHandler.sync(this);
			_alt = getInterpreter().adaptivePredict(_input,15,_ctx);
			while ( _alt!=2 && _alt!=org.antlr.v4.runtime.atn.ATN.INVALID_ALT_NUMBER ) {
				if ( _alt==1 ) {
					if ( _parseListeners!=null ) triggerExitRuleEvent();
					_prevctx = _localctx;
					{
					setState(236);
					_errHandler.sync(this);
					switch ( getInterpreter().adaptivePredict(_input,14,_ctx) ) {
					case 1:
						{
						_localctx = new MulDivModContext(new ExpressionContext(_parentctx, _parentState));
						pushNewRecursionContext(_localctx, _startState, RULE_expression);
						setState(195);
						if (!(precpred(_ctx, 12))) throw new FailedPredicateException(this, "precpred(_ctx, 12)");
						setState(196);
						_la = _input.LA(1);
						if ( !((((_la) & ~0x3f) == 0 && ((1L << _la) & ((1L << MUL) | (1L << DIV) | (1L << MOD))) != 0)) ) {
						_errHandler.recoverInline(this);
						} else {
							consume();
						}
						setState(197);
						expression(13);
						}
						break;
					case 2:
						{
						_localctx = new AddSubContext(new ExpressionContext(_parentctx, _parentState));
						pushNewRecursionContext(_localctx, _startState, RULE_expression);
						setState(198);
						if (!(precpred(_ctx, 11))) throw new FailedPredicateException(this, "precpred(_ctx, 11)");
						setState(199);
						_la = _input.LA(1);
						if ( !(_la==ADD || _la==SUB) ) {
						_errHandler.recoverInline(this);
						} else {
							consume();
						}
						setState(200);
						expression(12);
						}
						break;
					case 3:
						{
						_localctx = new ShiftContext(new ExpressionContext(_parentctx, _parentState));
						pushNewRecursionContext(_localctx, _startState, RULE_expression);
						setState(201);
						if (!(precpred(_ctx, 10))) throw new FailedPredicateException(this, "precpred(_ctx, 10)");
						setState(202);
						_la = _input.LA(1);
						if ( !((((_la) & ~0x3f) == 0 && ((1L << _la) & ((1L << LSHIFT) | (1L << RSHIFT) | (1L << URSHIFT))) != 0)) ) {
						_errHandler.recoverInline(this);
						} else {
							consume();
						}
						setState(203);
						expression(11);
						}
						break;
					case 4:
						{
						_localctx = new GreatOrLessContext(new ExpressionContext(_parentctx, _parentState));
						pushNewRecursionContext(_localctx, _startState, RULE_expression);
						setState(204);
						if (!(precpred(_ctx, 9))) throw new FailedPredicateException(this, "precpred(_ctx, 9)");
						setState(205);
						_la = _input.LA(1);
						if ( !((((_la) & ~0x3f) == 0 && ((1L << _la) & ((1L << GT) | (1L << LT) | (1L << LE) | (1L << GE))) != 0)) ) {
						_errHandler.recoverInline(this);
						} else {
							consume();
						}
						setState(206);
						expression(10);
						}
						break;
					case 5:
						{
						_localctx = new EqualOrNotEqualContext(new ExpressionContext(_parentctx, _parentState));
						pushNewRecursionContext(_localctx, _startState, RULE_expression);
						setState(207);
						if (!(precpred(_ctx, 8))) throw new FailedPredicateException(this, "precpred(_ctx, 8)");
						setState(208);
						_la = _input.LA(1);
						if ( !(_la==EQUAL || _la==NOTEQUAL) ) {
						_errHandler.recoverInline(this);
						} else {
							consume();
						}
						setState(209);
						expression(9);
						}
						break;
					case 6:
						{
						_localctx = new BitAndContext(new ExpressionContext(_parentctx, _parentState));
						pushNewRecursionContext(_localctx, _startState, RULE_expression);
						setState(210);
						if (!(precpred(_ctx, 7))) throw new FailedPredicateException(this, "precpred(_ctx, 7)");
						setState(211);
						match(BITAND);
						setState(212);
						expression(8);
						}
						break;
					case 7:
						{
						_localctx = new BitOrContext(new ExpressionContext(_parentctx, _parentState));
						pushNewRecursionContext(_localctx, _startState, RULE_expression);
						setState(213);
						if (!(precpred(_ctx, 6))) throw new FailedPredicateException(this, "precpred(_ctx, 6)");
						setState(214);
						match(BITOR);
						setState(215);
						expression(7);
						}
						break;
					case 8:
						{
						_localctx = new XorContext(new ExpressionContext(_parentctx, _parentState));
						pushNewRecursionContext(_localctx, _startState, RULE_expression);
						setState(216);
						if (!(precpred(_ctx, 5))) throw new FailedPredicateException(this, "precpred(_ctx, 5)");
						setState(217);
						match(CARET);
						setState(218);
						expression(6);
						}
						break;
					case 9:
						{
						_localctx = new ShortCircuitAndContext(new ExpressionContext(_parentctx, _parentState));
						pushNewRecursionContext(_localctx, _startState, RULE_expression);
						setState(219);
						if (!(precpred(_ctx, 4))) throw new FailedPredicateException(this, "precpred(_ctx, 4)");
						setState(220);
						match(AND);
						setState(221);
						expression(5);
						}
						break;
					case 10:
						{
						_localctx = new ShortCircuitOrContext(new ExpressionContext(_parentctx, _parentState));
						pushNewRecursionContext(_localctx, _startState, RULE_expression);
						setState(222);
						if (!(precpred(_ctx, 3))) throw new FailedPredicateException(this, "precpred(_ctx, 3)");
						setState(223);
						match(OR);
						setState(224);
						expression(4);
						}
						break;
					case 11:
						{
						_localctx = new TernaryContext(new ExpressionContext(_parentctx, _parentState));
						pushNewRecursionContext(_localctx, _startState, RULE_expression);
						setState(225);
						if (!(precpred(_ctx, 2))) throw new FailedPredicateException(this, "precpred(_ctx, 2)");
						setState(226);
						match(QUESTION);
						setState(227);
						expression(0);
						setState(228);
						match(COLON);
						setState(229);
						expression(3);
						}
						break;
					case 12:
						{
						_localctx = new AssignmentContext(new ExpressionContext(_parentctx, _parentState));
						pushNewRecursionContext(_localctx, _startState, RULE_expression);
						setState(231);
						if (!(precpred(_ctx, 1))) throw new FailedPredicateException(this, "precpred(_ctx, 1)");
						setState(232);
						_la = _input.LA(1);
						if ( !(((((_la - 36)) & ~0x3f) == 0 && ((1L << (_la - 36)) & ((1L << (ASSIGN - 36)) | (1L << (ADD_ASSIGN - 36)) | (1L << (SUB_ASSIGN - 36)) | (1L << (MUL_ASSIGN - 36)) | (1L << (DIV_ASSIGN - 36)) | (1L << (AND_ASSIGN - 36)) | (1L << (OR_ASSIGN - 36)) | (1L << (XOR_ASSIGN - 36)) | (1L << (MOD_ASSIGN - 36)) | (1L << (LSHIFT_ASSIGN - 36)) | (1L << (RSHIFT_ASSIGN - 36)) | (1L << (URSHIFT_ASSIGN - 36)))) != 0)) ) {
						_errHandler.recoverInline(this);
						} else {
							consume();
						}
						setState(233);
						expression(2);
						}
						break;
					case 13:
						{
						_localctx = new SuffixUnaryContext(new ExpressionContext(_parentctx, _parentState));
						pushNewRecursionContext(_localctx, _startState, RULE_expression);
						setState(234);
						if (!(precpred(_ctx, 13))) throw new FailedPredicateException(this, "precpred(_ctx, 13)");
						setState(235);
						_la = _input.LA(1);
						if ( !(_la==INC || _la==DEC) ) {
						_errHandler.recoverInline(this);
						} else {
							consume();
						}
						}
						break;
					}
					} 
				}
				setState(240);
				_errHandler.sync(this);
				_alt = getInterpreter().adaptivePredict(_input,15,_ctx);
			}
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			unrollRecursionContexts(_parentctx);
		}
		return _localctx;
	}

	public static class BeanAccessContext extends ParserRuleContext {
		public ObjectAccessContext objectAccess() {
			return getRuleContext(ObjectAccessContext.class,0);
		}
		public BeanAccessContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_beanAccess; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof TemplateParserListener ) ((TemplateParserListener)listener).enterBeanAccess(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof TemplateParserListener ) ((TemplateParserListener)listener).exitBeanAccess(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof TemplateParserVisitor ) return ((TemplateParserVisitor<? extends T>)visitor).visitBeanAccess(this);
			else return visitor.visitChildren(this);
		}
	}

	public final BeanAccessContext beanAccess() throws RecognitionException {
		BeanAccessContext _localctx = new BeanAccessContext(_ctx, getState());
		enterRule(_localctx, 28, RULE_beanAccess);
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(241);
			match(T__2);
			setState(242);
			objectAccess();
			setState(243);
			match(T__3);
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	public static class ObjectAccessContext extends ParserRuleContext {
		public TerminalNode Identifier() { return getToken(TemplateParserParser.Identifier, 0); }
		public List<PropertyAccessContext> propertyAccess() {
			return getRuleContexts(PropertyAccessContext.class);
		}
		public PropertyAccessContext propertyAccess(int i) {
			return getRuleContext(PropertyAccessContext.class,i);
		}
		public List<ArrayAccessContext> arrayAccess() {
			return getRuleContexts(ArrayAccessContext.class);
		}
		public ArrayAccessContext arrayAccess(int i) {
			return getRuleContext(ArrayAccessContext.class,i);
		}
		public List<MapAccessContext> mapAccess() {
			return getRuleContexts(MapAccessContext.class);
		}
		public MapAccessContext mapAccess(int i) {
			return getRuleContext(MapAccessContext.class,i);
		}
		public List<MethodCallContext> methodCall() {
			return getRuleContexts(MethodCallContext.class);
		}
		public MethodCallContext methodCall(int i) {
			return getRuleContext(MethodCallContext.class,i);
		}
		public ObjectAccessContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_objectAccess; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof TemplateParserListener ) ((TemplateParserListener)listener).enterObjectAccess(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof TemplateParserListener ) ((TemplateParserListener)listener).exitObjectAccess(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof TemplateParserVisitor ) return ((TemplateParserVisitor<? extends T>)visitor).visitObjectAccess(this);
			else return visitor.visitChildren(this);
		}
	}

	public final ObjectAccessContext objectAccess() throws RecognitionException {
		ObjectAccessContext _localctx = new ObjectAccessContext(_ctx, getState());
		enterRule(_localctx, 30, RULE_objectAccess);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(245);
			match(Identifier);
			setState(252);
			_errHandler.sync(this);
			_la = _input.LA(1);
			while (_la==LBRACK || _la==DOT) {
				{
				setState(250);
				_errHandler.sync(this);
				switch ( getInterpreter().adaptivePredict(_input,16,_ctx) ) {
				case 1:
					{
					setState(246);
					propertyAccess();
					}
					break;
				case 2:
					{
					setState(247);
					arrayAccess();
					}
					break;
				case 3:
					{
					setState(248);
					mapAccess();
					}
					break;
				case 4:
					{
					setState(249);
					methodCall();
					}
					break;
				}
				}
				setState(254);
				_errHandler.sync(this);
				_la = _input.LA(1);
			}
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	public static class PropertyAccessContext extends ParserRuleContext {
		public TerminalNode Identifier() { return getToken(TemplateParserParser.Identifier, 0); }
		public PropertyAccessContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_propertyAccess; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof TemplateParserListener ) ((TemplateParserListener)listener).enterPropertyAccess(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof TemplateParserListener ) ((TemplateParserListener)listener).exitPropertyAccess(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof TemplateParserVisitor ) return ((TemplateParserVisitor<? extends T>)visitor).visitPropertyAccess(this);
			else return visitor.visitChildren(this);
		}
	}

	public final PropertyAccessContext propertyAccess() throws RecognitionException {
		PropertyAccessContext _localctx = new PropertyAccessContext(_ctx, getState());
		enterRule(_localctx, 32, RULE_propertyAccess);
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(255);
			match(DOT);
			setState(256);
			match(Identifier);
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	public static class ArrayAccessContext extends ParserRuleContext {
		public TerminalNode IntegerLiteral() { return getToken(TemplateParserParser.IntegerLiteral, 0); }
		public ArrayAccessContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_arrayAccess; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof TemplateParserListener ) ((TemplateParserListener)listener).enterArrayAccess(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof TemplateParserListener ) ((TemplateParserListener)listener).exitArrayAccess(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof TemplateParserVisitor ) return ((TemplateParserVisitor<? extends T>)visitor).visitArrayAccess(this);
			else return visitor.visitChildren(this);
		}
	}

	public final ArrayAccessContext arrayAccess() throws RecognitionException {
		ArrayAccessContext _localctx = new ArrayAccessContext(_ctx, getState());
		enterRule(_localctx, 34, RULE_arrayAccess);
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(258);
			match(LBRACK);
			setState(259);
			match(IntegerLiteral);
			setState(260);
			match(RBRACK);
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	public static class MapAccessContext extends ParserRuleContext {
		public TerminalNode StringLiteral() { return getToken(TemplateParserParser.StringLiteral, 0); }
		public MapAccessContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_mapAccess; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof TemplateParserListener ) ((TemplateParserListener)listener).enterMapAccess(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof TemplateParserListener ) ((TemplateParserListener)listener).exitMapAccess(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof TemplateParserVisitor ) return ((TemplateParserVisitor<? extends T>)visitor).visitMapAccess(this);
			else return visitor.visitChildren(this);
		}
	}

	public final MapAccessContext mapAccess() throws RecognitionException {
		MapAccessContext _localctx = new MapAccessContext(_ctx, getState());
		enterRule(_localctx, 36, RULE_mapAccess);
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(262);
			match(LBRACK);
			setState(263);
			match(StringLiteral);
			setState(264);
			match(RBRACK);
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	public static class MethodCallContext extends ParserRuleContext {
		public TerminalNode Identifier() { return getToken(TemplateParserParser.Identifier, 0); }
		public CallMethodParametersContext callMethodParameters() {
			return getRuleContext(CallMethodParametersContext.class,0);
		}
		public MethodCallContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_methodCall; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof TemplateParserListener ) ((TemplateParserListener)listener).enterMethodCall(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof TemplateParserListener ) ((TemplateParserListener)listener).exitMethodCall(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof TemplateParserVisitor ) return ((TemplateParserVisitor<? extends T>)visitor).visitMethodCall(this);
			else return visitor.visitChildren(this);
		}
	}

	public final MethodCallContext methodCall() throws RecognitionException {
		MethodCallContext _localctx = new MethodCallContext(_ctx, getState());
		enterRule(_localctx, 38, RULE_methodCall);
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(266);
			match(DOT);
			setState(267);
			match(Identifier);
			setState(268);
			callMethodParameters();
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	public static class CallMethodParametersContext extends ParserRuleContext {
		public List<ExpressionContext> expression() {
			return getRuleContexts(ExpressionContext.class);
		}
		public ExpressionContext expression(int i) {
			return getRuleContext(ExpressionContext.class,i);
		}
		public CallMethodParametersContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_callMethodParameters; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof TemplateParserListener ) ((TemplateParserListener)listener).enterCallMethodParameters(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof TemplateParserListener ) ((TemplateParserListener)listener).exitCallMethodParameters(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof TemplateParserVisitor ) return ((TemplateParserVisitor<? extends T>)visitor).visitCallMethodParameters(this);
			else return visitor.visitChildren(this);
		}
	}

	public final CallMethodParametersContext callMethodParameters() throws RecognitionException {
		CallMethodParametersContext _localctx = new CallMethodParametersContext(_ctx, getState());
		enterRule(_localctx, 40, RULE_callMethodParameters);
		int _la;
		try {
			setState(283);
			_errHandler.sync(this);
			switch ( getInterpreter().adaptivePredict(_input,19,_ctx) ) {
			case 1:
				enterOuterAlt(_localctx, 1);
				{
				setState(270);
				match(LPAREN);
				setState(271);
				match(RPAREN);
				}
				break;
			case 2:
				enterOuterAlt(_localctx, 2);
				{
				setState(272);
				match(LPAREN);
				setState(273);
				expression(0);
				setState(278);
				_errHandler.sync(this);
				_la = _input.LA(1);
				while (_la==T__1) {
					{
					{
					setState(274);
					match(T__1);
					setState(275);
					expression(0);
					}
					}
					setState(280);
					_errHandler.sync(this);
					_la = _input.LA(1);
				}
				setState(281);
				match(RPAREN);
				}
				break;
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	public boolean sempred(RuleContext _localctx, int ruleIndex, int predIndex) {
		switch (ruleIndex) {
		case 13:
			return expression_sempred((ExpressionContext)_localctx, predIndex);
		}
		return true;
	}
	private boolean expression_sempred(ExpressionContext _localctx, int predIndex) {
		switch (predIndex) {
		case 0:
			return precpred(_ctx, 12);
		case 1:
			return precpred(_ctx, 11);
		case 2:
			return precpred(_ctx, 10);
		case 3:
			return precpred(_ctx, 9);
		case 4:
			return precpred(_ctx, 8);
		case 5:
			return precpred(_ctx, 7);
		case 6:
			return precpred(_ctx, 6);
		case 7:
			return precpred(_ctx, 5);
		case 8:
			return precpred(_ctx, 4);
		case 9:
			return precpred(_ctx, 3);
		case 10:
			return precpred(_ctx, 2);
		case 11:
			return precpred(_ctx, 1);
		case 12:
			return precpred(_ctx, 13);
		}
		return true;
	}

	public static final String _serializedATN =
		"\3\u0430\ud6d1\u8206\uad2d\u4417\uaef1\u8d80\uaadd\3N\u0120\4\2\t\2\4"+
		"\3\t\3\4\4\t\4\4\5\t\5\4\6\t\6\4\7\t\7\4\b\t\b\4\t\t\t\4\n\t\n\4\13\t"+
		"\13\4\f\t\f\4\r\t\r\4\16\t\16\4\17\t\17\4\20\t\20\4\21\t\21\4\22\t\22"+
		"\4\23\t\23\4\24\t\24\4\25\t\25\4\26\t\26\3\2\5\2.\n\2\3\2\3\2\7\2\62\n"+
		"\2\f\2\16\2\65\13\2\3\3\3\3\3\3\3\3\3\3\3\3\3\3\3\3\3\3\3\3\7\3A\n\3\f"+
		"\3\16\3D\13\3\3\4\3\4\3\4\3\4\3\5\3\5\3\5\3\5\5\5N\n\5\3\5\5\5Q\n\5\3"+
		"\5\3\5\3\6\3\6\3\6\3\6\3\6\3\6\3\6\3\7\3\7\3\7\7\7_\n\7\f\7\16\7b\13\7"+
		"\3\b\3\b\3\b\3\b\3\t\3\t\3\t\3\t\3\t\3\t\3\n\3\n\3\n\3\n\3\n\3\n\7\nt"+
		"\n\n\f\n\16\nw\13\n\3\n\5\nz\n\n\3\13\3\13\3\13\3\13\3\13\3\13\3\13\3"+
		"\13\3\13\3\13\3\13\3\13\7\13\u0088\n\13\f\13\16\13\u008b\13\13\3\13\3"+
		"\13\5\13\u008f\n\13\3\13\3\13\3\f\3\f\3\f\3\f\3\f\3\f\3\f\3\f\3\f\6\f"+
		"\u009c\n\f\r\f\16\f\u009d\3\f\3\f\3\f\3\f\5\f\u00a4\n\f\3\f\3\f\3\r\3"+
		"\r\3\r\3\r\3\r\3\r\3\r\3\16\3\16\3\16\3\16\3\16\3\16\3\16\3\16\3\16\3"+
		"\17\3\17\3\17\3\17\3\17\3\17\3\17\3\17\3\17\3\17\3\17\3\17\5\17\u00c4"+
		"\n\17\3\17\3\17\3\17\3\17\3\17\3\17\3\17\3\17\3\17\3\17\3\17\3\17\3\17"+
		"\3\17\3\17\3\17\3\17\3\17\3\17\3\17\3\17\3\17\3\17\3\17\3\17\3\17\3\17"+
		"\3\17\3\17\3\17\3\17\3\17\3\17\3\17\3\17\3\17\3\17\3\17\3\17\3\17\3\17"+
		"\7\17\u00ef\n\17\f\17\16\17\u00f2\13\17\3\20\3\20\3\20\3\20\3\21\3\21"+
		"\3\21\3\21\3\21\7\21\u00fd\n\21\f\21\16\21\u0100\13\21\3\22\3\22\3\22"+
		"\3\23\3\23\3\23\3\23\3\24\3\24\3\24\3\24\3\25\3\25\3\25\3\25\3\26\3\26"+
		"\3\26\3\26\3\26\3\26\7\26\u0117\n\26\f\26\16\26\u011a\13\26\3\26\3\26"+
		"\5\26\u011e\n\26\3\26\2\3\34\27\2\4\6\b\n\f\16\20\22\24\26\30\32\34\36"+
		" \"$&(*\2\13\4\2\34\34\37\37\4\2)*\63\66\4\2\678<<\3\2\65\66\3\2?A\4\2"+
		"\'(./\4\2--\60\60\4\2&&BL\3\2\63\64\u0138\2-\3\2\2\2\4B\3\2\2\2\6E\3\2"+
		"\2\2\bI\3\2\2\2\nT\3\2\2\2\f[\3\2\2\2\16c\3\2\2\2\20g\3\2\2\2\22y\3\2"+
		"\2\2\24{\3\2\2\2\26\u0092\3\2\2\2\30\u00a7\3\2\2\2\32\u00ae\3\2\2\2\34"+
		"\u00c3\3\2\2\2\36\u00f3\3\2\2\2 \u00f7\3\2\2\2\"\u0101\3\2\2\2$\u0104"+
		"\3\2\2\2&\u0108\3\2\2\2(\u010c\3\2\2\2*\u011d\3\2\2\2,.\5\6\4\2-,\3\2"+
		"\2\2-.\3\2\2\2./\3\2\2\2/\63\5\16\b\2\60\62\5\20\t\2\61\60\3\2\2\2\62"+
		"\65\3\2\2\2\63\61\3\2\2\2\63\64\3\2\2\2\64\3\3\2\2\2\65\63\3\2\2\2\66"+
		"A\7\27\2\2\67A\7\30\2\28A\7\31\2\29A\5\n\6\2:A\5\b\5\2;A\5\24\13\2<A\5"+
		"\26\f\2=A\5\30\r\2>A\5\32\16\2?A\5\36\20\2@\66\3\2\2\2@\67\3\2\2\2@8\3"+
		"\2\2\2@9\3\2\2\2@:\3\2\2\2@;\3\2\2\2@<\3\2\2\2@=\3\2\2\2@>\3\2\2\2@?\3"+
		"\2\2\2AD\3\2\2\2B@\3\2\2\2BC\3\2\2\2C\5\3\2\2\2DB\3\2\2\2EF\7\7\2\2FG"+
		"\5\f\7\2GH\7\3\2\2H\7\3\2\2\2IJ\7\b\2\2JM\7!\2\2KN\7\26\2\2LN\5\f\7\2"+
		"MK\3\2\2\2ML\3\2\2\2NP\3\2\2\2OQ\5(\25\2PO\3\2\2\2PQ\3\2\2\2QR\3\2\2\2"+
		"RS\7\"\2\2S\t\3\2\2\2TU\7\t\2\2UV\7!\2\2VW\7M\2\2WX\7&\2\2XY\5\34\17\2"+
		"YZ\7\"\2\2Z\13\3\2\2\2[`\7M\2\2\\]\7%\2\2]_\7M\2\2^\\\3\2\2\2_b\3\2\2"+
		"\2`^\3\2\2\2`a\3\2\2\2a\r\3\2\2\2b`\3\2\2\2cd\7\n\2\2de\5\4\3\2ef\7\25"+
		"\2\2f\17\3\2\2\2gh\7\13\2\2hi\7M\2\2ij\5\22\n\2jk\5\4\3\2kl\7\25\2\2l"+
		"\21\3\2\2\2mn\7!\2\2nz\7\"\2\2op\7!\2\2pu\7M\2\2qr\7\4\2\2rt\7M\2\2sq"+
		"\3\2\2\2tw\3\2\2\2us\3\2\2\2uv\3\2\2\2vx\3\2\2\2wu\3\2\2\2xz\7\"\2\2y"+
		"m\3\2\2\2yo\3\2\2\2z\23\3\2\2\2{|\7\f\2\2|}\7!\2\2}~\5\34\17\2~\177\7"+
		"\"\2\2\177\u0089\5\4\3\2\u0080\u0081\7\r\2\2\u0081\u0082\7\16\2\2\u0082"+
		"\u0083\7!\2\2\u0083\u0084\5\34\17\2\u0084\u0085\7\"\2\2\u0085\u0086\5"+
		"\4\3\2\u0086\u0088\3\2\2\2\u0087\u0080\3\2\2\2\u0088\u008b\3\2\2\2\u0089"+
		"\u0087\3\2\2\2\u0089\u008a\3\2\2\2\u008a\u008e\3\2\2\2\u008b\u0089\3\2"+
		"\2\2\u008c\u008d\7\r\2\2\u008d\u008f\5\4\3\2\u008e\u008c\3\2\2\2\u008e"+
		"\u008f\3\2\2\2\u008f\u0090\3\2\2\2\u0090\u0091\7\25\2\2\u0091\25\3\2\2"+
		"\2\u0092\u0093\7\21\2\2\u0093\u0094\7!\2\2\u0094\u0095\5\36\20\2\u0095"+
		"\u009b\7\"\2\2\u0096\u0097\7\22\2\2\u0097\u0098\t\2\2\2\u0098\u0099\5"+
		"\4\3\2\u0099\u009a\7\23\2\2\u009a\u009c\3\2\2\2\u009b\u0096\3\2\2\2\u009c"+
		"\u009d\3\2\2\2\u009d\u009b\3\2\2\2\u009d\u009e\3\2\2\2\u009e\u00a3\3\2"+
		"\2\2\u009f\u00a0\7\24\2\2\u00a0\u00a1\5\4\3\2\u00a1\u00a2\7\23\2\2\u00a2"+
		"\u00a4\3\2\2\2\u00a3\u009f\3\2\2\2\u00a3\u00a4\3\2\2\2\u00a4\u00a5\3\2"+
		"\2\2\u00a5\u00a6\7\25\2\2\u00a6\27\3\2\2\2\u00a7\u00a8\7\20\2\2\u00a8"+
		"\u00a9\7!\2\2\u00a9\u00aa\5\34\17\2\u00aa\u00ab\7\"\2\2\u00ab\u00ac\5"+
		"\4\3\2\u00ac\u00ad\7\25\2\2\u00ad\31\3\2\2\2\u00ae\u00af\7\17\2\2\u00af"+
		"\u00b0\7!\2\2\u00b0\u00b1\7M\2\2\u00b1\u00b2\7,\2\2\u00b2\u00b3\5\36\20"+
		"\2\u00b3\u00b4\7\"\2\2\u00b4\u00b5\5\4\3\2\u00b5\u00b6\7\25\2\2\u00b6"+
		"\33\3\2\2\2\u00b7\u00b8\b\17\1\2\u00b8\u00c4\5\36\20\2\u00b9\u00c4\7\34"+
		"\2\2\u00ba\u00c4\7\35\2\2\u00bb\u00c4\7\36\2\2\u00bc\u00c4\7\37\2\2\u00bd"+
		"\u00be\7!\2\2\u00be\u00bf\5\34\17\2\u00bf\u00c0\7\"\2\2\u00c0\u00c4\3"+
		"\2\2\2\u00c1\u00c2\t\3\2\2\u00c2\u00c4\5\34\17\20\u00c3\u00b7\3\2\2\2"+
		"\u00c3\u00b9\3\2\2\2\u00c3\u00ba\3\2\2\2\u00c3\u00bb\3\2\2\2\u00c3\u00bc"+
		"\3\2\2\2\u00c3\u00bd\3\2\2\2\u00c3\u00c1\3\2\2\2\u00c4\u00f0\3\2\2\2\u00c5"+
		"\u00c6\f\16\2\2\u00c6\u00c7\t\4\2\2\u00c7\u00ef\5\34\17\17\u00c8\u00c9"+
		"\f\r\2\2\u00c9\u00ca\t\5\2\2\u00ca\u00ef\5\34\17\16\u00cb\u00cc\f\f\2"+
		"\2\u00cc\u00cd\t\6\2\2\u00cd\u00ef\5\34\17\r\u00ce\u00cf\f\13\2\2\u00cf"+
		"\u00d0\t\7\2\2\u00d0\u00ef\5\34\17\f\u00d1\u00d2\f\n\2\2\u00d2\u00d3\t"+
		"\b\2\2\u00d3\u00ef\5\34\17\13\u00d4\u00d5\f\t\2\2\u00d5\u00d6\79\2\2\u00d6"+
		"\u00ef\5\34\17\n\u00d7\u00d8\f\b\2\2\u00d8\u00d9\7:\2\2\u00d9\u00ef\5"+
		"\34\17\t\u00da\u00db\f\7\2\2\u00db\u00dc\7;\2\2\u00dc\u00ef\5\34\17\b"+
		"\u00dd\u00de\f\6\2\2\u00de\u00df\7\61\2\2\u00df\u00ef\5\34\17\7\u00e0"+
		"\u00e1\f\5\2\2\u00e1\u00e2\7\62\2\2\u00e2\u00ef\5\34\17\6\u00e3\u00e4"+
		"\f\4\2\2\u00e4\u00e5\7+\2\2\u00e5\u00e6\5\34\17\2\u00e6\u00e7\7,\2\2\u00e7"+
		"\u00e8\5\34\17\5\u00e8\u00ef\3\2\2\2\u00e9\u00ea\f\3\2\2\u00ea\u00eb\t"+
		"\t\2\2\u00eb\u00ef\5\34\17\4\u00ec\u00ed\f\17\2\2\u00ed\u00ef\t\n\2\2"+
		"\u00ee\u00c5\3\2\2\2\u00ee\u00c8\3\2\2\2\u00ee\u00cb\3\2\2\2\u00ee\u00ce"+
		"\3\2\2\2\u00ee\u00d1\3\2\2\2\u00ee\u00d4\3\2\2\2\u00ee\u00d7\3\2\2\2\u00ee"+
		"\u00da\3\2\2\2\u00ee\u00dd\3\2\2\2\u00ee\u00e0\3\2\2\2\u00ee\u00e3\3\2"+
		"\2\2\u00ee\u00e9\3\2\2\2\u00ee\u00ec\3\2\2\2\u00ef\u00f2\3\2\2\2\u00f0"+
		"\u00ee\3\2\2\2\u00f0\u00f1\3\2\2\2\u00f1\35\3\2\2\2\u00f2\u00f0\3\2\2"+
		"\2\u00f3\u00f4\7\5\2\2\u00f4\u00f5\5 \21\2\u00f5\u00f6\7\6\2\2\u00f6\37"+
		"\3\2\2\2\u00f7\u00fe\7M\2\2\u00f8\u00fd\5\"\22\2\u00f9\u00fd\5$\23\2\u00fa"+
		"\u00fd\5&\24\2\u00fb\u00fd\5(\25\2\u00fc\u00f8\3\2\2\2\u00fc\u00f9\3\2"+
		"\2\2\u00fc\u00fa\3\2\2\2\u00fc\u00fb\3\2\2\2\u00fd\u0100\3\2\2\2\u00fe"+
		"\u00fc\3\2\2\2\u00fe\u00ff\3\2\2\2\u00ff!\3\2\2\2\u0100\u00fe\3\2\2\2"+
		"\u0101\u0102\7%\2\2\u0102\u0103\7M\2\2\u0103#\3\2\2\2\u0104\u0105\7#\2"+
		"\2\u0105\u0106\7\34\2\2\u0106\u0107\7$\2\2\u0107%\3\2\2\2\u0108\u0109"+
		"\7#\2\2\u0109\u010a\7\37\2\2\u010a\u010b\7$\2\2\u010b\'\3\2\2\2\u010c"+
		"\u010d\7%\2\2\u010d\u010e\7M\2\2\u010e\u010f\5*\26\2\u010f)\3\2\2\2\u0110"+
		"\u0111\7!\2\2\u0111\u011e\7\"\2\2\u0112\u0113\7!\2\2\u0113\u0118\5\34"+
		"\17\2\u0114\u0115\7\4\2\2\u0115\u0117\5\34\17\2\u0116\u0114\3\2\2\2\u0117"+
		"\u011a\3\2\2\2\u0118\u0116\3\2\2\2\u0118\u0119\3\2\2\2\u0119\u011b\3\2"+
		"\2\2\u011a\u0118\3\2\2\2\u011b\u011c\7\"\2\2\u011c\u011e\3\2\2\2\u011d"+
		"\u0110\3\2\2\2\u011d\u0112\3\2\2\2\u011e+\3\2\2\2\26-\63@BMP`uy\u0089"+
		"\u008e\u009d\u00a3\u00c3\u00ee\u00f0\u00fc\u00fe\u0118\u011d";
	public static final ATN _ATN =
		new ATNDeserializer().deserialize(_serializedATN.toCharArray());
	static {
		_decisionToDFA = new DFA[_ATN.getNumberOfDecisions()];
		for (int i = 0; i < _ATN.getNumberOfDecisions(); i++) {
			_decisionToDFA[i] = new DFA(_ATN.getDecisionState(i), i);
		}
	}
}