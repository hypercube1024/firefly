// Generated from /Users/qiupengtao/Develop/github_project/firefly/firefly-template2/src/main/java/com/firefly/template2/parser/Template2.g4 by ANTLR 4.6
package com.firefly.template2.parser;
import org.antlr.v4.runtime.tree.ParseTreeListener;

/**
 * This interface defines a complete listener for a parse tree produced by
 * {@link Template2Parser}.
 */
public interface Template2Listener extends ParseTreeListener {
	/**
	 * Enter a parse tree produced by {@link Template2Parser#program}.
	 * @param ctx the parse tree
	 */
	void enterProgram(Template2Parser.ProgramContext ctx);
	/**
	 * Exit a parse tree produced by {@link Template2Parser#program}.
	 * @param ctx the parse tree
	 */
	void exitProgram(Template2Parser.ProgramContext ctx);
	/**
	 * Enter a parse tree produced by {@link Template2Parser#templateBody}.
	 * @param ctx the parse tree
	 */
	void enterTemplateBody(Template2Parser.TemplateBodyContext ctx);
	/**
	 * Exit a parse tree produced by {@link Template2Parser#templateBody}.
	 * @param ctx the parse tree
	 */
	void exitTemplateBody(Template2Parser.TemplateBodyContext ctx);
	/**
	 * Enter a parse tree produced by {@link Template2Parser#extendTemplate}.
	 * @param ctx the parse tree
	 */
	void enterExtendTemplate(Template2Parser.ExtendTemplateContext ctx);
	/**
	 * Exit a parse tree produced by {@link Template2Parser#extendTemplate}.
	 * @param ctx the parse tree
	 */
	void exitExtendTemplate(Template2Parser.ExtendTemplateContext ctx);
	/**
	 * Enter a parse tree produced by {@link Template2Parser#include}.
	 * @param ctx the parse tree
	 */
	void enterInclude(Template2Parser.IncludeContext ctx);
	/**
	 * Exit a parse tree produced by {@link Template2Parser#include}.
	 * @param ctx the parse tree
	 */
	void exitInclude(Template2Parser.IncludeContext ctx);
	/**
	 * Enter a parse tree produced by {@link Template2Parser#set}.
	 * @param ctx the parse tree
	 */
	void enterSet(Template2Parser.SetContext ctx);
	/**
	 * Exit a parse tree produced by {@link Template2Parser#set}.
	 * @param ctx the parse tree
	 */
	void exitSet(Template2Parser.SetContext ctx);
	/**
	 * Enter a parse tree produced by {@link Template2Parser#templatePath}.
	 * @param ctx the parse tree
	 */
	void enterTemplatePath(Template2Parser.TemplatePathContext ctx);
	/**
	 * Exit a parse tree produced by {@link Template2Parser#templatePath}.
	 * @param ctx the parse tree
	 */
	void exitTemplatePath(Template2Parser.TemplatePathContext ctx);
	/**
	 * Enter a parse tree produced by {@link Template2Parser#output}.
	 * @param ctx the parse tree
	 */
	void enterOutput(Template2Parser.OutputContext ctx);
	/**
	 * Exit a parse tree produced by {@link Template2Parser#output}.
	 * @param ctx the parse tree
	 */
	void exitOutput(Template2Parser.OutputContext ctx);
	/**
	 * Enter a parse tree produced by {@link Template2Parser#mainFunction}.
	 * @param ctx the parse tree
	 */
	void enterMainFunction(Template2Parser.MainFunctionContext ctx);
	/**
	 * Exit a parse tree produced by {@link Template2Parser#mainFunction}.
	 * @param ctx the parse tree
	 */
	void exitMainFunction(Template2Parser.MainFunctionContext ctx);
	/**
	 * Enter a parse tree produced by {@link Template2Parser#functionDeclaration}.
	 * @param ctx the parse tree
	 */
	void enterFunctionDeclaration(Template2Parser.FunctionDeclarationContext ctx);
	/**
	 * Exit a parse tree produced by {@link Template2Parser#functionDeclaration}.
	 * @param ctx the parse tree
	 */
	void exitFunctionDeclaration(Template2Parser.FunctionDeclarationContext ctx);
	/**
	 * Enter a parse tree produced by {@link Template2Parser#functionParameters}.
	 * @param ctx the parse tree
	 */
	void enterFunctionParameters(Template2Parser.FunctionParametersContext ctx);
	/**
	 * Exit a parse tree produced by {@link Template2Parser#functionParameters}.
	 * @param ctx the parse tree
	 */
	void exitFunctionParameters(Template2Parser.FunctionParametersContext ctx);
	/**
	 * Enter a parse tree produced by {@link Template2Parser#selection}.
	 * @param ctx the parse tree
	 */
	void enterSelection(Template2Parser.SelectionContext ctx);
	/**
	 * Exit a parse tree produced by {@link Template2Parser#selection}.
	 * @param ctx the parse tree
	 */
	void exitSelection(Template2Parser.SelectionContext ctx);
	/**
	 * Enter a parse tree produced by {@link Template2Parser#switchCondition}.
	 * @param ctx the parse tree
	 */
	void enterSwitchCondition(Template2Parser.SwitchConditionContext ctx);
	/**
	 * Exit a parse tree produced by {@link Template2Parser#switchCondition}.
	 * @param ctx the parse tree
	 */
	void exitSwitchCondition(Template2Parser.SwitchConditionContext ctx);
	/**
	 * Enter a parse tree produced by {@link Template2Parser#whileLoop}.
	 * @param ctx the parse tree
	 */
	void enterWhileLoop(Template2Parser.WhileLoopContext ctx);
	/**
	 * Exit a parse tree produced by {@link Template2Parser#whileLoop}.
	 * @param ctx the parse tree
	 */
	void exitWhileLoop(Template2Parser.WhileLoopContext ctx);
	/**
	 * Enter a parse tree produced by {@link Template2Parser#forLoop}.
	 * @param ctx the parse tree
	 */
	void enterForLoop(Template2Parser.ForLoopContext ctx);
	/**
	 * Exit a parse tree produced by {@link Template2Parser#forLoop}.
	 * @param ctx the parse tree
	 */
	void exitForLoop(Template2Parser.ForLoopContext ctx);
	/**
	 * Enter a parse tree produced by the {@code mulDivMod}
	 * labeled alternative in {@link Template2Parser#expression}.
	 * @param ctx the parse tree
	 */
	void enterMulDivMod(Template2Parser.MulDivModContext ctx);
	/**
	 * Exit a parse tree produced by the {@code mulDivMod}
	 * labeled alternative in {@link Template2Parser#expression}.
	 * @param ctx the parse tree
	 */
	void exitMulDivMod(Template2Parser.MulDivModContext ctx);
	/**
	 * Enter a parse tree produced by the {@code suffixUnary}
	 * labeled alternative in {@link Template2Parser#expression}.
	 * @param ctx the parse tree
	 */
	void enterSuffixUnary(Template2Parser.SuffixUnaryContext ctx);
	/**
	 * Exit a parse tree produced by the {@code suffixUnary}
	 * labeled alternative in {@link Template2Parser#expression}.
	 * @param ctx the parse tree
	 */
	void exitSuffixUnary(Template2Parser.SuffixUnaryContext ctx);
	/**
	 * Enter a parse tree produced by the {@code parens}
	 * labeled alternative in {@link Template2Parser#expression}.
	 * @param ctx the parse tree
	 */
	void enterParens(Template2Parser.ParensContext ctx);
	/**
	 * Exit a parse tree produced by the {@code parens}
	 * labeled alternative in {@link Template2Parser#expression}.
	 * @param ctx the parse tree
	 */
	void exitParens(Template2Parser.ParensContext ctx);
	/**
	 * Enter a parse tree produced by the {@code bitOr}
	 * labeled alternative in {@link Template2Parser#expression}.
	 * @param ctx the parse tree
	 */
	void enterBitOr(Template2Parser.BitOrContext ctx);
	/**
	 * Exit a parse tree produced by the {@code bitOr}
	 * labeled alternative in {@link Template2Parser#expression}.
	 * @param ctx the parse tree
	 */
	void exitBitOr(Template2Parser.BitOrContext ctx);
	/**
	 * Enter a parse tree produced by the {@code assignment}
	 * labeled alternative in {@link Template2Parser#expression}.
	 * @param ctx the parse tree
	 */
	void enterAssignment(Template2Parser.AssignmentContext ctx);
	/**
	 * Exit a parse tree produced by the {@code assignment}
	 * labeled alternative in {@link Template2Parser#expression}.
	 * @param ctx the parse tree
	 */
	void exitAssignment(Template2Parser.AssignmentContext ctx);
	/**
	 * Enter a parse tree produced by the {@code shift}
	 * labeled alternative in {@link Template2Parser#expression}.
	 * @param ctx the parse tree
	 */
	void enterShift(Template2Parser.ShiftContext ctx);
	/**
	 * Exit a parse tree produced by the {@code shift}
	 * labeled alternative in {@link Template2Parser#expression}.
	 * @param ctx the parse tree
	 */
	void exitShift(Template2Parser.ShiftContext ctx);
	/**
	 * Enter a parse tree produced by the {@code addSub}
	 * labeled alternative in {@link Template2Parser#expression}.
	 * @param ctx the parse tree
	 */
	void enterAddSub(Template2Parser.AddSubContext ctx);
	/**
	 * Exit a parse tree produced by the {@code addSub}
	 * labeled alternative in {@link Template2Parser#expression}.
	 * @param ctx the parse tree
	 */
	void exitAddSub(Template2Parser.AddSubContext ctx);
	/**
	 * Enter a parse tree produced by the {@code floatingPointLiteral}
	 * labeled alternative in {@link Template2Parser#expression}.
	 * @param ctx the parse tree
	 */
	void enterFloatingPointLiteral(Template2Parser.FloatingPointLiteralContext ctx);
	/**
	 * Exit a parse tree produced by the {@code floatingPointLiteral}
	 * labeled alternative in {@link Template2Parser#expression}.
	 * @param ctx the parse tree
	 */
	void exitFloatingPointLiteral(Template2Parser.FloatingPointLiteralContext ctx);
	/**
	 * Enter a parse tree produced by the {@code shortCircuitOr}
	 * labeled alternative in {@link Template2Parser#expression}.
	 * @param ctx the parse tree
	 */
	void enterShortCircuitOr(Template2Parser.ShortCircuitOrContext ctx);
	/**
	 * Exit a parse tree produced by the {@code shortCircuitOr}
	 * labeled alternative in {@link Template2Parser#expression}.
	 * @param ctx the parse tree
	 */
	void exitShortCircuitOr(Template2Parser.ShortCircuitOrContext ctx);
	/**
	 * Enter a parse tree produced by the {@code prefixUnary}
	 * labeled alternative in {@link Template2Parser#expression}.
	 * @param ctx the parse tree
	 */
	void enterPrefixUnary(Template2Parser.PrefixUnaryContext ctx);
	/**
	 * Exit a parse tree produced by the {@code prefixUnary}
	 * labeled alternative in {@link Template2Parser#expression}.
	 * @param ctx the parse tree
	 */
	void exitPrefixUnary(Template2Parser.PrefixUnaryContext ctx);
	/**
	 * Enter a parse tree produced by the {@code equalOrNotEqual}
	 * labeled alternative in {@link Template2Parser#expression}.
	 * @param ctx the parse tree
	 */
	void enterEqualOrNotEqual(Template2Parser.EqualOrNotEqualContext ctx);
	/**
	 * Exit a parse tree produced by the {@code equalOrNotEqual}
	 * labeled alternative in {@link Template2Parser#expression}.
	 * @param ctx the parse tree
	 */
	void exitEqualOrNotEqual(Template2Parser.EqualOrNotEqualContext ctx);
	/**
	 * Enter a parse tree produced by the {@code bitAnd}
	 * labeled alternative in {@link Template2Parser#expression}.
	 * @param ctx the parse tree
	 */
	void enterBitAnd(Template2Parser.BitAndContext ctx);
	/**
	 * Exit a parse tree produced by the {@code bitAnd}
	 * labeled alternative in {@link Template2Parser#expression}.
	 * @param ctx the parse tree
	 */
	void exitBitAnd(Template2Parser.BitAndContext ctx);
	/**
	 * Enter a parse tree produced by the {@code stringLiteral}
	 * labeled alternative in {@link Template2Parser#expression}.
	 * @param ctx the parse tree
	 */
	void enterStringLiteral(Template2Parser.StringLiteralContext ctx);
	/**
	 * Exit a parse tree produced by the {@code stringLiteral}
	 * labeled alternative in {@link Template2Parser#expression}.
	 * @param ctx the parse tree
	 */
	void exitStringLiteral(Template2Parser.StringLiteralContext ctx);
	/**
	 * Enter a parse tree produced by the {@code integerLiteral}
	 * labeled alternative in {@link Template2Parser#expression}.
	 * @param ctx the parse tree
	 */
	void enterIntegerLiteral(Template2Parser.IntegerLiteralContext ctx);
	/**
	 * Exit a parse tree produced by the {@code integerLiteral}
	 * labeled alternative in {@link Template2Parser#expression}.
	 * @param ctx the parse tree
	 */
	void exitIntegerLiteral(Template2Parser.IntegerLiteralContext ctx);
	/**
	 * Enter a parse tree produced by the {@code xor}
	 * labeled alternative in {@link Template2Parser#expression}.
	 * @param ctx the parse tree
	 */
	void enterXor(Template2Parser.XorContext ctx);
	/**
	 * Exit a parse tree produced by the {@code xor}
	 * labeled alternative in {@link Template2Parser#expression}.
	 * @param ctx the parse tree
	 */
	void exitXor(Template2Parser.XorContext ctx);
	/**
	 * Enter a parse tree produced by the {@code booleanLiteral}
	 * labeled alternative in {@link Template2Parser#expression}.
	 * @param ctx the parse tree
	 */
	void enterBooleanLiteral(Template2Parser.BooleanLiteralContext ctx);
	/**
	 * Exit a parse tree produced by the {@code booleanLiteral}
	 * labeled alternative in {@link Template2Parser#expression}.
	 * @param ctx the parse tree
	 */
	void exitBooleanLiteral(Template2Parser.BooleanLiteralContext ctx);
	/**
	 * Enter a parse tree produced by the {@code ternary}
	 * labeled alternative in {@link Template2Parser#expression}.
	 * @param ctx the parse tree
	 */
	void enterTernary(Template2Parser.TernaryContext ctx);
	/**
	 * Exit a parse tree produced by the {@code ternary}
	 * labeled alternative in {@link Template2Parser#expression}.
	 * @param ctx the parse tree
	 */
	void exitTernary(Template2Parser.TernaryContext ctx);
	/**
	 * Enter a parse tree produced by the {@code bean}
	 * labeled alternative in {@link Template2Parser#expression}.
	 * @param ctx the parse tree
	 */
	void enterBean(Template2Parser.BeanContext ctx);
	/**
	 * Exit a parse tree produced by the {@code bean}
	 * labeled alternative in {@link Template2Parser#expression}.
	 * @param ctx the parse tree
	 */
	void exitBean(Template2Parser.BeanContext ctx);
	/**
	 * Enter a parse tree produced by the {@code shortCircuitAnd}
	 * labeled alternative in {@link Template2Parser#expression}.
	 * @param ctx the parse tree
	 */
	void enterShortCircuitAnd(Template2Parser.ShortCircuitAndContext ctx);
	/**
	 * Exit a parse tree produced by the {@code shortCircuitAnd}
	 * labeled alternative in {@link Template2Parser#expression}.
	 * @param ctx the parse tree
	 */
	void exitShortCircuitAnd(Template2Parser.ShortCircuitAndContext ctx);
	/**
	 * Enter a parse tree produced by the {@code greatOrLess}
	 * labeled alternative in {@link Template2Parser#expression}.
	 * @param ctx the parse tree
	 */
	void enterGreatOrLess(Template2Parser.GreatOrLessContext ctx);
	/**
	 * Exit a parse tree produced by the {@code greatOrLess}
	 * labeled alternative in {@link Template2Parser#expression}.
	 * @param ctx the parse tree
	 */
	void exitGreatOrLess(Template2Parser.GreatOrLessContext ctx);
	/**
	 * Enter a parse tree produced by {@link Template2Parser#beanAccess}.
	 * @param ctx the parse tree
	 */
	void enterBeanAccess(Template2Parser.BeanAccessContext ctx);
	/**
	 * Exit a parse tree produced by {@link Template2Parser#beanAccess}.
	 * @param ctx the parse tree
	 */
	void exitBeanAccess(Template2Parser.BeanAccessContext ctx);
	/**
	 * Enter a parse tree produced by {@link Template2Parser#objectAccess}.
	 * @param ctx the parse tree
	 */
	void enterObjectAccess(Template2Parser.ObjectAccessContext ctx);
	/**
	 * Exit a parse tree produced by {@link Template2Parser#objectAccess}.
	 * @param ctx the parse tree
	 */
	void exitObjectAccess(Template2Parser.ObjectAccessContext ctx);
	/**
	 * Enter a parse tree produced by {@link Template2Parser#propertyAccess}.
	 * @param ctx the parse tree
	 */
	void enterPropertyAccess(Template2Parser.PropertyAccessContext ctx);
	/**
	 * Exit a parse tree produced by {@link Template2Parser#propertyAccess}.
	 * @param ctx the parse tree
	 */
	void exitPropertyAccess(Template2Parser.PropertyAccessContext ctx);
	/**
	 * Enter a parse tree produced by {@link Template2Parser#arrayAccess}.
	 * @param ctx the parse tree
	 */
	void enterArrayAccess(Template2Parser.ArrayAccessContext ctx);
	/**
	 * Exit a parse tree produced by {@link Template2Parser#arrayAccess}.
	 * @param ctx the parse tree
	 */
	void exitArrayAccess(Template2Parser.ArrayAccessContext ctx);
	/**
	 * Enter a parse tree produced by {@link Template2Parser#mapAccess}.
	 * @param ctx the parse tree
	 */
	void enterMapAccess(Template2Parser.MapAccessContext ctx);
	/**
	 * Exit a parse tree produced by {@link Template2Parser#mapAccess}.
	 * @param ctx the parse tree
	 */
	void exitMapAccess(Template2Parser.MapAccessContext ctx);
	/**
	 * Enter a parse tree produced by {@link Template2Parser#methodCall}.
	 * @param ctx the parse tree
	 */
	void enterMethodCall(Template2Parser.MethodCallContext ctx);
	/**
	 * Exit a parse tree produced by {@link Template2Parser#methodCall}.
	 * @param ctx the parse tree
	 */
	void exitMethodCall(Template2Parser.MethodCallContext ctx);
	/**
	 * Enter a parse tree produced by {@link Template2Parser#callMethodParameters}.
	 * @param ctx the parse tree
	 */
	void enterCallMethodParameters(Template2Parser.CallMethodParametersContext ctx);
	/**
	 * Exit a parse tree produced by {@link Template2Parser#callMethodParameters}.
	 * @param ctx the parse tree
	 */
	void exitCallMethodParameters(Template2Parser.CallMethodParametersContext ctx);
}