package com.onthegomap.planetiler.expression;

import static com.onthegomap.planetiler.TestUtils.newPoint;
import static com.onthegomap.planetiler.expression.Expression.*;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.onthegomap.planetiler.reader.SimpleFeature;
import com.onthegomap.planetiler.reader.SourceFeature;
import com.onthegomap.planetiler.reader.WithTags;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.Test;

class ExpressionTest {

  public static final Expression.MatchAny matchAB = matchAny("a", "b");
  public static final Expression.MatchAny matchCD = matchAny("c", "d");
  public static final Expression.MatchAny matchBC = matchAny("b", "c");

  static SourceFeature featureWithTags(String... tags) {
    Map<String, Object> map = new HashMap<>();
    for (int i = 0; i < tags.length; i += 2) {
      map.put(tags[i], tags[i + 1]);
    }
    return SimpleFeature.create(newPoint(0, 0), map);
  }

  @Test
  void testSimplify() {
    assertEquals(matchAB, matchAB.simplify());
  }

  @Test
  void testSimplifyAdjacentOrs() {
    assertEquals(or(matchAB, matchCD),
      or(or(matchAB), or(matchCD)).simplify()
    );
  }

  @Test
  void testSimplifyOrWithOneChild() {
    assertEquals(matchAB, or(matchAB).simplify());
  }

  @Test
  void testSimplifyOAndWithOneChild() {
    assertEquals(matchAB, and(matchAB).simplify());
  }

  @Test
  void testSimplifyDeeplyNested() {
    assertEquals(matchAB, or(or(and(and(matchAB)))).simplify());
  }

  @Test
  void testSimplifyDeeplyNested2() {
    assertEquals(or(matchAB, matchBC),
      or(or(and(and(matchAB)), matchBC)).simplify());
  }

  @Test
  void testSimplifyDeeplyNested3() {
    assertEquals(or(and(matchAB, matchCD), matchBC),
      or(or(and(and(matchAB), matchCD), matchBC)).simplify());
  }

  @Test
  void testNotNot() {
    assertEquals(matchAB, not(not(matchAB)).simplify());
  }

  @Test
  void testDemorgans() {
    assertEquals(or(not(matchAB), not(matchBC)), not(and(matchAB, matchBC)).simplify());
    assertEquals(and(not(matchAB), not(matchBC)), not(or(matchAB, matchBC)).simplify());
  }

  @Test
  void testSimplifyFalse() {
    assertEquals(FALSE, and(FALSE).simplify());
    assertEquals(FALSE, and(FALSE, matchAB).simplify());
    assertEquals(FALSE, or(FALSE).simplify());
  }

  @Test
  void testSimplifyTrue() {
    assertEquals(TRUE, and(TRUE).simplify());
    assertEquals(matchAB, and(TRUE, matchAB).simplify());
    assertEquals(TRUE, or(TRUE, matchAB).simplify());
  }

  @Test
  void testSimplifyAndCases() {
    assertEquals(TRUE, and(TRUE).simplify());
    assertEquals(TRUE, and(TRUE, TRUE).simplify());
    assertEquals(TRUE, and(TRUE, and()).simplify());
    assertEquals(TRUE, and(and(TRUE)).simplify());
    assertEquals(TRUE, and(and(TRUE), TRUE).simplify());
    assertEquals(TRUE, and(TRUE, and(TRUE), TRUE).simplify());
    assertEquals(matchAB, and(TRUE, and(TRUE), matchAB).simplify());
  }

  @Test
  void testSimplifyOrCases() {
    assertEquals(FALSE, or(or(FALSE)).simplify());
    assertEquals(FALSE, or(or(FALSE), FALSE).simplify());
    assertEquals(FALSE, or(FALSE, or(FALSE), FALSE).simplify());
    assertEquals(matchAB, or(FALSE, or(FALSE), matchAB).simplify());
  }

  @Test
  void testSimplifyNotCases() {
    assertEquals(FALSE, not(TRUE).simplify());
    assertEquals(TRUE, not(FALSE).simplify());
  }

  @Test
  void testEvaluateEmptyAnd() {
    assertEquals(
      and().evaluate(featureWithTags(), new ArrayList<>()),
      and().simplify().evaluate(featureWithTags(), new ArrayList<>())
    );
  }

  @Test
  void testReplace() {
    assertEquals(
      or(not(matchCD), matchCD, and(matchCD, matchBC)),
      or(not(matchAB), matchAB, and(matchAB, matchBC))
        .replace(matchAB, matchCD));
  }

  @Test
  void testReplacePredicate() {
    assertEquals(
      or(not(matchCD), matchCD, and(matchCD, matchCD)),
      or(not(matchCD), matchCD, and(matchCD, matchCD))
        .replace(e -> Set.of(matchAB, matchBC).contains(e), matchCD));
  }

  @Test
  void testContains() {
    assertTrue(matchCD.contains(e -> e.equals(matchCD)));
    assertTrue(or(not(matchCD)).contains(e -> e.equals(matchCD)));
    assertFalse(matchCD.contains(e -> e.equals(matchAB)));
    assertFalse(or(not(matchCD)).contains(e -> e.equals(matchAB)));
  }

  @Test
  void testStringifyExpression() {
    //Ensure Expression.toString() returns valid Java code
    assertEquals("matchAny(\"key\", \"true\")", matchAny("key", "true").generateJavaCode());
    assertEquals("matchAny(\"key\", \"foo\")", matchAny("key", "foo").generateJavaCode());
    var expression = matchAnyTyped("key", WithTags::getDirection, 1);
    assertThrows(UnsupportedOperationException.class, expression::generateJavaCode);
  }
}
