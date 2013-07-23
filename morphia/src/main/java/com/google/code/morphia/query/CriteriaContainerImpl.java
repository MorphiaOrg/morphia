package com.google.code.morphia.query;


import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.mongodb.BasicDBList;
import com.mongodb.BasicDBObject;
import com.mongodb.DBObject;


public class CriteriaContainerImpl extends AbstractCriteria implements CriteriaContainer {
  protected final CriteriaJoin   joinMethod;
  protected List<Criteria> children;

  protected QueryImpl<?> query;

  protected CriteriaContainerImpl(final CriteriaJoin joinMethod) {
    this.joinMethod = joinMethod;
    children = new ArrayList<Criteria>();
  }

  protected CriteriaContainerImpl(final QueryImpl<?> query, final CriteriaJoin joinMethod) {
    this(joinMethod);
    this.query = query;
  }

  public void add(final Criteria... criteria) {
    for (final Criteria c : criteria) {
      c.attach(this);
      children.add(c);
    }
  }

  public void remove(final Criteria criteria) {
    children.remove(criteria);
  }

  public void addTo(final DBObject obj) {
	if (joinMethod == CriteriaJoin.AND) {
		Map<String,Integer> fields = new HashMap<String,Integer>();      
		for (final Criteria child : children) {
			String field = child.getFieldName();
			if (null != field) {
				if (fields.containsKey(field))
					fields.put(field, fields.get(field)+1);
				else
					fields.put(field, 1);
        	}
      	}
		List<Criteria> ands = new ArrayList<Criteria>();
		for (Criteria child: this.children) {
			String field = child.getFieldName();
			if (null != field || fields.get(field) <= 1) {
				child.addTo(obj);
			}
			else {
				ands.add(child);
			}
		}
		if (ands.size()>0) {
        	//use $and
        	final BasicDBList and = new BasicDBList();

        	for (final Criteria child : ands) {
        		final BasicDBObject container = new BasicDBObject();
        		child.addTo(container);
        		and.add(container);
        	}

        	obj.put("$and", and);
      	}
	} else if (joinMethod == CriteriaJoin.OR) {
      	final BasicDBList or = new BasicDBList();

      	for (final Criteria child : children) {
        	final BasicDBObject container = new BasicDBObject();
        	child.addTo(container);
        	or.add(container);
      	}

      	obj.put("$or", or);
	}
  }

  public CriteriaContainer and(final Criteria... criteria) {
    return collect(CriteriaJoin.AND, criteria);
  }

  public CriteriaContainer or(final Criteria... criteria) {
    return collect(CriteriaJoin.OR, criteria);
  }

  private CriteriaContainer collect(final CriteriaJoin cj, final Criteria... criteria) {
    final CriteriaContainerImpl parent = new CriteriaContainerImpl(query, cj);

    for (final Criteria c : criteria) {
      parent.add(c);
    }

    add(parent);

    return parent;
  }

  public FieldEnd<? extends CriteriaContainer> criteria(final String name) {
    return criteria(name, query.isValidatingNames());
  }

  private FieldEnd<? extends CriteriaContainer> criteria(final String field, final boolean validateName) {
    return new FieldEndImpl<CriteriaContainerImpl>(query, field, this, validateName);
  }

  public String getFieldName() {
    return joinMethod.toString();
  }
}
