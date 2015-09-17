package co.mglvl.spreadsheet.interpreter

import co.mglvl.spreadsheet._
import co.mglvl.spreadsheet.frp.{Cell, Exp}

object Interpreter {

  /*
  def getReferences(exp: Expression): Set[Int] = {
    exp match {
      case _: Literal => Set.empty
      case CellReference(id) => Set(id)
      case Add(left,right) => getReferences(left) union getReferences(right)
      case Multiply(left,right) => getReferences(left) union getReferences(right)
    }
  }
  */

  def evaluate(expression: Expression[_])(env: Int => Cell[LiteralValue]): Exp[LiteralValue] =
    expression match {
      case floatExpression: FloatExpression => evaluateFloatExpression(floatExpression)(env)
      case booleanExpression: BooleanExpression => evaluateBooleanExpression(booleanExpression)(env)
      case cellRef: CellReference[_] => env(cellRef.id).get()
      case IfElse(condition,ifTrue,ifNot) =>
        evaluateBooleanExpression(condition)(env).flatMap { condition =>
          if(condition.value) {
            evaluate(ifTrue)(env)
          } else {
            evaluate(ifNot)(env)
          }
        }
    }

  def evaluateFloatExpression(expression: FloatExpression)(env: Int => Cell[LiteralValue]): Exp[FloatValue] =
    expression match {
      case LiteralFloat(n)     => Exp.unit( n )
      case FloatReference(id)           => env(id).get().map(_.asInstanceOf[FloatValue])
      case Add            (left,right)  => Exp.map2( evaluateFloatExpression(left)(env) , evaluateFloatExpression(right)(env) ){ _ + _ }
      case Multiply       (left,right)  => Exp.map2( evaluateFloatExpression(left)(env) , evaluateFloatExpression(right)(env) ){ _ * _ }
    }

  def evaluateBooleanExpression(expression: BooleanExpression)(env: Int => Cell[LiteralValue]): Exp[BooleanValue] =
    expression match {
      case BooleanReference(id)   => env(id).get().map(_.asInstanceOf[BooleanValue])
      case comparison: Comparison => evaluateComparison(comparison)(env)
    }

  def evaluateComparison(comparison: Comparison)(env: Int => Cell[LiteralValue]): Exp[BooleanValue] = {
    def joinWith(f: (FloatValue, FloatValue) => BooleanValue) = {
      Exp.map2(evaluateFloatExpression(comparison.left)(env), evaluateFloatExpression(comparison.right)(env))(f)
    }
    comparison match {
      case LessThanOrEqual    (left, right) => joinWith(_ <= _)
      case GreaterThanOrEqual (left, right) => joinWith(_ >= _)
      case Equal              (left, right) => joinWith(_ == _)
    }
  }


}
