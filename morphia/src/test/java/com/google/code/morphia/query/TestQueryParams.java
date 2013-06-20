package com.google.code.morphia.query;


import java.util.Collections;

import org.junit.Test;
import com.google.code.morphia.TestBase;
import com.google.code.morphia.TestMapping.BaseEntity;
import com.google.code.morphia.annotations.Entity;
import com.google.code.morphia.testutil.AssertedFailure;


public class TestQueryParams extends TestBase {
  @Entity
  static class E extends BaseEntity {

  }

  @Test
  public void testNullAcceptance() throws Exception {
    final Query<E> q = ds.createQuery(E.class);
    final FieldEnd<?> e = q.field("_id");

    // have to succeed:
    e.equal(null);
    e.notEqual(null);
    e.hasThisOne(null);

    // have to fail:
    new AssertedFailure() {
      @Override
      public void thisMustFail() {
        e.greaterThan(null);
      }
    };

    new AssertedFailure() {
      @Override
      public void thisMustFail() {
        e.greaterThanOrEq(null);
      }
    };

    new AssertedFailure() {
      @Override
      public void thisMustFail() {
        e.hasAllOf(null);
      }
    };

    new AssertedFailure() {
      @Override
      public void thisMustFail() {
        e.hasAnyOf(null);
      }
    };

    new AssertedFailure() {
      @Override
      public void thisMustFail() {
        e.hasNoneOf(null);
      }
    };

    new AssertedFailure() {
      @Override
      public void thisMustFail() {
        e.hasThisElement(null);
      }
    };

    new AssertedFailure() {
      @Override
      public void thisMustFail() {
        e.lessThan(null);
      }
    };

    new AssertedFailure() {
      @Override
      public void thisMustFail() {
        e.lessThanOrEq(null);
      }
    };

    new AssertedFailure() {
      @Override
      public void thisMustFail() {
        e.startsWith(null);
      }
    };

    new AssertedFailure() {
      @Override
      public void thisMustFail() {
        e.startsWithIgnoreCase(null);
      }
    };
  }

  @Test
  public void testEmptyCollectionAcceptance() throws Exception {
    final Query<E> q = ds.createQuery(E.class);
    final FieldEnd<?> e = q.field("_id");

    new AssertedFailure() {
      @Override
      public void thisMustFail() {
        e.hasAllOf(Collections.emptyList());
      }
    };

    new AssertedFailure() {
      @Override
      public void thisMustFail() {
        e.hasNoneOf(Collections.emptyList());
      }
    };

    //		new AssertedFailure() {
    //			public void thisMustFail() throws Throwable {
    //				e.hasAnyOf(Collections.EMPTY_LIST);
    //			}
    //		};
  }
}
