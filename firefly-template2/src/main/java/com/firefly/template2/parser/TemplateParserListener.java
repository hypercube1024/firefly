// Generated from /Users/qiupengtao/Develop/github_project/firefly/firefly-template2/src/main/java/com/firefly/template2/parser/TemplateParser.g4 by ANTLR 4.5.3
package com.firefly.template2.parser;
import org.antlr.v4.runtime.tree.ParseTreeListener;

/**
 * This interface defines a complete listener for a parse tree produced by
 * {@link TemplateParserParser}.
 */
public interface TemplateParserListener extends ParseTreeListener {
	/**
	 * Enter a parse tree produced by {@link TemplateParserParser#program}.
	 * @param ctx the parse tree
	 */
	void enterProgram(TemplateParserParser.ProgramContext ctx);
	/**
	 * Exit a parse tree produced by {@link TemplateParserParser#program}.
	 * @param ctx the parse tree
	 */
	void exitProgram(TemplateParserParser.ProgramContext ctx);
	/**
	 * Enter a parse tree produced by {@link TemplateParserParser#templateBody}.
	 * @param ctx the parse tree
	 */
	void enterTemplateBody(TemplateParserParser.TemplateBodyContext ctx);
	/**
	 * Exit a parse tree produced by {@link TemplateParserParser#templateBody}.
	 * @param ctx the parse tree
	 */
	void exitTemplateBody(TemplateParserParser.TemplateBodyContext ctx);
	/**
	 * Enter a parse tree produced by {@link TemplateParserParser#extends}.
	 * @param ctx the parse tree
	 */
	void enterExtends(TemplateParserParser.ExtendsContext ctx);
	/**
	 * Exit a parse tree produced by {@link TemplateParserParser#extends}.
	 * @param ctx the parse tree
	 */
	void exitExtends(TemplateParserParser.ExtendsContext ctx);
	/**
	 * Enter a parse tree produced by {@link TemplateParserParser#include}.
	 * @param ctx the parse tree
	 */
	void enterInclude(TemplateParserParser.IncludeContext ctx);
	/**
	 * Exit a parse tree produced by {@link TemplateParserParser#include}.
	 * @param ctx the parse tree
	 */
	void exitInclude(TemplateParserParser.IncludeContext ctx);
	/**
	 * Enter a parse tree produced by {@link TemplateParserParser#set}.
	 * @param ctx the parse tree
	 */
	void enterSet(TemplateParserParser.SetContext ctx);
	/**
	 * Exit a parse tree produced by {@link TemplateParserParser#set}.
	 * @param ctx the parse tree
	 */
	void exitSet(TemplateParserParser.SetContext ctx);
	/**
	 * Enter a parse tree produced by {@link TemplateParserParser#templatePath}.
	 * @param ctx the parse tree
	 */
	void enterTemplatePath(TemplateParserParser.TemplatePathContext ctx);
	/**
	 * Exit a parse tree produced by {@link TemplateParserParser#templatePath}.
	 * @param ctx the parse tree
	 */
	void exitTemplatePath(TemplateParserParser.TemplatePathContext ctx);
	/**
	 * Enter a parse tree produced by {@link TemplateParserParser#mainFunction}.
	 * @param ctx the parse tree
	 */
	void enterMainFunction(TemplateParserParser.MainFunctionContext ctx);
	/**
	 * Exit a parse tree produced by {@link TemplateParserParser#mainFunction}.
	 * @param ctx the parse tree
	 */
	void exitMainFunction(TemplateParserParser.MainFunctionContext ctx);
	/**
	 * Enter a parse tree produced by {@link TemplateParserParser#functionDeclaration}.
	 * @param ctx the parse tree
	 */
	void enterFunctionDeclaration(TemplateParserParser.FunctionDeclarationContext ctx);
	/**
	 * Exit a parse tree produced by {@link TemplateParserParser#functionDeclaration}.
	 * @param ctx the parse tree
	 */
	void exitFunctionDeclaration(TemplateParserParser.FunctionDeclarationContext ctx);
	/**
	 * Enter a parse tree produced by {@link TemplateParserParser#functionParameters}.
	 * @param ctx the parse tree
	 */
	void enterFunctionParameters(TemplateParserParser.FunctionParametersContext ctx);
	/**
	 * Exit a parse tree produced by {@link TemplateParserParser#functionParameters}.
	 * @param ctx the parse tree
	 */
	void exitFunctionParameters(TemplateParserParser.FunctionParametersContext ctx);
	/**
	 * Enter a parse tree produced by {@link TemplateParserParser#selection}.
	 * @param ctx the parse tree
	 */
	void enterSelection(TemplateParserParser.SelectionContext ctx);
	/**
	 * Exit a parse tree produced by {@link TemplateParserParser#selection}.
	 * @param ctx the parse tree
	 */
	void exitSelection(TemplateParserParser.SelectionContext ctx);
	/**
	 * Enter a parse tree produced by {@link TemplateParserParser#switch}.
	 * @param ctx the parse tree
	 */
	void enterSwitch(TemplateParserParser.SwitchContext ctx);
	/**
	 * Exit a parse tree produced by {@link TemplateParserParser#switch}.
	 * @param ctx the parse tree
	 */
	void exitSwitch(TemplateParserParser.SwitchContext ctx);
	/**
	 * Enter a parse tree produced by {@link TemplateParserParser#whileLoop}.
	 * @param ctx the parse tree
	 */
	void enterWhileLoop(TemplateParserParser.WhileLoopContext ctx);
	/**
	 * Exit a parse tree produced by {@link TemplateParserParser#whileLoop}.
	 * @param ctx the parse tree
	 */
	void exitWhileLoop(TemplateParserParser.WhileLoopContext ctx);
	/**
	 * Enter a parse tree produced by {@link TemplateParserParser#forLoop}.
	 * @param ctx the parse tree
	 */
	void enterForLoop(TemplateParserParser.ForLoopContext ctx);
	/**
	 * Exit a parse tree produced by {@link TemplateParserParser#forLoop}.
	 * @param ctx the parse tree
	 */
	void exitForLoop(TemplateParserParser.ForLoopContext ctx);
	/**
	 * Enter a parse tree produced by the {@code mulDivMod}
	 * labeled alternative in {@link TemplateParserParser#expression}.
	 * @param ctx the parse tree
	 */
	void enterMulDivMod(TemplateParserParser.MulDivModContext ctx);
	/**
	 * Exit a parse tree produced by the {@code mulDivMod}
	 * labeled alternative in {@link TemplateParserParser#expression}.
	 * @param ctx the parse tree
	 */
	void exitMulDivMod(TemplateParserParser.MulDivModContext ctx);
	/**
	 * Enter a parse tree produced by the {@code suffixUnary}
	 * labeled alternative in {@link TemplateParserParser#expression}.
	 * @param ctx the parse tree
	 */
	void enterSuffixUnary(TemplateParserParser.SuffixUnaryContext ctx);
	/**
	 * Exit a parse tree produced by the {@code suffixUnary}
	 * labeled alternative in {@link TemplateParserParser#expression}.
	 * @param ctx the parse tree
	 */
	void exitSuffixUnary(TemplateParserParser.SuffixUnaryContext ctx);
	/**
	 * Enter a parse tree produced by the {@code parens}
	 * labeled alternative in {@link TemplateParserParser#expression}.
	 * @param ctx the parse tree
	 */
	void enterParens(TemplateParserParser.ParensContext ctx);
	/**
	 * Exit a parse tree produced by the {@code parens}
	 * labeled alternative in {@link TemplateParserParser#expression}.
	 * @param ctx the parse tree
	 */
	void exitParens(TemplateParserParser.ParensContext ctx);
	/**
	 * Enter a parse tree produced by the {@code bitOr}
	 * labeled alternative in {@link TemplateParserParser#expression}.
	 * @param ctx the parse tree
	 */
	void enterBitOr(TemplateParserParser.BitOrContext ctx);
	/**
	 * Exit a parse tree produced by the {@code bitOr}
	 * labeled alternative in {@link TemplateParserParser#expression}.
	 * @param ctx the parse tree
	 */
	void exitBitOr(TemplateParserParser.BitOrContext ctx);
	/**
	 * Enter a parse tree produced by the {@code assignment}
	 * labeled alternative in {@link TemplateParserParser#expression}.
	 * @param ctx the parse tree
	 */
	void enterAssignment(TemplateParserParser.AssignmentContext ctx);
	/**
	 * Exit a parse tree produced by the {@code assignment}
	 * labeled alternative in {@link TemplateParserParser#expression}.
	 * @param ctx the parse tree
	 */
	void exitAssignment(TemplateParserParser.AssignmentContext ctx);
	/**
	 * Enter a parse tree produced by the {@code shift}
	 * labeled alternative in {@link TemplateParserParser#expression}.
	 * @param ctx the parse tree
	 */
	void enterShift(TemplateParserParser.ShiftContext ctx);
	/**
	 * Exit a parse tree produced by the {@code shift}
	 * labeled alternative in {@link TemplateParserParser#expression}.
	 * @param ctx the parse tree
	 */
	void exitShift(TemplateParserParser.ShiftContext ctx);
	/**
	 * Enter a parse tree produced by the {@code addSub}
	 * labeled alternative in {@link TemplateParserParser#expression}.
	 * @param ctx the parse tree
	 */
	void enterAddSub(TemplateParserParser.AddSubContext ctx);
	/**
	 * Exit a parse tree produced by the {@code addSub}
	 * labeled alternative in {@link TemplateParserParser#expression}.
	 * @param ctx the parse tree
	 */
	void exitAddSub(TemplateParserParser.AddSubContext ctx);
	/**
	 * Enter a parse tree produced by the {@code floatingPointLiteral}
	 * labeled alternative in {@link TemplateParserParser#expression}.
	 * @param ctx the parse tree
	 */
	void enterFloatingPointLiteral(TemplateParserParser.FloatingPointLiteralContext ctx);
	/**
	 * Exit a parse tree produced by the {@code floatingPointLiteral}
	 * labeled alternative in {@link TemplateParserParser#expression}.
	 * @param ctx the parse tree
	 */
	void exitFloatingPointLiteral(TemplateParserParser.FloatingPointLiteralContext ctx);
	/**
	 * Enter a parse tree produced by the {@code shortCircuitOr}
	 * labeled alternative in {@link TemplateParserParser#expression}.
	 * @param ctx the parse tree
	 */
	void enterShortCircuitOr(TemplateParserParser.ShortCircuitOrContext ctx);
	/**
	 * Exit a parse tree produced by the {@code shortCircuitOr}
	 * labeled alternative in {@link TemplateParserParser#expression}.
	 * @param ctx the parse tree
	 */
	void exitShortCircuitOr(TemplateParserParser.ShortCircuitOrContext ctx);
	/**
	 * Enter a parse tree produced by the {@code prefixUnary}
	 * labeled alternative in {@link TemplateParserParser#expression}.
	 * @param ctx the parse tree
	 */
	void enterPrefixUnary(TemplateParserParser.PrefixUnaryContext ctx);
	/**
	 * Exit a parse tree produced by the {@code prefixUnary}
	 * labeled alternative in {@link TemplateParserParser#expression}.
	 * @param ctx the parse tree
	 */
	void exitPrefixUnary(TemplateParserParser.PrefixUnaryContext ctx);
	/**
	 * Enter a parse tree produced by the {@code equalOrNotEqual}
	 * labeled alternative in {@link TemplateParserParser#expression}.
	 * @param ctx the parse tree
	 */
	void enterEqualOrNotEqual(TemplateParserParser.EqualOrNotEqualContext ctx);
	/**
	 * Exit a parse tree produced by the {@code equalOrNotEqual}
	 * labeled alternative in {@link TemplateParserParser#expression}.
	 * @param ctx the parse tree
	 */
	void exitEqualOrNotEqual(TemplateParserParser.EqualOrNotEqualContext ctx);
	/**
	 * Enter a parse tree produced by the {@code bitAnd}
	 * labeled alternative in {@link TemplateParserParser#expression}.
	 * @param ctx the parse tree
	 */
	void enterBitAnd(TemplateParserParser.BitAndContext ctx);
	/**
	 * Exit a parse tree produced by the {@code bitAnd}
	 * labeled alternative in {@link TemplateParserParser#expression}.
	 * @param ctx the parse tree
	 */
	void exitBitAnd(TemplateParserParser.BitAndContext ctx);
	/**
	 * Enter a parse tree produced by the {@code stringLiteral}
	 * labeled alternative in {@link TemplateParserParser#expression}.
	 * @param ctx the parse tree
	 */
	void enterStringLiteral(TemplateParserParser.StringLiteralContext ctx);
	/**
	 * Exit a parse tree produced by the {@code stringLiteral}
	 * labeled alternative in {@link TemplateParserParser#expression}.
	 * @param ctx the parse tree
	 */
	void exitStringLiteral(TemplateParserParser.StringLiteralContext ctx);
	/**
	 * Enter a parse tree produced by the {@code integerLiteral}
	 * labeled alternative in {@link TemplateParserParser#expression}.
	 * @param ctx the parse tree
	 */
	void enterIntegerLiteral(TemplateParserParser.IntegerLiteralContext ctx);
	/**
	 * Exit a parse tree produced by the {@code integerLiteral}
	 * labeled alternative in {@link TemplateParserParser#expression}.
	 * @param ctx the parse tree
	 */
	void exitIntegerLiteral(TemplateParserParser.IntegerLiteralContext ctx);
	/**
	 * Enter a parse tree produced by the {@code xor}
	 * labeled alternative in {@link TemplateParserParser#expression}.
	 * @param ctx the parse tree
	 */
	void enterXor(TemplateParserParser.XorContext ctx);
	/**
	 * Exit a parse tree produced by the {@code xor}
	 * labeled alternative in {@link TemplateParserParser#expression}.
	 * @param ctx the parse tree
	 */
	void exitXor(TemplateParserParser.XorContext ctx);
	/**
	 * Enter a parse tree produced by the {@code booleanLiteral}
	 * labeled alternative in {@link TemplateParserParser#expression}.
	 * @param ctx the parse tree
	 */
	void enterBooleanLiteral(TemplateParserParser.BooleanLiteralContext ctx);
	/**
	 * Exit a parse tree produced by the {@code booleanLiteral}
	 * labeled alternative in {@link TemplateParserParser#expression}.
	 * @param ctx the parse tree
	 */
	void exitBooleanLiteral(TemplateParserParser.BooleanLiteralContext ctx);
	/**
	 * Enter a parse tree produced by the {@code ternary}
	 * labeled alternative in {@link TemplateParserParser#expression}.
	 * @param ctx the parse tree
	 */
	void enterTernary(TemplateParserParser.TernaryContext ctx);
	/**
	 * Exit a parse tree produced by the {@code ternary}
	 * labeled alternative in {@link TemplateParserParser#expression}.
	 * @param ctx the parse tree
	 */
	void exitTernary(TemplateParserParser.TernaryContext ctx);
	/**
	 * Enter a parse tree produced by the {@code bean}
	 * labeled alternative in {@link TemplateParserParser#expression}.
	 * @param ctx the parse tree
	 */
	void enterBean(TemplateParserParser.BeanContext ctx);
	/**
	 * Exit a parse tree produced by the {@code bean}
	 * labeled alternative in {@link TemplateParserParser#expression}.
	 * @param ctx the parse tree
	 */
	void exitBean(TemplateParserParser.BeanContext ctx);
	/**
	 * Enter a parse tree produced by the {@code shortCircuitAnd}
	 * labeled alternative in {@link TemplateParserParser#expression}.
	 * @param ctx the parse tree
	 */
	void enterShortCircuitAnd(TemplateParserParser.ShortCircuitAndContext ctx);
	/**
	 * Exit a parse tree produced by the {@code shortCircuitAnd}
	 * labeled alternative in {@link TemplateParserParser#expression}.
	 * @param ctx the parse tree
	 */
	void exitShortCircuitAnd(TemplateParserParser.ShortCircuitAndContext ctx);
	/**
	 * Enter a parse tree produced by the {@code greatOrLess}
	 * labeled alternative in {@link TemplateParserParser#expression}.
	 * @param ctx the parse tree
	 */
	void enterGreatOrLess(TemplateParserParser.GreatOrLessContext ctx);
	/**
	 * Exit a parse tree produced by the {@code greatOrLess}
	 * labeled alternative in {@link TemplateParserParser#expression}.
	 * @param ctx the parse tree
	 */
	void exitGreatOrLess(TemplateParserParser.GreatOrLessContext ctx);
	/**
	 * Enter a parse tree produced by {@link TemplateParserParser#beanAccess}.
	 * @param ctx the parse tree
	 */
	void enterBeanAccess(TemplateParserParser.BeanAccessContext ctx);
	/**
	 * Exit a parse tree produced by {@link TemplateParserParser#beanAccess}.
	 * @param ctx the parse tree
	 */
	void exitBeanAccess(TemplateParserParser.BeanAccessContext ctx);
	/**
	 * Enter a parse tree produced by {@link TemplateParserParser#objectAccess}.
	 * @param ctx the parse tree
	 */
	void enterObjectAccess(TemplateParserParser.ObjectAccessContext ctx);
	/**
	 * Exit a parse tree produced by {@link TemplateParserParser#objectAccess}.
	 * @param ctx the parse tree
	 */
	void exitObjectAccess(TemplateParserParser.ObjectAccessContext ctx);
	/**
	 * Enter a parse tree produced by {@link TemplateParserParser#propertyAccess}.
	 * @param ctx the parse tree
	 */
	void enterPropertyAccess(TemplateParserParser.PropertyAccessContext ctx);
	/**
	 * Exit a parse tree produced by {@link TemplateParserParser#propertyAccess}.
	 * @param ctx the parse tree
	 */
	void exitPropertyAccess(TemplateParserParser.PropertyAccessContext ctx);
	/**
	 * Enter a parse tree produced by {@link TemplateParserParser#arrayAccess}.
	 * @param ctx the parse tree
	 */
	void enterArrayAccess(TemplateParserParser.ArrayAccessContext ctx);
	/**
	 * Exit a parse tree produced by {@link TemplateParserParser#arrayAccess}.
	 * @param ctx the parse tree
	 */
	void exitArrayAccess(TemplateParserParser.ArrayAccessContext ctx);
	/**
	 * Enter a parse tree produced by {@link TemplateParserParser#mapAccess}.
	 * @param ctx the parse tree
	 */
	void enterMapAccess(TemplateParserParser.MapAccessContext ctx);
	/**
	 * Exit a parse tree produced by {@link TemplateParserParser#mapAccess}.
	 * @param ctx the parse tree
	 */
	void exitMapAccess(TemplateParserParser.MapAccessContext ctx);
	/**
	 * Enter a parse tree produced by {@link TemplateParserParser#methodCall}.
	 * @param ctx the parse tree
	 */
	void enterMethodCall(TemplateParserParser.MethodCallContext ctx);
	/**
	 * Exit a parse tree produced by {@link TemplateParserParser#methodCall}.
	 * @param ctx the parse tree
	 */
	void exitMethodCall(TemplateParserParser.MethodCallContext ctx);
	/**
	 * Enter a parse tree produced by {@link TemplateParserParser#callMethodParameters}.
	 * @param ctx the parse tree
	 */
	void enterCallMethodParameters(TemplateParserParser.CallMethodParametersContext ctx);
	/**
	 * Exit a parse tree produced by {@link TemplateParserParser#callMethodParameters}.
	 * @param ctx the parse tree
	 */
	void exitCallMethodParameters(TemplateParserParser.CallMethodParametersContext ctx);
}