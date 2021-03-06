/*                     __                                               *\
**     ________ ___   / /  ___      __ ____  Scala.js Test Suite        **
**    / __/ __// _ | / /  / _ | __ / // __/  (c) 2013, LAMP/EPFL        **
**  __\ \/ /__/ __ |/ /__/ __ |/_// /_\ \    http://scala-js.org/       **
** /____/\___/_/ |_/____/_/ | |__/ /____/                               **
**                          |/____/                                     **
\*                                                                      */
package scala.scalajs.test
package compiler

import scala.scalajs.js
import scala.scalajs.test.JasmineTest
import scala.scalajs.js.annotation.JSName
import scala.scalajs.js.annotation.JSBracketAccess

/*
 * Based on examples in:
 * http://lampwww.epfl.ch/~doeraene/scala-js/doc/js-interoperability.html
 */
object InteroperabilityTest extends JasmineTest {

  describe("JavaScript interoperability") {

    it("should support backquotes to escape Scala fields") {
      trait InteroperabilityTestFieldEscape extends js.Object {
        var `def`: Int
        def `val`(): Int = ???
        def `val`(n: Int): Int = ???
      }

      val obj = js.eval("""
        var interoperabilityTestFieldEscape = {
          def: 0,
          val: function(x) { if (x) this.def = x; return this.def; }
        };
        interoperabilityTestFieldEscape;
      """).asInstanceOf[InteroperabilityTestFieldEscape]

      obj.`def` = 7357
      expect(obj.`def`).toEqual(7357)
      expect(obj.`val`()).toEqual(7357)
      expect(obj.`val`(42)).toEqual(42)
    }

    it("should support @JSName to specify the JavaScript name for fields") {
      trait InteroperabilityTestJSName extends js.Object {
        @JSName("val")
        def value(): Int = ???
        @JSName("val")
        def value(n: Int): Int = ???
      }

      val obj = js.eval("""
        var interoperabilityTestJSName = {
          def: 42,
          val: function(x) { if (x) this.def = x; return this.def; }
        };
        interoperabilityTestJSName;
      """).asInstanceOf[InteroperabilityTestJSName]

      expect(obj.value()).toEqual(42)
      expect(obj.value(7357)).toEqual(7357)
    }

    it("should translate explicit getter and setter names to field access") {
      trait InteroperabilityTestProperty extends js.Object {
        def a_=(x: Int): Unit = ???
        def a: Int = ???
      }
      val obj = js.eval("""
        var interoperabilityTestProperty = { a: 1 };
        interoperabilityTestProperty;
        """).asInstanceOf[InteroperabilityTestProperty]

      expect(obj.a).toEqual(1)
      obj.a = 100
      expect(obj.a).toEqual(100)
    }

    it("should support @JSName together with field access") {
      trait InteroperabilityTestPropertyNamed extends js.Object {
        @JSName("b")
        def a_=(x: Int): Unit = ???
        @JSName("b")
        def a: Int = ???
        def b: Int = ???
      }
      val obj = js.eval("""
        var interoperabilityTestProperty = { b: 1 };
        interoperabilityTestProperty;
        """).asInstanceOf[InteroperabilityTestPropertyNamed]

      expect(obj.a).toEqual(1)
      obj.a = 100
      expect(obj.a).toEqual(100)
      expect(obj.b).toEqual(100)
    }

    it("should support @JSBracketAccess to specify access using []-subscription") {
      trait InteroperabilityTestJSBracketAccess extends js.Object {
        @JSBracketAccess
        def apply(index: Int): Int = ???
        @JSBracketAccess
        def update(index: Int, v: Int): Unit = ???
      }

      val obj = js.eval("""
        var interoperabilityTestJSBracketAccess = [ 0, 1, 7357 ];
        interoperabilityTestJSBracketAccess;
      """).asInstanceOf[InteroperabilityTestJSBracketAccess]

      expect(obj(2)).toEqual(7357)
      obj(2) = 42
      expect(obj(2)).toEqual(42)
    }

    it("should allow instanciation of JS classes inheriting from js.Object") {
      js.eval("""
        var InteroperabilityTestInherit = {
          Pattern: function(x) {
            this.field = 42;
            this.method = function() {
              return '42';
            };
            this.getConstructorParam = function() {
              return x;
            };
          }
      }
      """)

      val obj = new InteroperabilityTestPattern("Scala.js")
      expect(obj.field).toEqual(42)
      expect(obj.method).toEqual("42")
      expect(obj.getConstructorParam).toEqual("Scala.js")
    }

    it("should acces top-level JS objects via Scala objects inheriting from js.Object") {
      js.eval("""
        var InteroperabilityTestTopLevelObject = function(value) {
          return {
            value: value,
            valueAsInt: function() {
              return parseInt(value);
            }
          };
    	}
      """)

      // Use alias for convenience: see end of file for definition
      val TopLevel = InteroperabilityTestTopLevel

      val obj = TopLevel("7357")
      expect(obj.value).toEqual("7357")
      expect(obj.valueAsInt).toEqual(7357)
    }

    it("should acces top-level JS objects via Scala object inheriting from js.GlobalScope") {
      js.eval("""
        var interoperabilityTestGlobalScopeValue = "7357";
        var interoperabilityTestGlobalScopeValueAsInt = function() {
          return parseInt(interoperabilityTestGlobalScopeValue);
        };
      """)

      // Use alias for convenience: see end of file for definition
      val Global = InteroperabilityTestGlobalScope

      expect(Global.interoperabilityTestGlobalScopeValue).toEqual("7357")
      expect(Global.interoperabilityTestGlobalScopeValueAsInt).toEqual(7357)

      Global.interoperabilityTestGlobalScopeValue = "42"
      expect(Global.interoperabilityTestGlobalScopeValueAsInt).toEqual(42)
    }

    it("should protect receiver of raw JS apply if it's a select") {
      trait InteroperabilityTestRawReceiver extends js.Object {
        val check: js.Function1[Int, Int] = ???
      }

      val rawReceiver = js.eval("""
        var interoperabilityTestRawReceiver = {
          member: 0xbad,
          check: function(raw) { return this.member ? this.member : raw; }
        };
        interoperabilityTestRawReceiver;
      """).asInstanceOf[InteroperabilityTestRawReceiver]

      expect(rawReceiver.check(7357)).toEqual(7357)

      val check = rawReceiver.check
      expect(check(0x600d)).toEqual(0x600d)
    }

    it("should properly handle default parameters") {
      /** Trait with different method signatures, all forwarded to the same
       *  JS raw function that returns the argument list for inspection
       */
      trait InteroperabilityTestDefaultParam extends js.Object {
        @JSName("fun")
        def simple(x: Int, y: Int = 5): js.Any = ???
        @JSName("fun")
        def named(x: Int = 1, y: Int = 1, z: Int = 1): js.Any = ???
        @JSName("fun")
        def multi(x: Int = 1)(ys: Int*)(z: Int = 1): js.Any = ???
      }

      val obj = js.eval("""
        var interoperabilityTestDefaultParam = {
          fun: function() { return arguments; }
        };
        interoperabilityTestDefaultParam;
      """).asInstanceOf[InteroperabilityTestDefaultParam]

      // Helpers
      import js.Dynamic.{literal => args}
      val undef = js.undefined

      expect(obj.simple(1)).toEqual(args(`0` = 1))
      expect(obj.simple(1,5)).toEqual(args(`0` = 1, `1` = 5))
      expect(obj.named(y = 5)).toEqual(args(`0` = undef, `1` = 5))
      expect(obj.named(x = 5)).toEqual(args(`0` = 5))
      expect(obj.multi()(1,2,3,4)()).toEqual(args(
          `0` = undef,
          `1` = 1,
          `2` = 2,
          `3` = 3,
          `4` = 4))
      expect(obj.multi(2)()(5)).toEqual(args(
          `0` = 2,
          `1` = 5))
    }

    it("should generate exports for methods inherited from traits - #178") {
      import js.annotation.JSExport

      trait Foo {
        @JSExport
        def theValue = 1
      }
      class Bar extends Foo

      val x = (new Bar).asInstanceOf[js.Dynamic]

      // Call the export by using js.Dynamic
      expect(x.theValue).toEqual(1)
    }

    it("should unbox Chars received from calling a JS interop method") {
      trait InteroperabilityTestCharResult extends js.Object {
        def get(): Char = ???
      }

      val obj = js.eval("""
        var obj = {
          get: function() { return JSUtils().stringToChar('e'); }
        };
        obj;
      """).asInstanceOf[InteroperabilityTestCharResult]

      expect(obj.get().toInt).toEqual('e'.toInt)
    }

    it("should box Chars given to a JS interop method") {
      trait InteroperabilityTestCharParam extends js.Object {
        def twice(c: Char): String = ???
      }

      val obj = js.eval("""
        var obj = {
          twice: function(c) { c = JSUtils().charToString(c); return c+c; }
        };
        obj;
      """).asInstanceOf[InteroperabilityTestCharParam]

      expect(obj.twice('x')).toEqual("xx")
    }

    it("should unbox value classes received from calling a JS interop method") {
      trait InteroperabilityTestValueClassResult extends js.Object {
        def test(vc: Any): SomeValueClass = ???
      }

      val obj = js.eval("""
        var obj = {
          test: function(vc) { return vc; }
        };
        obj;
      """).asInstanceOf[InteroperabilityTestValueClassResult]

      val r = obj.test(new SomeValueClass(5))
      expect(r.i).toEqual(5)
    }

    it("should box value classes given to a JS interop method") {
      trait InteroperabilityTestValueClassParam extends js.Object {
        def stringOf(vc: SomeValueClass): String = ???
      }

      val obj = js.eval("""
        var obj = {
          stringOf: function(vc) { return vc.toString(); }
        };
        obj;
      """).asInstanceOf[InteroperabilityTestValueClassParam]

      val vc = new SomeValueClass(7)
      expect(obj.stringOf(vc)).toEqual("SomeValueClass(7)")
    }

    it("should protect conversions from JS types to Scala types") {
      class Foo
      val foo: Any = new Foo

      val invalidNumber: js.prim.Number = foo.asInstanceOf[js.prim.Number]
      val nullNumber: js.prim.Number = null
      expect(() => invalidNumber: Double).toThrow
      expect(nullNumber: Double).toEqual(0)

      val invalidBoolean: js.prim.Boolean = foo.asInstanceOf[js.prim.Boolean]
      val nullBoolean: js.prim.Boolean = null
      expect(() => invalidBoolean: Boolean).toThrow
      expect(nullBoolean: Boolean).toEqual(false)

      val invalidString: js.prim.String = foo.asInstanceOf[js.prim.String]
      val nullString: js.prim.String = null
      expect(() => invalidString: String).toThrow
      expect(nullString: String).toBeNull
    }

    it("should asInstanceOf values received from calling a JS interop method") {
      trait InteroperabilityTestAsInstanceOfResult extends js.Object {
        def testChar(): Char = ???
        def testInt(): Int = ???
        def testShort(): Short = ???
        def testDouble(): Double = ???
        def testString(): String = ???
        def testValueClass(): SomeValueClass = ???
        def testNormalClass(): List[Int] = ???
        def testAny(): Any = ???
      }

      val obj = js.eval("""
        var obj = {
          testChar: function() { return 5; },
          testInt: function() { return 6.4; },
          testShort: function() { return 60000; },
          testDouble: function() { return JSUtils().stringToChar('e'); },
          testString: function() { return {}; },
          testValueClass: function() { return "hello"; },
          testNormalClass: function() { return 45; },
          testAny: function() { return {}; }
        };
        obj;
      """).asInstanceOf[InteroperabilityTestAsInstanceOfResult]

      expect(() => obj.testChar()).toThrow
      expect(() => obj.testInt()).toThrow
      expect(() => obj.testShort()).toThrow
      expect(() => obj.testDouble()).toThrow
      expect(() => obj.testString()).toThrow
      expect(() => obj.testValueClass()).toThrow
      expect(() => obj.testNormalClass()).toThrow
      expect(() => obj.testAny()).not.toThrow
    }

  }
}

/*
 * Helper classes, traits and objects defined here since they cannot be nested.
 */

@JSName("InteroperabilityTestInherit.Pattern")
class InteroperabilityTestPattern protected () extends js.Object {
  def this(pattern: String) = this()
  val field: Int = ???
  def method(): String = ???
  def getConstructorParam(): String = ???
}

trait InteroperabilityTestTopLevel extends js.Object {
  val value: String = ???
  def valueAsInt(): Int = ???
}

@JSName("InteroperabilityTestTopLevelObject")
object InteroperabilityTestTopLevel extends js.Object {
  def apply(value: String): InteroperabilityTestTopLevel = ???
}

object InteroperabilityTestGlobalScope extends js.GlobalScope {
  var interoperabilityTestGlobalScopeValue: String = ???
  def interoperabilityTestGlobalScopeValueAsInt(): Int = ???
}

class SomeValueClass(val i: Int) extends AnyVal {
  override def toString(): String = s"SomeValueClass($i)"
}
