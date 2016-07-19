package de.gitzoz.commonfunctions

object CommonFunctions {
  def fallbackWithManyStrategies[T, U](value: T, strategies: List[T => List[U]], default: List[U]): List[U] = strategies match {
    case head :: tail => head(value) match {
      case result if result.isEmpty => fallbackWithManyStrategies(value, tail, default)
      case result: List[U]          => result
    }
    case Nil => default
  }
}