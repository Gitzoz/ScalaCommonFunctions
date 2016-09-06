/*
 * Copyright 2016 Stefan Roehrbein
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package de.gitzoz.scalacommonfunctions

import org.scalatest.WordSpecLike
import org.scalatest.Matchers

class CommonFunctionsSpecs extends WordSpecLike with Matchers {
  "A fallbackWithManyStrategies" should {
    "fallback to the default value if every other strategy returns an empty result" in {
      val value = "test"
      val strategies =
        List((item: String) => List.empty, (item: String) => List.empty)
      val default = List("a")
      val result =
        CommonFunctions.fallbackWithManyStrategies(value, strategies, default)
      assert(result == List("a"))
    }

    "return value from first function" in {
      val value = "test"
      val strategies =
        List((item: String) => List("b"), (item: String) => List.empty)
      val default = List("a")
      val result =
        CommonFunctions.fallbackWithManyStrategies(value, strategies, default)
      assert(result == List("b"))
    }

    "return value from second function" in {
      val value = "test"
      val strategies =
        List((item: String) => List.empty, (item: String) => List("c"))
      val default = List("a")
      val result =
        CommonFunctions.fallbackWithManyStrategies(value, strategies, default)
      assert(result == List("c"))
    }
  }
}
