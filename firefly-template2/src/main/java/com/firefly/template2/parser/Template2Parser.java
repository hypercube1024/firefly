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
public class Template2Parser extends Parser {
	static { RuntimeMetaData.checkVersion("4.6", RuntimeMetaData.VERSION); }

	protected static final DFA[] _decisionToDFA;
	protected static final PredictionContextCache _sharedContextCache =
		new PredictionContextCache();
	public static final int
		T__0=1, T__1=2, T__2=3, T__3=4, EXTENDS=5, INCLUDE=6, SET=7, MAIN=8, FUNCTION=9, 
		IF=10, ELSE=11, THEN_IF=12, FOR=13, WHILE=14, SWITCH=15, CASE=16, BREAK=17, 
		DEFAULT=18, END=19, THIS=20, SUPER=21, OutputString=22, EscapeOutputString=23, 
		OutputNewLine=24, OutputSpace=25, COMMENT=26, LINE_COMMENT=27, IntegerLiteral=28, 
		FloatingPointLiteral=29, BooleanLiteral=30, StringLiteral=31, NullLiteral=32, 
		LPAREN=33, RPAREN=34, LBRACK=35, RBRACK=36, DOT=37, ASSIGN=38, GT=39, 
		LT=40, BANG=41, TILDE=42, QUESTION=43, COLON=44, EQUAL=45, LE=46, GE=47, 
		NOTEQUAL=48, AND=49, OR=50, INC=51, DEC=52, ADD=53, SUB=54, MUL=55, DIV=56, 
		BITAND=57, BITOR=58, CARET=59, MOD=60, ARROW=61, COLONCOLON=62, LSHIFT=63, 
		RSHIFT=64, URSHIFT=65, ADD_ASSIGN=66, SUB_ASSIGN=67, MUL_ASSIGN=68, DIV_ASSIGN=69, 
		AND_ASSIGN=70, OR_ASSIGN=71, XOR_ASSIGN=72, MOD_ASSIGN=73, LSHIFT_ASSIGN=74, 
		RSHIFT_ASSIGN=75, URSHIFT_ASSIGN=76, Identifier=77, WS=78;
	public static final int
		RULE_program = 0, RULE_templateBody = 1, RULE_extendTemplate = 2, RULE_include = 3, 
		RULE_set = 4, RULE_templatePath = 5, RULE_output = 6, RULE_mainFunction = 7, 
		RULE_functionDeclaration = 8, RULE_functionParameters = 9, RULE_selection = 10, 
		RULE_switchCondition = 11, RULE_whileLoop = 12, RULE_forLoop = 13, RULE_expression = 14, 
		RULE_beanAccess = 15, RULE_objectAccess = 16, RULE_propertyAccess = 17, 
		RULE_arrayAccess = 18, RULE_mapAccess = 19, RULE_methodCall = 20, RULE_callMethodParameters = 21;
	public static final String[] ruleNames = {
		"program", "templateBody", "extendTemplate", "include", "set", "templatePath", 
		"output", "mainFunction", "functionDeclaration", "functionParameters", 
		"selection", "switchCondition", "whileLoop", "forLoop", "expression", 
		"beanAccess", "objectAccess", "propertyAccess", "arrayAccess", "mapAccess", 
		"methodCall", "callMethodParameters"
	};

	private static final String[] _LITERAL_NAMES = {
		null, "';'", "','", "'${'", "'}'", "'#extends'", "'#include'", "'#set'", 
		"'#main'", "'#function'", "'#if'", "'#else'", "'if'", "'#for'", "'#while'", 
		"'#switch'", "'#case'", "'#break'", "'#default'", "'#end'", "'this'", 
		"'super'", null, null, "'&nl;'", "'&sp;'", null, null, null, null, null, 
		null, "'null'", "'('", "')'", "'['", "']'", "'.'", "'='", "'>'", "'<'", 
		"'!'", "'~'", "'?'", "':'", "'=='", "'<='", "'>='", "'!='", "'&&'", "'||'", 
		"'++'", "'--'", "'+'", "'-'", "'*'", "'/'", "'&'", "'|'", "'^'", "'%'", 
		"'->'", "'::'", "'<<'", "'>>'", "'>>>'", "'+='", "'-='", "'*='", "'/='", 
		"'&='", "'|='", "'^='", "'%='", "'<<='", "'>>='", "'>>>='"
	};
	private static final String[] _SYMBOLIC_NAMES = {
		null, null, null, null, null, "EXTENDS", "INCLUDE", "SET", "MAIN", "FUNCTION", 
		"IF", "ELSE", "THEN_IF", "FOR", "WHILE", "SWITCH", "CASE", "BREAK", "DEFAULT", 
		"END", "THIS", "SUPER", "OutputString", "EscapeOutputString", "OutputNewLine", 
		"OutputSpace", "COMMENT", "LINE_COMMENT", "IntegerLiteral", "FloatingPointLiteral", 
		"BooleanLiteral", "StringLiteral", "NullLiteral", "LPAREN", "RPAREN", 
		"LBRACK", "RBRACK", "DOT", "ASSIGN", "GT", "LT", "BANG", "TILDE", "QUESTION", 
		"COLON", "EQUAL", "LE", "GE", "NOTEQUAL", "AND", "OR", "INC", "DEC", "ADD", 
		"SUB", "MUL", "DIV", "BITAND", "BITOR", "CARET", "MOD", "ARROW", "COLONCOLON", 
		"LSHIFT", "RSHIFT", "URSHIFT", "ADD_ASSIGN", "SUB_ASSIGN", "MUL_ASSIGN", 
		"DIV_ASSIGN", "AND_ASSIGN", "OR_ASSIGN", "XOR_ASSIGN", "MOD_ASSIGN", "LSHIFT_ASSIGN", 
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
	public String getGrammarFileName() { return "Template2.g4"; }

	@Override
	public String[] getRuleNames() { return ruleNames; }

	@Override
	public String getSerializedATN() { return _serializedATN; }

	@Override
	public ATN getATN() { return _ATN; }

	public Template2Parser(TokenStream input) {
		super(input);
		_interp = new ParserATNSimulator(this,_ATN,_decisionToDFA,_sharedContextCache);
	}
	public static class ProgramContext extends ParserRuleContext {
		public ExtendTemplateContext extendTemplate() {
			return getRuleContext(ExtendTemplateContext.class,0);
		}
		public MainFunctionContext mainFunction() {
			return getRuleContext(MainFunctionContext.class,0);
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
			if ( listener instanceof Template2Listener ) ((Template2Listener)listener).enterProgram(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof Template2Listener ) ((Template2Listener)listener).exitProgram(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof Template2Visitor ) return ((Template2Visitor<? extends T>)visitor).visitProgram(this);
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
			setState(45);
			_errHandler.sync(this);
			_la = _input.LA(1);
			if (_la==EXTENDS) {
				{
				setState(44);
				extendTemplate();
				}
			}

			setState(48);
			_errHandler.sync(this);
			_la = _input.LA(1);
			if (_la==MAIN) {
				{
				setState(47);
				mainFunction();
				}
			}

			setState(53);
			_errHandler.sync(this);
			_la = _input.LA(1);
			while (_la==FUNCTION) {
				{
				{
				setState(50);
				functionDeclaration();
				}
				}
				setState(55);
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
		public List<OutputContext> output() {
			return getRuleContexts(OutputContext.class);
		}
		public OutputContext output(int i) {
			return getRuleContext(OutputContext.class,i);
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
		public List<SwitchConditionContext> switchCondition() {
			return getRuleContexts(SwitchConditionContext.class);
		}
		public SwitchConditionContext switchCondition(int i) {
			return getRuleContext(SwitchConditionContext.class,i);
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
			if ( listener instanceof Template2Listener ) ((Template2Listener)listener).enterTemplateBody(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof Template2Listener ) ((Template2Listener)listener).exitTemplateBody(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof Template2Visitor ) return ((Template2Visitor<? extends T>)visitor).visitTemplateBody(this);
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
			setState(66);
			_errHandler.sync(this);
			_la = _input.LA(1);
			while ((((_la) & ~0x3f) == 0 && ((1L << _la) & ((1L << T__2) | (1L << INCLUDE) | (1L << SET) | (1L << IF) | (1L << FOR) | (1L << WHILE) | (1L << SWITCH) | (1L << OutputString) | (1L << EscapeOutputString) | (1L << OutputNewLine) | (1L << OutputSpace))) != 0)) {
				{
				setState(64);
				_errHandler.sync(this);
				switch (_input.LA(1)) {
				case OutputString:
				case EscapeOutputString:
				case OutputNewLine:
				case OutputSpace:
					{
					setState(56);
					output();
					}
					break;
				case SET:
					{
					setState(57);
					set();
					}
					break;
				case INCLUDE:
					{
					setState(58);
					include();
					}
					break;
				case IF:
					{
					setState(59);
					selection();
					}
					break;
				case SWITCH:
					{
					setState(60);
					switchCondition();
					}
					break;
				case WHILE:
					{
					setState(61);
					whileLoop();
					}
					break;
				case FOR:
					{
					setState(62);
					forLoop();
					}
					break;
				case T__2:
					{
					setState(63);
					beanAccess();
					}
					break;
				default:
					throw new NoViableAltException(this);
				}
				}
				setState(68);
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

	public static class ExtendTemplateContext extends ParserRuleContext {
		public TerminalNode EXTENDS() { return getToken(Template2Parser.EXTENDS, 0); }
		public TemplatePathContext templatePath() {
			return getRuleContext(TemplatePathContext.class,0);
		}
		public ExtendTemplateContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_extendTemplate; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof Template2Listener ) ((Template2Listener)listener).enterExtendTemplate(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof Template2Listener ) ((Template2Listener)listener).exitExtendTemplate(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof Template2Visitor ) return ((Template2Visitor<? extends T>)visitor).visitExtendTemplate(this);
			else return visitor.visitChildren(this);
		}
	}

	public final ExtendTemplateContext extendTemplate() throws RecognitionException {
		ExtendTemplateContext _localctx = new ExtendTemplateContext(_ctx, getState());
		enterRule(_localctx, 4, RULE_extendTemplate);
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(69);
			match(EXTENDS);
			setState(70);
			templatePath();
			setState(71);
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
		public TerminalNode INCLUDE() { return getToken(Template2Parser.INCLUDE, 0); }
		public TerminalNode THIS() { return getToken(Template2Parser.THIS, 0); }
		public TerminalNode SUPER() { return getToken(Template2Parser.SUPER, 0); }
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
			if ( listener instanceof Template2Listener ) ((Template2Listener)listener).enterInclude(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof Template2Listener ) ((Template2Listener)listener).exitInclude(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof Template2Visitor ) return ((Template2Visitor<? extends T>)visitor).visitInclude(this);
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
			setState(73);
			match(INCLUDE);
			setState(74);
			match(LPAREN);
			setState(78);
			_errHandler.sync(this);
			switch (_input.LA(1)) {
			case THIS:
				{
				setState(75);
				match(THIS);
				}
				break;
			case SUPER:
				{
				setState(76);
				match(SUPER);
				}
				break;
			case Identifier:
				{
				setState(77);
				templatePath();
				}
				break;
			default:
				throw new NoViableAltException(this);
			}
			setState(81);
			_errHandler.sync(this);
			_la = _input.LA(1);
			if (_la==DOT) {
				{
				setState(80);
				methodCall();
				}
			}

			setState(83);
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
		public TerminalNode SET() { return getToken(Template2Parser.SET, 0); }
		public TerminalNode Identifier() { return getToken(Template2Parser.Identifier, 0); }
		public ExpressionContext expression() {
			return getRuleContext(ExpressionContext.class,0);
		}
		public SetContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_set; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof Template2Listener ) ((Template2Listener)listener).enterSet(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof Template2Listener ) ((Template2Listener)listener).exitSet(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof Template2Visitor ) return ((Template2Visitor<? extends T>)visitor).visitSet(this);
			else return visitor.visitChildren(this);
		}
	}

	public final SetContext set() throws RecognitionException {
		SetContext _localctx = new SetContext(_ctx, getState());
		enterRule(_localctx, 8, RULE_set);
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(85);
			match(SET);
			setState(86);
			match(LPAREN);
			setState(87);
			match(Identifier);
			setState(88);
			match(ASSIGN);
			setState(89);
			expression(0);
			setState(90);
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
		public List<TerminalNode> Identifier() { return getTokens(Template2Parser.Identifier); }
		public TerminalNode Identifier(int i) {
			return getToken(Template2Parser.Identifier, i);
		}
		public TemplatePathContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_templatePath; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof Template2Listener ) ((Template2Listener)listener).enterTemplatePath(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof Template2Listener ) ((Template2Listener)listener).exitTemplatePath(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof Template2Visitor ) return ((Template2Visitor<? extends T>)visitor).visitTemplatePath(this);
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
			setState(92);
			match(Identifier);
			setState(97);
			_errHandler.sync(this);
			_alt = getInterpreter().adaptivePredict(_input,7,_ctx);
			while ( _alt!=2 && _alt!= ATN.INVALID_ALT_NUMBER ) {
				if ( _alt==1 ) {
					{
					{
					setState(93);
					match(DOT);
					setState(94);
					match(Identifier);
					}
					} 
				}
				setState(99);
				_errHandler.sync(this);
				_alt = getInterpreter().adaptivePredict(_input,7,_ctx);
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

	public static class OutputContext extends ParserRuleContext {
		public TerminalNode OutputString() { return getToken(Template2Parser.OutputString, 0); }
		public TerminalNode EscapeOutputString() { return getToken(Template2Parser.EscapeOutputString, 0); }
		public TerminalNode OutputNewLine() { return getToken(Template2Parser.OutputNewLine, 0); }
		public TerminalNode OutputSpace() { return getToken(Template2Parser.OutputSpace, 0); }
		public OutputContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_output; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof Template2Listener ) ((Template2Listener)listener).enterOutput(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof Template2Listener ) ((Template2Listener)listener).exitOutput(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof Template2Visitor ) return ((Template2Visitor<? extends T>)visitor).visitOutput(this);
			else return visitor.visitChildren(this);
		}
	}

	public final OutputContext output() throws RecognitionException {
		OutputContext _localctx = new OutputContext(_ctx, getState());
		enterRule(_localctx, 12, RULE_output);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(100);
			_la = _input.LA(1);
			if ( !((((_la) & ~0x3f) == 0 && ((1L << _la) & ((1L << OutputString) | (1L << EscapeOutputString) | (1L << OutputNewLine) | (1L << OutputSpace))) != 0)) ) {
			_errHandler.recoverInline(this);
			}
			else {
				if ( _input.LA(1)==Token.EOF ) matchedEOF = true;
				_errHandler.reportMatch(this);
				consume();
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
		public TerminalNode MAIN() { return getToken(Template2Parser.MAIN, 0); }
		public TemplateBodyContext templateBody() {
			return getRuleContext(TemplateBodyContext.class,0);
		}
		public TerminalNode END() { return getToken(Template2Parser.END, 0); }
		public MainFunctionContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_mainFunction; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof Template2Listener ) ((Template2Listener)listener).enterMainFunction(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof Template2Listener ) ((Template2Listener)listener).exitMainFunction(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof Template2Visitor ) return ((Template2Visitor<? extends T>)visitor).visitMainFunction(this);
			else return visitor.visitChildren(this);
		}
	}

	public final MainFunctionContext mainFunction() throws RecognitionException {
		MainFunctionContext _localctx = new MainFunctionContext(_ctx, getState());
		enterRule(_localctx, 14, RULE_mainFunction);
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(102);
			match(MAIN);
			setState(103);
			templateBody();
			setState(104);
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
		public TerminalNode FUNCTION() { return getToken(Template2Parser.FUNCTION, 0); }
		public TerminalNode Identifier() { return getToken(Template2Parser.Identifier, 0); }
		public FunctionParametersContext functionParameters() {
			return getRuleContext(FunctionParametersContext.class,0);
		}
		public TemplateBodyContext templateBody() {
			return getRuleContext(TemplateBodyContext.class,0);
		}
		public TerminalNode END() { return getToken(Template2Parser.END, 0); }
		public FunctionDeclarationContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_functionDeclaration; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof Template2Listener ) ((Template2Listener)listener).enterFunctionDeclaration(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof Template2Listener ) ((Template2Listener)listener).exitFunctionDeclaration(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof Template2Visitor ) return ((Template2Visitor<? extends T>)visitor).visitFunctionDeclaration(this);
			else return visitor.visitChildren(this);
		}
	}

	public final FunctionDeclarationContext functionDeclaration() throws RecognitionException {
		FunctionDeclarationContext _localctx = new FunctionDeclarationContext(_ctx, getState());
		enterRule(_localctx, 16, RULE_functionDeclaration);
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(106);
			match(FUNCTION);
			setState(107);
			match(Identifier);
			setState(108);
			functionParameters();
			setState(109);
			templateBody();
			setState(110);
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
		public List<TerminalNode> Identifier() { return getTokens(Template2Parser.Identifier); }
		public TerminalNode Identifier(int i) {
			return getToken(Template2Parser.Identifier, i);
		}
		public FunctionParametersContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_functionParameters; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof Template2Listener ) ((Template2Listener)listener).enterFunctionParameters(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof Template2Listener ) ((Template2Listener)listener).exitFunctionParameters(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof Template2Visitor ) return ((Template2Visitor<? extends T>)visitor).visitFunctionParameters(this);
			else return visitor.visitChildren(this);
		}
	}

	public final FunctionParametersContext functionParameters() throws RecognitionException {
		FunctionParametersContext _localctx = new FunctionParametersContext(_ctx, getState());
		enterRule(_localctx, 18, RULE_functionParameters);
		int _la;
		try {
			setState(124);
			_errHandler.sync(this);
			switch ( getInterpreter().adaptivePredict(_input,9,_ctx) ) {
			case 1:
				enterOuterAlt(_localctx, 1);
				{
				setState(112);
				match(LPAREN);
				setState(113);
				match(RPAREN);
				}
				break;
			case 2:
				enterOuterAlt(_localctx, 2);
				{
				setState(114);
				match(LPAREN);
				setState(115);
				match(Identifier);
				setState(120);
				_errHandler.sync(this);
				_la = _input.LA(1);
				while (_la==T__1) {
					{
					{
					setState(116);
					match(T__1);
					setState(117);
					match(Identifier);
					}
					}
					setState(122);
					_errHandler.sync(this);
					_la = _input.LA(1);
				}
				setState(123);
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
		public TerminalNode IF() { return getToken(Template2Parser.IF, 0); }
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
		public TerminalNode END() { return getToken(Template2Parser.END, 0); }
		public List<TerminalNode> ELSE() { return getTokens(Template2Parser.ELSE); }
		public TerminalNode ELSE(int i) {
			return getToken(Template2Parser.ELSE, i);
		}
		public List<TerminalNode> THEN_IF() { return getTokens(Template2Parser.THEN_IF); }
		public TerminalNode THEN_IF(int i) {
			return getToken(Template2Parser.THEN_IF, i);
		}
		public SelectionContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_selection; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof Template2Listener ) ((Template2Listener)listener).enterSelection(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof Template2Listener ) ((Template2Listener)listener).exitSelection(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof Template2Visitor ) return ((Template2Visitor<? extends T>)visitor).visitSelection(this);
			else return visitor.visitChildren(this);
		}
	}

	public final SelectionContext selection() throws RecognitionException {
		SelectionContext _localctx = new SelectionContext(_ctx, getState());
		enterRule(_localctx, 20, RULE_selection);
		int _la;
		try {
			int _alt;
			enterOuterAlt(_localctx, 1);
			{
			setState(126);
			match(IF);
			setState(127);
			match(LPAREN);
			setState(128);
			expression(0);
			setState(129);
			match(RPAREN);
			setState(130);
			templateBody();
			setState(140);
			_errHandler.sync(this);
			_alt = getInterpreter().adaptivePredict(_input,10,_ctx);
			while ( _alt!=2 && _alt!= ATN.INVALID_ALT_NUMBER ) {
				if ( _alt==1 ) {
					{
					{
					setState(131);
					match(ELSE);
					setState(132);
					match(THEN_IF);
					setState(133);
					match(LPAREN);
					setState(134);
					expression(0);
					setState(135);
					match(RPAREN);
					setState(136);
					templateBody();
					}
					} 
				}
				setState(142);
				_errHandler.sync(this);
				_alt = getInterpreter().adaptivePredict(_input,10,_ctx);
			}
			setState(145);
			_errHandler.sync(this);
			_la = _input.LA(1);
			if (_la==ELSE) {
				{
				setState(143);
				match(ELSE);
				setState(144);
				templateBody();
				}
			}

			setState(147);
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

	public static class SwitchConditionContext extends ParserRuleContext {
		public TerminalNode SWITCH() { return getToken(Template2Parser.SWITCH, 0); }
		public BeanAccessContext beanAccess() {
			return getRuleContext(BeanAccessContext.class,0);
		}
		public TerminalNode END() { return getToken(Template2Parser.END, 0); }
		public List<TerminalNode> CASE() { return getTokens(Template2Parser.CASE); }
		public TerminalNode CASE(int i) {
			return getToken(Template2Parser.CASE, i);
		}
		public List<TemplateBodyContext> templateBody() {
			return getRuleContexts(TemplateBodyContext.class);
		}
		public TemplateBodyContext templateBody(int i) {
			return getRuleContext(TemplateBodyContext.class,i);
		}
		public List<TerminalNode> BREAK() { return getTokens(Template2Parser.BREAK); }
		public TerminalNode BREAK(int i) {
			return getToken(Template2Parser.BREAK, i);
		}
		public TerminalNode DEFAULT() { return getToken(Template2Parser.DEFAULT, 0); }
		public List<TerminalNode> StringLiteral() { return getTokens(Template2Parser.StringLiteral); }
		public TerminalNode StringLiteral(int i) {
			return getToken(Template2Parser.StringLiteral, i);
		}
		public List<TerminalNode> IntegerLiteral() { return getTokens(Template2Parser.IntegerLiteral); }
		public TerminalNode IntegerLiteral(int i) {
			return getToken(Template2Parser.IntegerLiteral, i);
		}
		public SwitchConditionContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_switchCondition; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof Template2Listener ) ((Template2Listener)listener).enterSwitchCondition(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof Template2Listener ) ((Template2Listener)listener).exitSwitchCondition(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof Template2Visitor ) return ((Template2Visitor<? extends T>)visitor).visitSwitchCondition(this);
			else return visitor.visitChildren(this);
		}
	}

	public final SwitchConditionContext switchCondition() throws RecognitionException {
		SwitchConditionContext _localctx = new SwitchConditionContext(_ctx, getState());
		enterRule(_localctx, 22, RULE_switchCondition);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(149);
			match(SWITCH);
			setState(150);
			match(LPAREN);
			setState(151);
			beanAccess();
			setState(152);
			match(RPAREN);
			setState(158); 
			_errHandler.sync(this);
			_la = _input.LA(1);
			do {
				{
				{
				setState(153);
				match(CASE);
				setState(154);
				_la = _input.LA(1);
				if ( !(_la==IntegerLiteral || _la==StringLiteral) ) {
				_errHandler.recoverInline(this);
				}
				else {
					if ( _input.LA(1)==Token.EOF ) matchedEOF = true;
					_errHandler.reportMatch(this);
					consume();
				}
				setState(155);
				templateBody();
				setState(156);
				match(BREAK);
				}
				}
				setState(160); 
				_errHandler.sync(this);
				_la = _input.LA(1);
			} while ( _la==CASE );
			setState(166);
			_errHandler.sync(this);
			_la = _input.LA(1);
			if (_la==DEFAULT) {
				{
				setState(162);
				match(DEFAULT);
				setState(163);
				templateBody();
				setState(164);
				match(BREAK);
				}
			}

			setState(168);
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
		public TerminalNode WHILE() { return getToken(Template2Parser.WHILE, 0); }
		public ExpressionContext expression() {
			return getRuleContext(ExpressionContext.class,0);
		}
		public TemplateBodyContext templateBody() {
			return getRuleContext(TemplateBodyContext.class,0);
		}
		public TerminalNode END() { return getToken(Template2Parser.END, 0); }
		public WhileLoopContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_whileLoop; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof Template2Listener ) ((Template2Listener)listener).enterWhileLoop(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof Template2Listener ) ((Template2Listener)listener).exitWhileLoop(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof Template2Visitor ) return ((Template2Visitor<? extends T>)visitor).visitWhileLoop(this);
			else return visitor.visitChildren(this);
		}
	}

	public final WhileLoopContext whileLoop() throws RecognitionException {
		WhileLoopContext _localctx = new WhileLoopContext(_ctx, getState());
		enterRule(_localctx, 24, RULE_whileLoop);
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(170);
			match(WHILE);
			setState(171);
			match(LPAREN);
			setState(172);
			expression(0);
			setState(173);
			match(RPAREN);
			setState(174);
			templateBody();
			setState(175);
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
		public TerminalNode FOR() { return getToken(Template2Parser.FOR, 0); }
		public TerminalNode Identifier() { return getToken(Template2Parser.Identifier, 0); }
		public BeanAccessContext beanAccess() {
			return getRuleContext(BeanAccessContext.class,0);
		}
		public TemplateBodyContext templateBody() {
			return getRuleContext(TemplateBodyContext.class,0);
		}
		public TerminalNode END() { return getToken(Template2Parser.END, 0); }
		public ForLoopContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_forLoop; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof Template2Listener ) ((Template2Listener)listener).enterForLoop(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof Template2Listener ) ((Template2Listener)listener).exitForLoop(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof Template2Visitor ) return ((Template2Visitor<? extends T>)visitor).visitForLoop(this);
			else return visitor.visitChildren(this);
		}
	}

	public final ForLoopContext forLoop() throws RecognitionException {
		ForLoopContext _localctx = new ForLoopContext(_ctx, getState());
		enterRule(_localctx, 26, RULE_forLoop);
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(177);
			match(FOR);
			setState(178);
			match(LPAREN);
			setState(179);
			match(Identifier);
			setState(180);
			match(COLON);
			setState(181);
			beanAccess();
			setState(182);
			match(RPAREN);
			setState(183);
			templateBody();
			setState(184);
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
			if ( listener instanceof Template2Listener ) ((Template2Listener)listener).enterMulDivMod(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof Template2Listener ) ((Template2Listener)listener).exitMulDivMod(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof Template2Visitor ) return ((Template2Visitor<? extends T>)visitor).visitMulDivMod(this);
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
			if ( listener instanceof Template2Listener ) ((Template2Listener)listener).enterSuffixUnary(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof Template2Listener ) ((Template2Listener)listener).exitSuffixUnary(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof Template2Visitor ) return ((Template2Visitor<? extends T>)visitor).visitSuffixUnary(this);
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
			if ( listener instanceof Template2Listener ) ((Template2Listener)listener).enterParens(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof Template2Listener ) ((Template2Listener)listener).exitParens(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof Template2Visitor ) return ((Template2Visitor<? extends T>)visitor).visitParens(this);
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
			if ( listener instanceof Template2Listener ) ((Template2Listener)listener).enterBitOr(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof Template2Listener ) ((Template2Listener)listener).exitBitOr(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof Template2Visitor ) return ((Template2Visitor<? extends T>)visitor).visitBitOr(this);
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
			if ( listener instanceof Template2Listener ) ((Template2Listener)listener).enterAssignment(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof Template2Listener ) ((Template2Listener)listener).exitAssignment(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof Template2Visitor ) return ((Template2Visitor<? extends T>)visitor).visitAssignment(this);
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
			if ( listener instanceof Template2Listener ) ((Template2Listener)listener).enterShift(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof Template2Listener ) ((Template2Listener)listener).exitShift(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof Template2Visitor ) return ((Template2Visitor<? extends T>)visitor).visitShift(this);
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
			if ( listener instanceof Template2Listener ) ((Template2Listener)listener).enterAddSub(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof Template2Listener ) ((Template2Listener)listener).exitAddSub(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof Template2Visitor ) return ((Template2Visitor<? extends T>)visitor).visitAddSub(this);
			else return visitor.visitChildren(this);
		}
	}
	public static class FloatingPointLiteralContext extends ExpressionContext {
		public TerminalNode FloatingPointLiteral() { return getToken(Template2Parser.FloatingPointLiteral, 0); }
		public FloatingPointLiteralContext(ExpressionContext ctx) { copyFrom(ctx); }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof Template2Listener ) ((Template2Listener)listener).enterFloatingPointLiteral(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof Template2Listener ) ((Template2Listener)listener).exitFloatingPointLiteral(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof Template2Visitor ) return ((Template2Visitor<? extends T>)visitor).visitFloatingPointLiteral(this);
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
			if ( listener instanceof Template2Listener ) ((Template2Listener)listener).enterShortCircuitOr(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof Template2Listener ) ((Template2Listener)listener).exitShortCircuitOr(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof Template2Visitor ) return ((Template2Visitor<? extends T>)visitor).visitShortCircuitOr(this);
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
			if ( listener instanceof Template2Listener ) ((Template2Listener)listener).enterPrefixUnary(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof Template2Listener ) ((Template2Listener)listener).exitPrefixUnary(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof Template2Visitor ) return ((Template2Visitor<? extends T>)visitor).visitPrefixUnary(this);
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
			if ( listener instanceof Template2Listener ) ((Template2Listener)listener).enterEqualOrNotEqual(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof Template2Listener ) ((Template2Listener)listener).exitEqualOrNotEqual(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof Template2Visitor ) return ((Template2Visitor<? extends T>)visitor).visitEqualOrNotEqual(this);
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
			if ( listener instanceof Template2Listener ) ((Template2Listener)listener).enterBitAnd(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof Template2Listener ) ((Template2Listener)listener).exitBitAnd(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof Template2Visitor ) return ((Template2Visitor<? extends T>)visitor).visitBitAnd(this);
			else return visitor.visitChildren(this);
		}
	}
	public static class StringLiteralContext extends ExpressionContext {
		public TerminalNode StringLiteral() { return getToken(Template2Parser.StringLiteral, 0); }
		public StringLiteralContext(ExpressionContext ctx) { copyFrom(ctx); }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof Template2Listener ) ((Template2Listener)listener).enterStringLiteral(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof Template2Listener ) ((Template2Listener)listener).exitStringLiteral(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof Template2Visitor ) return ((Template2Visitor<? extends T>)visitor).visitStringLiteral(this);
			else return visitor.visitChildren(this);
		}
	}
	public static class IntegerLiteralContext extends ExpressionContext {
		public TerminalNode IntegerLiteral() { return getToken(Template2Parser.IntegerLiteral, 0); }
		public IntegerLiteralContext(ExpressionContext ctx) { copyFrom(ctx); }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof Template2Listener ) ((Template2Listener)listener).enterIntegerLiteral(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof Template2Listener ) ((Template2Listener)listener).exitIntegerLiteral(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof Template2Visitor ) return ((Template2Visitor<? extends T>)visitor).visitIntegerLiteral(this);
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
			if ( listener instanceof Template2Listener ) ((Template2Listener)listener).enterXor(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof Template2Listener ) ((Template2Listener)listener).exitXor(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof Template2Visitor ) return ((Template2Visitor<? extends T>)visitor).visitXor(this);
			else return visitor.visitChildren(this);
		}
	}
	public static class BooleanLiteralContext extends ExpressionContext {
		public TerminalNode BooleanLiteral() { return getToken(Template2Parser.BooleanLiteral, 0); }
		public BooleanLiteralContext(ExpressionContext ctx) { copyFrom(ctx); }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof Template2Listener ) ((Template2Listener)listener).enterBooleanLiteral(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof Template2Listener ) ((Template2Listener)listener).exitBooleanLiteral(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof Template2Visitor ) return ((Template2Visitor<? extends T>)visitor).visitBooleanLiteral(this);
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
			if ( listener instanceof Template2Listener ) ((Template2Listener)listener).enterTernary(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof Template2Listener ) ((Template2Listener)listener).exitTernary(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof Template2Visitor ) return ((Template2Visitor<? extends T>)visitor).visitTernary(this);
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
			if ( listener instanceof Template2Listener ) ((Template2Listener)listener).enterBean(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof Template2Listener ) ((Template2Listener)listener).exitBean(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof Template2Visitor ) return ((Template2Visitor<? extends T>)visitor).visitBean(this);
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
			if ( listener instanceof Template2Listener ) ((Template2Listener)listener).enterShortCircuitAnd(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof Template2Listener ) ((Template2Listener)listener).exitShortCircuitAnd(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof Template2Visitor ) return ((Template2Visitor<? extends T>)visitor).visitShortCircuitAnd(this);
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
			if ( listener instanceof Template2Listener ) ((Template2Listener)listener).enterGreatOrLess(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof Template2Listener ) ((Template2Listener)listener).exitGreatOrLess(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof Template2Visitor ) return ((Template2Visitor<? extends T>)visitor).visitGreatOrLess(this);
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
		int _startState = 28;
		enterRecursionRule(_localctx, 28, RULE_expression, _p);
		int _la;
		try {
			int _alt;
			enterOuterAlt(_localctx, 1);
			{
			setState(198);
			_errHandler.sync(this);
			switch (_input.LA(1)) {
			case T__2:
				{
				_localctx = new BeanContext(_localctx);
				_ctx = _localctx;
				_prevctx = _localctx;

				setState(187);
				beanAccess();
				}
				break;
			case IntegerLiteral:
				{
				_localctx = new IntegerLiteralContext(_localctx);
				_ctx = _localctx;
				_prevctx = _localctx;
				setState(188);
				match(IntegerLiteral);
				}
				break;
			case FloatingPointLiteral:
				{
				_localctx = new FloatingPointLiteralContext(_localctx);
				_ctx = _localctx;
				_prevctx = _localctx;
				setState(189);
				match(FloatingPointLiteral);
				}
				break;
			case BooleanLiteral:
				{
				_localctx = new BooleanLiteralContext(_localctx);
				_ctx = _localctx;
				_prevctx = _localctx;
				setState(190);
				match(BooleanLiteral);
				}
				break;
			case StringLiteral:
				{
				_localctx = new StringLiteralContext(_localctx);
				_ctx = _localctx;
				_prevctx = _localctx;
				setState(191);
				match(StringLiteral);
				}
				break;
			case LPAREN:
				{
				_localctx = new ParensContext(_localctx);
				_ctx = _localctx;
				_prevctx = _localctx;
				setState(192);
				match(LPAREN);
				setState(193);
				expression(0);
				setState(194);
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
				setState(196);
				_la = _input.LA(1);
				if ( !((((_la) & ~0x3f) == 0 && ((1L << _la) & ((1L << BANG) | (1L << TILDE) | (1L << INC) | (1L << DEC) | (1L << ADD) | (1L << SUB))) != 0)) ) {
				_errHandler.recoverInline(this);
				}
				else {
					if ( _input.LA(1)==Token.EOF ) matchedEOF = true;
					_errHandler.reportMatch(this);
					consume();
				}
				setState(197);
				expression(14);
				}
				break;
			default:
				throw new NoViableAltException(this);
			}
			_ctx.stop = _input.LT(-1);
			setState(243);
			_errHandler.sync(this);
			_alt = getInterpreter().adaptivePredict(_input,16,_ctx);
			while ( _alt!=2 && _alt!= ATN.INVALID_ALT_NUMBER ) {
				if ( _alt==1 ) {
					if ( _parseListeners!=null ) triggerExitRuleEvent();
					_prevctx = _localctx;
					{
					setState(241);
					_errHandler.sync(this);
					switch ( getInterpreter().adaptivePredict(_input,15,_ctx) ) {
					case 1:
						{
						_localctx = new MulDivModContext(new ExpressionContext(_parentctx, _parentState));
						pushNewRecursionContext(_localctx, _startState, RULE_expression);
						setState(200);
						if (!(precpred(_ctx, 12))) throw new FailedPredicateException(this, "precpred(_ctx, 12)");
						setState(201);
						_la = _input.LA(1);
						if ( !((((_la) & ~0x3f) == 0 && ((1L << _la) & ((1L << MUL) | (1L << DIV) | (1L << MOD))) != 0)) ) {
						_errHandler.recoverInline(this);
						}
						else {
							if ( _input.LA(1)==Token.EOF ) matchedEOF = true;
							_errHandler.reportMatch(this);
							consume();
						}
						setState(202);
						expression(13);
						}
						break;
					case 2:
						{
						_localctx = new AddSubContext(new ExpressionContext(_parentctx, _parentState));
						pushNewRecursionContext(_localctx, _startState, RULE_expression);
						setState(203);
						if (!(precpred(_ctx, 11))) throw new FailedPredicateException(this, "precpred(_ctx, 11)");
						setState(204);
						_la = _input.LA(1);
						if ( !(_la==ADD || _la==SUB) ) {
						_errHandler.recoverInline(this);
						}
						else {
							if ( _input.LA(1)==Token.EOF ) matchedEOF = true;
							_errHandler.reportMatch(this);
							consume();
						}
						setState(205);
						expression(12);
						}
						break;
					case 3:
						{
						_localctx = new ShiftContext(new ExpressionContext(_parentctx, _parentState));
						pushNewRecursionContext(_localctx, _startState, RULE_expression);
						setState(206);
						if (!(precpred(_ctx, 10))) throw new FailedPredicateException(this, "precpred(_ctx, 10)");
						setState(207);
						_la = _input.LA(1);
						if ( !(((((_la - 63)) & ~0x3f) == 0 && ((1L << (_la - 63)) & ((1L << (LSHIFT - 63)) | (1L << (RSHIFT - 63)) | (1L << (URSHIFT - 63)))) != 0)) ) {
						_errHandler.recoverInline(this);
						}
						else {
							if ( _input.LA(1)==Token.EOF ) matchedEOF = true;
							_errHandler.reportMatch(this);
							consume();
						}
						setState(208);
						expression(11);
						}
						break;
					case 4:
						{
						_localctx = new GreatOrLessContext(new ExpressionContext(_parentctx, _parentState));
						pushNewRecursionContext(_localctx, _startState, RULE_expression);
						setState(209);
						if (!(precpred(_ctx, 9))) throw new FailedPredicateException(this, "precpred(_ctx, 9)");
						setState(210);
						_la = _input.LA(1);
						if ( !((((_la) & ~0x3f) == 0 && ((1L << _la) & ((1L << GT) | (1L << LT) | (1L << LE) | (1L << GE))) != 0)) ) {
						_errHandler.recoverInline(this);
						}
						else {
							if ( _input.LA(1)==Token.EOF ) matchedEOF = true;
							_errHandler.reportMatch(this);
							consume();
						}
						setState(211);
						expression(10);
						}
						break;
					case 5:
						{
						_localctx = new EqualOrNotEqualContext(new ExpressionContext(_parentctx, _parentState));
						pushNewRecursionContext(_localctx, _startState, RULE_expression);
						setState(212);
						if (!(precpred(_ctx, 8))) throw new FailedPredicateException(this, "precpred(_ctx, 8)");
						setState(213);
						_la = _input.LA(1);
						if ( !(_la==EQUAL || _la==NOTEQUAL) ) {
						_errHandler.recoverInline(this);
						}
						else {
							if ( _input.LA(1)==Token.EOF ) matchedEOF = true;
							_errHandler.reportMatch(this);
							consume();
						}
						setState(214);
						expression(9);
						}
						break;
					case 6:
						{
						_localctx = new BitAndContext(new ExpressionContext(_parentctx, _parentState));
						pushNewRecursionContext(_localctx, _startState, RULE_expression);
						setState(215);
						if (!(precpred(_ctx, 7))) throw new FailedPredicateException(this, "precpred(_ctx, 7)");
						setState(216);
						match(BITAND);
						setState(217);
						expression(8);
						}
						break;
					case 7:
						{
						_localctx = new BitOrContext(new ExpressionContext(_parentctx, _parentState));
						pushNewRecursionContext(_localctx, _startState, RULE_expression);
						setState(218);
						if (!(precpred(_ctx, 6))) throw new FailedPredicateException(this, "precpred(_ctx, 6)");
						setState(219);
						match(BITOR);
						setState(220);
						expression(7);
						}
						break;
					case 8:
						{
						_localctx = new XorContext(new ExpressionContext(_parentctx, _parentState));
						pushNewRecursionContext(_localctx, _startState, RULE_expression);
						setState(221);
						if (!(precpred(_ctx, 5))) throw new FailedPredicateException(this, "precpred(_ctx, 5)");
						setState(222);
						match(CARET);
						setState(223);
						expression(6);
						}
						break;
					case 9:
						{
						_localctx = new ShortCircuitAndContext(new ExpressionContext(_parentctx, _parentState));
						pushNewRecursionContext(_localctx, _startState, RULE_expression);
						setState(224);
						if (!(precpred(_ctx, 4))) throw new FailedPredicateException(this, "precpred(_ctx, 4)");
						setState(225);
						match(AND);
						setState(226);
						expression(5);
						}
						break;
					case 10:
						{
						_localctx = new ShortCircuitOrContext(new ExpressionContext(_parentctx, _parentState));
						pushNewRecursionContext(_localctx, _startState, RULE_expression);
						setState(227);
						if (!(precpred(_ctx, 3))) throw new FailedPredicateException(this, "precpred(_ctx, 3)");
						setState(228);
						match(OR);
						setState(229);
						expression(4);
						}
						break;
					case 11:
						{
						_localctx = new TernaryContext(new ExpressionContext(_parentctx, _parentState));
						pushNewRecursionContext(_localctx, _startState, RULE_expression);
						setState(230);
						if (!(precpred(_ctx, 2))) throw new FailedPredicateException(this, "precpred(_ctx, 2)");
						setState(231);
						match(QUESTION);
						setState(232);
						expression(0);
						setState(233);
						match(COLON);
						setState(234);
						expression(3);
						}
						break;
					case 12:
						{
						_localctx = new AssignmentContext(new ExpressionContext(_parentctx, _parentState));
						pushNewRecursionContext(_localctx, _startState, RULE_expression);
						setState(236);
						if (!(precpred(_ctx, 1))) throw new FailedPredicateException(this, "precpred(_ctx, 1)");
						setState(237);
						_la = _input.LA(1);
						if ( !(((((_la - 38)) & ~0x3f) == 0 && ((1L << (_la - 38)) & ((1L << (ASSIGN - 38)) | (1L << (ADD_ASSIGN - 38)) | (1L << (SUB_ASSIGN - 38)) | (1L << (MUL_ASSIGN - 38)) | (1L << (DIV_ASSIGN - 38)) | (1L << (AND_ASSIGN - 38)) | (1L << (OR_ASSIGN - 38)) | (1L << (XOR_ASSIGN - 38)) | (1L << (MOD_ASSIGN - 38)) | (1L << (LSHIFT_ASSIGN - 38)) | (1L << (RSHIFT_ASSIGN - 38)) | (1L << (URSHIFT_ASSIGN - 38)))) != 0)) ) {
						_errHandler.recoverInline(this);
						}
						else {
							if ( _input.LA(1)==Token.EOF ) matchedEOF = true;
							_errHandler.reportMatch(this);
							consume();
						}
						setState(238);
						expression(2);
						}
						break;
					case 13:
						{
						_localctx = new SuffixUnaryContext(new ExpressionContext(_parentctx, _parentState));
						pushNewRecursionContext(_localctx, _startState, RULE_expression);
						setState(239);
						if (!(precpred(_ctx, 13))) throw new FailedPredicateException(this, "precpred(_ctx, 13)");
						setState(240);
						_la = _input.LA(1);
						if ( !(_la==INC || _la==DEC) ) {
						_errHandler.recoverInline(this);
						}
						else {
							if ( _input.LA(1)==Token.EOF ) matchedEOF = true;
							_errHandler.reportMatch(this);
							consume();
						}
						}
						break;
					}
					} 
				}
				setState(245);
				_errHandler.sync(this);
				_alt = getInterpreter().adaptivePredict(_input,16,_ctx);
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
			if ( listener instanceof Template2Listener ) ((Template2Listener)listener).enterBeanAccess(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof Template2Listener ) ((Template2Listener)listener).exitBeanAccess(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof Template2Visitor ) return ((Template2Visitor<? extends T>)visitor).visitBeanAccess(this);
			else return visitor.visitChildren(this);
		}
	}

	public final BeanAccessContext beanAccess() throws RecognitionException {
		BeanAccessContext _localctx = new BeanAccessContext(_ctx, getState());
		enterRule(_localctx, 30, RULE_beanAccess);
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(246);
			match(T__2);
			setState(247);
			objectAccess();
			setState(248);
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
		public TerminalNode Identifier() { return getToken(Template2Parser.Identifier, 0); }
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
			if ( listener instanceof Template2Listener ) ((Template2Listener)listener).enterObjectAccess(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof Template2Listener ) ((Template2Listener)listener).exitObjectAccess(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof Template2Visitor ) return ((Template2Visitor<? extends T>)visitor).visitObjectAccess(this);
			else return visitor.visitChildren(this);
		}
	}

	public final ObjectAccessContext objectAccess() throws RecognitionException {
		ObjectAccessContext _localctx = new ObjectAccessContext(_ctx, getState());
		enterRule(_localctx, 32, RULE_objectAccess);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(250);
			match(Identifier);
			setState(257);
			_errHandler.sync(this);
			_la = _input.LA(1);
			while (_la==LBRACK || _la==DOT) {
				{
				setState(255);
				_errHandler.sync(this);
				switch ( getInterpreter().adaptivePredict(_input,17,_ctx) ) {
				case 1:
					{
					setState(251);
					propertyAccess();
					}
					break;
				case 2:
					{
					setState(252);
					arrayAccess();
					}
					break;
				case 3:
					{
					setState(253);
					mapAccess();
					}
					break;
				case 4:
					{
					setState(254);
					methodCall();
					}
					break;
				}
				}
				setState(259);
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
		public TerminalNode Identifier() { return getToken(Template2Parser.Identifier, 0); }
		public PropertyAccessContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_propertyAccess; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof Template2Listener ) ((Template2Listener)listener).enterPropertyAccess(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof Template2Listener ) ((Template2Listener)listener).exitPropertyAccess(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof Template2Visitor ) return ((Template2Visitor<? extends T>)visitor).visitPropertyAccess(this);
			else return visitor.visitChildren(this);
		}
	}

	public final PropertyAccessContext propertyAccess() throws RecognitionException {
		PropertyAccessContext _localctx = new PropertyAccessContext(_ctx, getState());
		enterRule(_localctx, 34, RULE_propertyAccess);
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(260);
			match(DOT);
			setState(261);
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
		public TerminalNode IntegerLiteral() { return getToken(Template2Parser.IntegerLiteral, 0); }
		public ArrayAccessContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_arrayAccess; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof Template2Listener ) ((Template2Listener)listener).enterArrayAccess(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof Template2Listener ) ((Template2Listener)listener).exitArrayAccess(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof Template2Visitor ) return ((Template2Visitor<? extends T>)visitor).visitArrayAccess(this);
			else return visitor.visitChildren(this);
		}
	}

	public final ArrayAccessContext arrayAccess() throws RecognitionException {
		ArrayAccessContext _localctx = new ArrayAccessContext(_ctx, getState());
		enterRule(_localctx, 36, RULE_arrayAccess);
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(263);
			match(LBRACK);
			setState(264);
			match(IntegerLiteral);
			setState(265);
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
		public TerminalNode StringLiteral() { return getToken(Template2Parser.StringLiteral, 0); }
		public MapAccessContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_mapAccess; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof Template2Listener ) ((Template2Listener)listener).enterMapAccess(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof Template2Listener ) ((Template2Listener)listener).exitMapAccess(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof Template2Visitor ) return ((Template2Visitor<? extends T>)visitor).visitMapAccess(this);
			else return visitor.visitChildren(this);
		}
	}

	public final MapAccessContext mapAccess() throws RecognitionException {
		MapAccessContext _localctx = new MapAccessContext(_ctx, getState());
		enterRule(_localctx, 38, RULE_mapAccess);
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(267);
			match(LBRACK);
			setState(268);
			match(StringLiteral);
			setState(269);
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
		public TerminalNode Identifier() { return getToken(Template2Parser.Identifier, 0); }
		public CallMethodParametersContext callMethodParameters() {
			return getRuleContext(CallMethodParametersContext.class,0);
		}
		public MethodCallContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_methodCall; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof Template2Listener ) ((Template2Listener)listener).enterMethodCall(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof Template2Listener ) ((Template2Listener)listener).exitMethodCall(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof Template2Visitor ) return ((Template2Visitor<? extends T>)visitor).visitMethodCall(this);
			else return visitor.visitChildren(this);
		}
	}

	public final MethodCallContext methodCall() throws RecognitionException {
		MethodCallContext _localctx = new MethodCallContext(_ctx, getState());
		enterRule(_localctx, 40, RULE_methodCall);
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(271);
			match(DOT);
			setState(272);
			match(Identifier);
			setState(273);
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
			if ( listener instanceof Template2Listener ) ((Template2Listener)listener).enterCallMethodParameters(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof Template2Listener ) ((Template2Listener)listener).exitCallMethodParameters(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof Template2Visitor ) return ((Template2Visitor<? extends T>)visitor).visitCallMethodParameters(this);
			else return visitor.visitChildren(this);
		}
	}

	public final CallMethodParametersContext callMethodParameters() throws RecognitionException {
		CallMethodParametersContext _localctx = new CallMethodParametersContext(_ctx, getState());
		enterRule(_localctx, 42, RULE_callMethodParameters);
		int _la;
		try {
			setState(288);
			_errHandler.sync(this);
			switch ( getInterpreter().adaptivePredict(_input,20,_ctx) ) {
			case 1:
				enterOuterAlt(_localctx, 1);
				{
				setState(275);
				match(LPAREN);
				setState(276);
				match(RPAREN);
				}
				break;
			case 2:
				enterOuterAlt(_localctx, 2);
				{
				setState(277);
				match(LPAREN);
				setState(278);
				expression(0);
				setState(283);
				_errHandler.sync(this);
				_la = _input.LA(1);
				while (_la==T__1) {
					{
					{
					setState(279);
					match(T__1);
					setState(280);
					expression(0);
					}
					}
					setState(285);
					_errHandler.sync(this);
					_la = _input.LA(1);
				}
				setState(286);
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
		case 14:
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
		"\3\u0430\ud6d1\u8206\uad2d\u4417\uaef1\u8d80\uaadd\3P\u0125\4\2\t\2\4"+
		"\3\t\3\4\4\t\4\4\5\t\5\4\6\t\6\4\7\t\7\4\b\t\b\4\t\t\t\4\n\t\n\4\13\t"+
		"\13\4\f\t\f\4\r\t\r\4\16\t\16\4\17\t\17\4\20\t\20\4\21\t\21\4\22\t\22"+
		"\4\23\t\23\4\24\t\24\4\25\t\25\4\26\t\26\4\27\t\27\3\2\5\2\60\n\2\3\2"+
		"\5\2\63\n\2\3\2\7\2\66\n\2\f\2\16\29\13\2\3\3\3\3\3\3\3\3\3\3\3\3\3\3"+
		"\3\3\7\3C\n\3\f\3\16\3F\13\3\3\4\3\4\3\4\3\4\3\5\3\5\3\5\3\5\3\5\5\5Q"+
		"\n\5\3\5\5\5T\n\5\3\5\3\5\3\6\3\6\3\6\3\6\3\6\3\6\3\6\3\7\3\7\3\7\7\7"+
		"b\n\7\f\7\16\7e\13\7\3\b\3\b\3\t\3\t\3\t\3\t\3\n\3\n\3\n\3\n\3\n\3\n\3"+
		"\13\3\13\3\13\3\13\3\13\3\13\7\13y\n\13\f\13\16\13|\13\13\3\13\5\13\177"+
		"\n\13\3\f\3\f\3\f\3\f\3\f\3\f\3\f\3\f\3\f\3\f\3\f\3\f\7\f\u008d\n\f\f"+
		"\f\16\f\u0090\13\f\3\f\3\f\5\f\u0094\n\f\3\f\3\f\3\r\3\r\3\r\3\r\3\r\3"+
		"\r\3\r\3\r\3\r\6\r\u00a1\n\r\r\r\16\r\u00a2\3\r\3\r\3\r\3\r\5\r\u00a9"+
		"\n\r\3\r\3\r\3\16\3\16\3\16\3\16\3\16\3\16\3\16\3\17\3\17\3\17\3\17\3"+
		"\17\3\17\3\17\3\17\3\17\3\20\3\20\3\20\3\20\3\20\3\20\3\20\3\20\3\20\3"+
		"\20\3\20\3\20\5\20\u00c9\n\20\3\20\3\20\3\20\3\20\3\20\3\20\3\20\3\20"+
		"\3\20\3\20\3\20\3\20\3\20\3\20\3\20\3\20\3\20\3\20\3\20\3\20\3\20\3\20"+
		"\3\20\3\20\3\20\3\20\3\20\3\20\3\20\3\20\3\20\3\20\3\20\3\20\3\20\3\20"+
		"\3\20\3\20\3\20\3\20\3\20\7\20\u00f4\n\20\f\20\16\20\u00f7\13\20\3\21"+
		"\3\21\3\21\3\21\3\22\3\22\3\22\3\22\3\22\7\22\u0102\n\22\f\22\16\22\u0105"+
		"\13\22\3\23\3\23\3\23\3\24\3\24\3\24\3\24\3\25\3\25\3\25\3\25\3\26\3\26"+
		"\3\26\3\26\3\27\3\27\3\27\3\27\3\27\3\27\7\27\u011c\n\27\f\27\16\27\u011f"+
		"\13\27\3\27\3\27\5\27\u0123\n\27\3\27\2\3\36\30\2\4\6\b\n\f\16\20\22\24"+
		"\26\30\32\34\36 \"$&(*,\2\f\3\2\30\33\4\2\36\36!!\4\2+,\658\4\29:>>\3"+
		"\2\678\3\2AC\4\2)*\60\61\4\2//\62\62\4\2((DN\3\2\65\66\u013c\2/\3\2\2"+
		"\2\4D\3\2\2\2\6G\3\2\2\2\bK\3\2\2\2\nW\3\2\2\2\f^\3\2\2\2\16f\3\2\2\2"+
		"\20h\3\2\2\2\22l\3\2\2\2\24~\3\2\2\2\26\u0080\3\2\2\2\30\u0097\3\2\2\2"+
		"\32\u00ac\3\2\2\2\34\u00b3\3\2\2\2\36\u00c8\3\2\2\2 \u00f8\3\2\2\2\"\u00fc"+
		"\3\2\2\2$\u0106\3\2\2\2&\u0109\3\2\2\2(\u010d\3\2\2\2*\u0111\3\2\2\2,"+
		"\u0122\3\2\2\2.\60\5\6\4\2/.\3\2\2\2/\60\3\2\2\2\60\62\3\2\2\2\61\63\5"+
		"\20\t\2\62\61\3\2\2\2\62\63\3\2\2\2\63\67\3\2\2\2\64\66\5\22\n\2\65\64"+
		"\3\2\2\2\669\3\2\2\2\67\65\3\2\2\2\678\3\2\2\28\3\3\2\2\29\67\3\2\2\2"+
		":C\5\16\b\2;C\5\n\6\2<C\5\b\5\2=C\5\26\f\2>C\5\30\r\2?C\5\32\16\2@C\5"+
		"\34\17\2AC\5 \21\2B:\3\2\2\2B;\3\2\2\2B<\3\2\2\2B=\3\2\2\2B>\3\2\2\2B"+
		"?\3\2\2\2B@\3\2\2\2BA\3\2\2\2CF\3\2\2\2DB\3\2\2\2DE\3\2\2\2E\5\3\2\2\2"+
		"FD\3\2\2\2GH\7\7\2\2HI\5\f\7\2IJ\7\3\2\2J\7\3\2\2\2KL\7\b\2\2LP\7#\2\2"+
		"MQ\7\26\2\2NQ\7\27\2\2OQ\5\f\7\2PM\3\2\2\2PN\3\2\2\2PO\3\2\2\2QS\3\2\2"+
		"\2RT\5*\26\2SR\3\2\2\2ST\3\2\2\2TU\3\2\2\2UV\7$\2\2V\t\3\2\2\2WX\7\t\2"+
		"\2XY\7#\2\2YZ\7O\2\2Z[\7(\2\2[\\\5\36\20\2\\]\7$\2\2]\13\3\2\2\2^c\7O"+
		"\2\2_`\7\'\2\2`b\7O\2\2a_\3\2\2\2be\3\2\2\2ca\3\2\2\2cd\3\2\2\2d\r\3\2"+
		"\2\2ec\3\2\2\2fg\t\2\2\2g\17\3\2\2\2hi\7\n\2\2ij\5\4\3\2jk\7\25\2\2k\21"+
		"\3\2\2\2lm\7\13\2\2mn\7O\2\2no\5\24\13\2op\5\4\3\2pq\7\25\2\2q\23\3\2"+
		"\2\2rs\7#\2\2s\177\7$\2\2tu\7#\2\2uz\7O\2\2vw\7\4\2\2wy\7O\2\2xv\3\2\2"+
		"\2y|\3\2\2\2zx\3\2\2\2z{\3\2\2\2{}\3\2\2\2|z\3\2\2\2}\177\7$\2\2~r\3\2"+
		"\2\2~t\3\2\2\2\177\25\3\2\2\2\u0080\u0081\7\f\2\2\u0081\u0082\7#\2\2\u0082"+
		"\u0083\5\36\20\2\u0083\u0084\7$\2\2\u0084\u008e\5\4\3\2\u0085\u0086\7"+
		"\r\2\2\u0086\u0087\7\16\2\2\u0087\u0088\7#\2\2\u0088\u0089\5\36\20\2\u0089"+
		"\u008a\7$\2\2\u008a\u008b\5\4\3\2\u008b\u008d\3\2\2\2\u008c\u0085\3\2"+
		"\2\2\u008d\u0090\3\2\2\2\u008e\u008c\3\2\2\2\u008e\u008f\3\2\2\2\u008f"+
		"\u0093\3\2\2\2\u0090\u008e\3\2\2\2\u0091\u0092\7\r\2\2\u0092\u0094\5\4"+
		"\3\2\u0093\u0091\3\2\2\2\u0093\u0094\3\2\2\2\u0094\u0095\3\2\2\2\u0095"+
		"\u0096\7\25\2\2\u0096\27\3\2\2\2\u0097\u0098\7\21\2\2\u0098\u0099\7#\2"+
		"\2\u0099\u009a\5 \21\2\u009a\u00a0\7$\2\2\u009b\u009c\7\22\2\2\u009c\u009d"+
		"\t\3\2\2\u009d\u009e\5\4\3\2\u009e\u009f\7\23\2\2\u009f\u00a1\3\2\2\2"+
		"\u00a0\u009b\3\2\2\2\u00a1\u00a2\3\2\2\2\u00a2\u00a0\3\2\2\2\u00a2\u00a3"+
		"\3\2\2\2\u00a3\u00a8\3\2\2\2\u00a4\u00a5\7\24\2\2\u00a5\u00a6\5\4\3\2"+
		"\u00a6\u00a7\7\23\2\2\u00a7\u00a9\3\2\2\2\u00a8\u00a4\3\2\2\2\u00a8\u00a9"+
		"\3\2\2\2\u00a9\u00aa\3\2\2\2\u00aa\u00ab\7\25\2\2\u00ab\31\3\2\2\2\u00ac"+
		"\u00ad\7\20\2\2\u00ad\u00ae\7#\2\2\u00ae\u00af\5\36\20\2\u00af\u00b0\7"+
		"$\2\2\u00b0\u00b1\5\4\3\2\u00b1\u00b2\7\25\2\2\u00b2\33\3\2\2\2\u00b3"+
		"\u00b4\7\17\2\2\u00b4\u00b5\7#\2\2\u00b5\u00b6\7O\2\2\u00b6\u00b7\7.\2"+
		"\2\u00b7\u00b8\5 \21\2\u00b8\u00b9\7$\2\2\u00b9\u00ba\5\4\3\2\u00ba\u00bb"+
		"\7\25\2\2\u00bb\35\3\2\2\2\u00bc\u00bd\b\20\1\2\u00bd\u00c9\5 \21\2\u00be"+
		"\u00c9\7\36\2\2\u00bf\u00c9\7\37\2\2\u00c0\u00c9\7 \2\2\u00c1\u00c9\7"+
		"!\2\2\u00c2\u00c3\7#\2\2\u00c3\u00c4\5\36\20\2\u00c4\u00c5\7$\2\2\u00c5"+
		"\u00c9\3\2\2\2\u00c6\u00c7\t\4\2\2\u00c7\u00c9\5\36\20\20\u00c8\u00bc"+
		"\3\2\2\2\u00c8\u00be\3\2\2\2\u00c8\u00bf\3\2\2\2\u00c8\u00c0\3\2\2\2\u00c8"+
		"\u00c1\3\2\2\2\u00c8\u00c2\3\2\2\2\u00c8\u00c6\3\2\2\2\u00c9\u00f5\3\2"+
		"\2\2\u00ca\u00cb\f\16\2\2\u00cb\u00cc\t\5\2\2\u00cc\u00f4\5\36\20\17\u00cd"+
		"\u00ce\f\r\2\2\u00ce\u00cf\t\6\2\2\u00cf\u00f4\5\36\20\16\u00d0\u00d1"+
		"\f\f\2\2\u00d1\u00d2\t\7\2\2\u00d2\u00f4\5\36\20\r\u00d3\u00d4\f\13\2"+
		"\2\u00d4\u00d5\t\b\2\2\u00d5\u00f4\5\36\20\f\u00d6\u00d7\f\n\2\2\u00d7"+
		"\u00d8\t\t\2\2\u00d8\u00f4\5\36\20\13\u00d9\u00da\f\t\2\2\u00da\u00db"+
		"\7;\2\2\u00db\u00f4\5\36\20\n\u00dc\u00dd\f\b\2\2\u00dd\u00de\7<\2\2\u00de"+
		"\u00f4\5\36\20\t\u00df\u00e0\f\7\2\2\u00e0\u00e1\7=\2\2\u00e1\u00f4\5"+
		"\36\20\b\u00e2\u00e3\f\6\2\2\u00e3\u00e4\7\63\2\2\u00e4\u00f4\5\36\20"+
		"\7\u00e5\u00e6\f\5\2\2\u00e6\u00e7\7\64\2\2\u00e7\u00f4\5\36\20\6\u00e8"+
		"\u00e9\f\4\2\2\u00e9\u00ea\7-\2\2\u00ea\u00eb\5\36\20\2\u00eb\u00ec\7"+
		".\2\2\u00ec\u00ed\5\36\20\5\u00ed\u00f4\3\2\2\2\u00ee\u00ef\f\3\2\2\u00ef"+
		"\u00f0\t\n\2\2\u00f0\u00f4\5\36\20\4\u00f1\u00f2\f\17\2\2\u00f2\u00f4"+
		"\t\13\2\2\u00f3\u00ca\3\2\2\2\u00f3\u00cd\3\2\2\2\u00f3\u00d0\3\2\2\2"+
		"\u00f3\u00d3\3\2\2\2\u00f3\u00d6\3\2\2\2\u00f3\u00d9\3\2\2\2\u00f3\u00dc"+
		"\3\2\2\2\u00f3\u00df\3\2\2\2\u00f3\u00e2\3\2\2\2\u00f3\u00e5\3\2\2\2\u00f3"+
		"\u00e8\3\2\2\2\u00f3\u00ee\3\2\2\2\u00f3\u00f1\3\2\2\2\u00f4\u00f7\3\2"+
		"\2\2\u00f5\u00f3\3\2\2\2\u00f5\u00f6\3\2\2\2\u00f6\37\3\2\2\2\u00f7\u00f5"+
		"\3\2\2\2\u00f8\u00f9\7\5\2\2\u00f9\u00fa\5\"\22\2\u00fa\u00fb\7\6\2\2"+
		"\u00fb!\3\2\2\2\u00fc\u0103\7O\2\2\u00fd\u0102\5$\23\2\u00fe\u0102\5&"+
		"\24\2\u00ff\u0102\5(\25\2\u0100\u0102\5*\26\2\u0101\u00fd\3\2\2\2\u0101"+
		"\u00fe\3\2\2\2\u0101\u00ff\3\2\2\2\u0101\u0100\3\2\2\2\u0102\u0105\3\2"+
		"\2\2\u0103\u0101\3\2\2\2\u0103\u0104\3\2\2\2\u0104#\3\2\2\2\u0105\u0103"+
		"\3\2\2\2\u0106\u0107\7\'\2\2\u0107\u0108\7O\2\2\u0108%\3\2\2\2\u0109\u010a"+
		"\7%\2\2\u010a\u010b\7\36\2\2\u010b\u010c\7&\2\2\u010c\'\3\2\2\2\u010d"+
		"\u010e\7%\2\2\u010e\u010f\7!\2\2\u010f\u0110\7&\2\2\u0110)\3\2\2\2\u0111"+
		"\u0112\7\'\2\2\u0112\u0113\7O\2\2\u0113\u0114\5,\27\2\u0114+\3\2\2\2\u0115"+
		"\u0116\7#\2\2\u0116\u0123\7$\2\2\u0117\u0118\7#\2\2\u0118\u011d\5\36\20"+
		"\2\u0119\u011a\7\4\2\2\u011a\u011c\5\36\20\2\u011b\u0119\3\2\2\2\u011c"+
		"\u011f\3\2\2\2\u011d\u011b\3\2\2\2\u011d\u011e\3\2\2\2\u011e\u0120\3\2"+
		"\2\2\u011f\u011d\3\2\2\2\u0120\u0121\7$\2\2\u0121\u0123\3\2\2\2\u0122"+
		"\u0115\3\2\2\2\u0122\u0117\3\2\2\2\u0123-\3\2\2\2\27/\62\67BDPScz~\u008e"+
		"\u0093\u00a2\u00a8\u00c8\u00f3\u00f5\u0101\u0103\u011d\u0122";
	public static final ATN _ATN =
		new ATNDeserializer().deserialize(_serializedATN.toCharArray());
	static {
		_decisionToDFA = new DFA[_ATN.getNumberOfDecisions()];
		for (int i = 0; i < _ATN.getNumberOfDecisions(); i++) {
			_decisionToDFA[i] = new DFA(_ATN.getDecisionState(i), i);
		}
	}
}