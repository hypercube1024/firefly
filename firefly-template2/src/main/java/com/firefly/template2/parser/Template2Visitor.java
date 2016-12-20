// Generated from /Users/qiupengtao/Develop/github_project/firefly/firefly-template2/src/main/java/com/firefly/template2/parser/Template2.g4 by ANTLR 4.6
package com.firefly.template2.parser;
import org.antlr.v4.runtime.tree.ParseTreeVisitor;

/**
 * This interface defines a complete generic visitor for a parse tree produced
 * by {@link Template2Parser}.
 *
 * @param <T> The return type of the visit operation. Use {@link Void} for
 * operations with no return type.
 */
public interface Template2Visitor<T> extends ParseTreeVisitor<T> {
	/**
	 * Visit a parse tree produced by {@link Template2Parser#program}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitProgram(Template2Parser.ProgramContext ctx);
	/**
	 * Visit a parse tree produced by {@link Template2Parser#templateBody}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitTemplateBody(Template2Parser.TemplateBodyContext ctx);
	/**
	 * Visit a parse tree produced by {@link Template2Parser#extendTemplate}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitExtendTemplate(Template2Parser.ExtendTemplateContext ctx);
	/**
	 * Visit a parse tree produced by {@link Template2Parser#include}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitInclude(Template2Parser.IncludeContext ctx);
	/**
	 * Visit a parse tree produced by {@link Template2Parser#set}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitSet(Template2Parser.SetContext ctx);
	/**
	 * Visit a parse tree produced by {@link Template2Parser#templatePath}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitTemplatePath(Template2Parser.TemplatePathContext ctx);
	/**
	 * Visit a parse tree produced by {@link Template2Parser#output}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitOutput(Template2Parser.OutputContext ctx);
	/**
	 * Visit a parse tree produced by {@link Template2Parser#mainFunction}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitMainFunction(Template2Parser.MainFunctionContext ctx);
	/**
	 * Visit a parse tree produced by {@link Template2Parser#functionDeclaration}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitFunctionDeclaration(Template2Parser.FunctionDeclarationContext ctx);
	/**
	 * Visit a parse tree produced by {@link Template2Parser#functionParameters}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitFunctionParameters(Template2Parser.FunctionParametersContext ctx);
	/**
	 * Visit a parse tree produced by {@link Template2Parser#selection}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitSelection(Template2Parser.SelectionContext ctx);
	/**
	 * Visit a parse tree produced by {@link Template2Parser#switchCondition}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitSwitchCondition(Template2Parser.SwitchConditionContext ctx);
	/**
	 * Visit a parse tree produced by {@link Template2Parser#whileLoop}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitWhileLoop(Template2Parser.WhileLoopContext ctx);
	/**
	 * Visit a parse tree produced by {@link Template2Parser#forLoop}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitForLoop(Template2Parser.ForLoopContext ctx);
	/**
	 * Visit a parse tree produced by the {@code mulDivMod}
	 * labeled alternative in {@link Template2Parser#expression}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitMulDivMod(Template2Parser.MulDivModContext ctx);
	/**
	 * Visit a parse tree produced by the {@code suffixUnary}
	 * labeled alternative in {@link Template2Parser#expression}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitSuffixUnary(Template2Parser.SuffixUnaryContext ctx);
	/**
	 * Visit a parse tree produced by the {@code parens}
	 * labeled alternative in {@link Template2Parser#expression}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitParens(Template2Parser.ParensContext ctx);
	/**
	 * Visit a parse tree produced by the {@code bitOr}
	 * labeled alternative in {@link Template2Parser#expression}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitBitOr(Template2Parser.BitOrContext ctx);
	/**
	 * Visit a parse tree produced by the {@code assignment}
	 * labeled alternative in {@link Template2Parser#expression}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitAssignment(Template2Parser.AssignmentContext ctx);
	/**
	 * Visit a parse tree produced by the {@code shift}
	 * labeled alternative in {@link Template2Parser#expression}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitShift(Template2Parser.ShiftContext ctx);
	/**
	 * Visit a parse tree produced by the {@code addSub}
	 * labeled alternative in {@link Template2Parser#expression}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitAddSub(Template2Parser.AddSubContext ctx);
	/**
	 * Visit a parse tree produced by the {@code floatingPointLiteral}
	 * labeled alternative in {@link Template2Parser#expression}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitFloatingPointLiteral(Template2Parser.FloatingPointLiteralContext ctx);
	/**
	 * Visit a parse tree produced by the {@code shortCircuitOr}
	 * labeled alternative in {@link Template2Parser#expression}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitShortCircuitOr(Template2Parser.ShortCircuitOrContext ctx);
	/**
	 * Visit a parse tree produced by the {@code prefixUnary}
	 * labeled alternative in {@link Template2Parser#expression}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitPrefixUnary(Template2Parser.PrefixUnaryContext ctx);
	/**
	 * Visit a parse tree produced by the {@code equalOrNotEqual}
	 * labeled alternative in {@link Template2Parser#expression}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitEqualOrNotEqual(Template2Parser.EqualOrNotEqualContext ctx);
	/**
	 * Visit a parse tree produced by the {@code bitAnd}
	 * labeled alternative in {@link Template2Parser#expression}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitBitAnd(Template2Parser.BitAndContext ctx);
	/**
	 * Visit a parse tree produced by the {@code stringLiteral}
	 * labeled alternative in {@link Template2Parser#expression}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitStringLiteral(Template2Parser.StringLiteralContext ctx);
	/**
	 * Visit a parse tree produced by the {@code integerLiteral}
	 * labeled alternative in {@link Template2Parser#expression}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitIntegerLiteral(Template2Parser.IntegerLiteralContext ctx);
	/**
	 * Visit a parse tree produced by the {@code xor}
	 * labeled alternative in {@link Template2Parser#expression}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitXor(Template2Parser.XorContext ctx);
	/**
	 * Visit a parse tree produced by the {@code booleanLiteral}
	 * labeled alternative in {@link Template2Parser#expression}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitBooleanLiteral(Template2Parser.BooleanLiteralContext ctx);
	/**
	 * Visit a parse tree produced by the {@code ternary}
	 * labeled alternative in {@link Template2Parser#expression}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitTernary(Template2Parser.TernaryContext ctx);
	/**
	 * Visit a parse tree produced by the {@code bean}
	 * labeled alternative in {@link Template2Parser#expression}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitBean(Template2Parser.BeanContext ctx);
	/**
	 * Visit a parse tree produced by the {@code shortCircuitAnd}
	 * labeled alternative in {@link Template2Parser#expression}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitShortCircuitAnd(Template2Parser.ShortCircuitAndContext ctx);
	/**
	 * Visit a parse tree produced by the {@code greatOrLess}
	 * labeled alternative in {@link Template2Parser#expression}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitGreatOrLess(Template2Parser.GreatOrLessContext ctx);
	/**
	 * Visit a parse tree produced by {@link Template2Parser#beanAccess}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitBeanAccess(Template2Parser.BeanAccessContext ctx);
	/**
	 * Visit a parse tree produced by {@link Template2Parser#objectAccess}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitObjectAccess(Template2Parser.ObjectAccessContext ctx);
	/**
	 * Visit a parse tree produced by {@link Template2Parser#propertyAccess}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitPropertyAccess(Template2Parser.PropertyAccessContext ctx);
	/**
	 * Visit a parse tree produced by {@link Template2Parser#arrayAccess}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitArrayAccess(Template2Parser.ArrayAccessContext ctx);
	/**
	 * Visit a parse tree produced by {@link Template2Parser#mapAccess}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitMapAccess(Template2Parser.MapAccessContext ctx);
	/**
	 * Visit a parse tree produced by {@link Template2Parser#methodCall}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitMethodCall(Template2Parser.MethodCallContext ctx);
	/**
	 * Visit a parse tree produced by {@link Template2Parser#callMethodParameters}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitCallMethodParameters(Template2Parser.CallMethodParametersContext ctx);
}