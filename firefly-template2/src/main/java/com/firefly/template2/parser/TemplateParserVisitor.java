// Generated from /Users/qiupengtao/Develop/github_project/firefly/firefly-template2/src/main/java/com/firefly/template2/parser/TemplateParser.g4 by ANTLR 4.5.3
package com.firefly.template2.parser;
import org.antlr.v4.runtime.tree.ParseTreeVisitor;

/**
 * This interface defines a complete generic visitor for a parse tree produced
 * by {@link TemplateParserParser}.
 *
 * @param <T> The return type of the visit operation. Use {@link Void} for
 * operations with no return type.
 */
public interface TemplateParserVisitor<T> extends ParseTreeVisitor<T> {
	/**
	 * Visit a parse tree produced by {@link TemplateParserParser#program}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitProgram(TemplateParserParser.ProgramContext ctx);
	/**
	 * Visit a parse tree produced by {@link TemplateParserParser#templateBody}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitTemplateBody(TemplateParserParser.TemplateBodyContext ctx);
	/**
	 * Visit a parse tree produced by {@link TemplateParserParser#extends}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitExtends(TemplateParserParser.ExtendsContext ctx);
	/**
	 * Visit a parse tree produced by {@link TemplateParserParser#include}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitInclude(TemplateParserParser.IncludeContext ctx);
	/**
	 * Visit a parse tree produced by {@link TemplateParserParser#set}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitSet(TemplateParserParser.SetContext ctx);
	/**
	 * Visit a parse tree produced by {@link TemplateParserParser#templatePath}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitTemplatePath(TemplateParserParser.TemplatePathContext ctx);
	/**
	 * Visit a parse tree produced by {@link TemplateParserParser#mainFunction}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitMainFunction(TemplateParserParser.MainFunctionContext ctx);
	/**
	 * Visit a parse tree produced by {@link TemplateParserParser#functionDeclaration}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitFunctionDeclaration(TemplateParserParser.FunctionDeclarationContext ctx);
	/**
	 * Visit a parse tree produced by {@link TemplateParserParser#functionParameters}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitFunctionParameters(TemplateParserParser.FunctionParametersContext ctx);
	/**
	 * Visit a parse tree produced by {@link TemplateParserParser#selection}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitSelection(TemplateParserParser.SelectionContext ctx);
	/**
	 * Visit a parse tree produced by {@link TemplateParserParser#switch}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitSwitch(TemplateParserParser.SwitchContext ctx);
	/**
	 * Visit a parse tree produced by {@link TemplateParserParser#whileLoop}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitWhileLoop(TemplateParserParser.WhileLoopContext ctx);
	/**
	 * Visit a parse tree produced by {@link TemplateParserParser#forLoop}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitForLoop(TemplateParserParser.ForLoopContext ctx);
	/**
	 * Visit a parse tree produced by the {@code mulDivMod}
	 * labeled alternative in {@link TemplateParserParser#expression}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitMulDivMod(TemplateParserParser.MulDivModContext ctx);
	/**
	 * Visit a parse tree produced by the {@code suffixUnary}
	 * labeled alternative in {@link TemplateParserParser#expression}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitSuffixUnary(TemplateParserParser.SuffixUnaryContext ctx);
	/**
	 * Visit a parse tree produced by the {@code parens}
	 * labeled alternative in {@link TemplateParserParser#expression}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitParens(TemplateParserParser.ParensContext ctx);
	/**
	 * Visit a parse tree produced by the {@code bitOr}
	 * labeled alternative in {@link TemplateParserParser#expression}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitBitOr(TemplateParserParser.BitOrContext ctx);
	/**
	 * Visit a parse tree produced by the {@code assignment}
	 * labeled alternative in {@link TemplateParserParser#expression}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitAssignment(TemplateParserParser.AssignmentContext ctx);
	/**
	 * Visit a parse tree produced by the {@code shift}
	 * labeled alternative in {@link TemplateParserParser#expression}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitShift(TemplateParserParser.ShiftContext ctx);
	/**
	 * Visit a parse tree produced by the {@code addSub}
	 * labeled alternative in {@link TemplateParserParser#expression}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitAddSub(TemplateParserParser.AddSubContext ctx);
	/**
	 * Visit a parse tree produced by the {@code floatingPointLiteral}
	 * labeled alternative in {@link TemplateParserParser#expression}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitFloatingPointLiteral(TemplateParserParser.FloatingPointLiteralContext ctx);
	/**
	 * Visit a parse tree produced by the {@code shortCircuitOr}
	 * labeled alternative in {@link TemplateParserParser#expression}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitShortCircuitOr(TemplateParserParser.ShortCircuitOrContext ctx);
	/**
	 * Visit a parse tree produced by the {@code prefixUnary}
	 * labeled alternative in {@link TemplateParserParser#expression}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitPrefixUnary(TemplateParserParser.PrefixUnaryContext ctx);
	/**
	 * Visit a parse tree produced by the {@code equalOrNotEqual}
	 * labeled alternative in {@link TemplateParserParser#expression}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitEqualOrNotEqual(TemplateParserParser.EqualOrNotEqualContext ctx);
	/**
	 * Visit a parse tree produced by the {@code bitAnd}
	 * labeled alternative in {@link TemplateParserParser#expression}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitBitAnd(TemplateParserParser.BitAndContext ctx);
	/**
	 * Visit a parse tree produced by the {@code stringLiteral}
	 * labeled alternative in {@link TemplateParserParser#expression}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitStringLiteral(TemplateParserParser.StringLiteralContext ctx);
	/**
	 * Visit a parse tree produced by the {@code integerLiteral}
	 * labeled alternative in {@link TemplateParserParser#expression}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitIntegerLiteral(TemplateParserParser.IntegerLiteralContext ctx);
	/**
	 * Visit a parse tree produced by the {@code xor}
	 * labeled alternative in {@link TemplateParserParser#expression}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitXor(TemplateParserParser.XorContext ctx);
	/**
	 * Visit a parse tree produced by the {@code booleanLiteral}
	 * labeled alternative in {@link TemplateParserParser#expression}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitBooleanLiteral(TemplateParserParser.BooleanLiteralContext ctx);
	/**
	 * Visit a parse tree produced by the {@code ternary}
	 * labeled alternative in {@link TemplateParserParser#expression}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitTernary(TemplateParserParser.TernaryContext ctx);
	/**
	 * Visit a parse tree produced by the {@code bean}
	 * labeled alternative in {@link TemplateParserParser#expression}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitBean(TemplateParserParser.BeanContext ctx);
	/**
	 * Visit a parse tree produced by the {@code shortCircuitAnd}
	 * labeled alternative in {@link TemplateParserParser#expression}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitShortCircuitAnd(TemplateParserParser.ShortCircuitAndContext ctx);
	/**
	 * Visit a parse tree produced by the {@code greatOrLess}
	 * labeled alternative in {@link TemplateParserParser#expression}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitGreatOrLess(TemplateParserParser.GreatOrLessContext ctx);
	/**
	 * Visit a parse tree produced by {@link TemplateParserParser#beanAccess}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitBeanAccess(TemplateParserParser.BeanAccessContext ctx);
	/**
	 * Visit a parse tree produced by {@link TemplateParserParser#objectAccess}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitObjectAccess(TemplateParserParser.ObjectAccessContext ctx);
	/**
	 * Visit a parse tree produced by {@link TemplateParserParser#propertyAccess}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitPropertyAccess(TemplateParserParser.PropertyAccessContext ctx);
	/**
	 * Visit a parse tree produced by {@link TemplateParserParser#arrayAccess}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitArrayAccess(TemplateParserParser.ArrayAccessContext ctx);
	/**
	 * Visit a parse tree produced by {@link TemplateParserParser#mapAccess}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitMapAccess(TemplateParserParser.MapAccessContext ctx);
	/**
	 * Visit a parse tree produced by {@link TemplateParserParser#methodCall}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitMethodCall(TemplateParserParser.MethodCallContext ctx);
	/**
	 * Visit a parse tree produced by {@link TemplateParserParser#callMethodParameters}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitCallMethodParameters(TemplateParserParser.CallMethodParametersContext ctx);
}