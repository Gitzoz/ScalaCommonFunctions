package de.gitzoz.commonfunctions

import org.scalatest.WordSpecLike
import org.scalatest.Matchers

class CommonFunctionsSpecs extends WordSpecLike with Matchers{
  "A fallbackWithManyStrategies" should {
    "fallback to the default value if every other strategy returns an empty result" in {
      val value = "test" 
      val strategies = List((item: String) => List.empty, (item: String) => List.empty)
      val default = List("a")
      val result = CommonFunctions.fallbackWithManyStrategies(value, strategies, default)
      assert(result == List("a"))
    }
    
    "return value from first function" in {
      val value = "test" 
      val strategies = List((item: String) => List("b"), (item: String) => List.empty)
      val default = List("a")
      val result = CommonFunctions.fallbackWithManyStrategies(value, strategies, default)
      assert(result == List("b"))
    }
    
    "return value from second function" in {
      val value = "test" 
      val strategies = List((item: String) => List.empty, (item: String) => List("c"))
      val default = List("a")
      val result = CommonFunctions.fallbackWithManyStrategies(value, strategies, default)
      assert(result == List("c"))
    }
  }
}