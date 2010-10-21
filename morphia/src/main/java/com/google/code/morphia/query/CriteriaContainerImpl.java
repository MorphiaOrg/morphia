package com.google.code.morphia.query;

import java.util.ArrayList;
import java.util.List;

import com.mongodb.BasicDBList;
import com.mongodb.BasicDBObject;
import com.mongodb.DBObject;

public class CriteriaContainerImpl extends AbstractCriteria implements Criteria, CriteriaContainer {
	private CriteriaJoin joinMethod;
	private List<Criteria> children;
	
	protected QueryImpl<?> query;
	
	protected CriteriaContainerImpl(CriteriaJoin joinMethod) {
		this.joinMethod = joinMethod;
		this.children = new ArrayList<Criteria>();
	}
	
	protected CriteriaContainerImpl(QueryImpl<?> query, CriteriaJoin joinMethod) {
		this(joinMethod);
		this.query = query;
	}
	
	public void add(Criteria... criteria) {
		for (Criteria c: criteria) {
			c.attach(this);
			this.children.add(c);
		}
	}
	
	public void remove(Criteria criteria) {
		this.children.remove(criteria);
	}
	
	public void addTo(DBObject obj) {
		if (this.joinMethod == CriteriaJoin.AND) {
			for (Criteria child: this.children) {
				child.addTo(obj);
			}
			
		} else if (this.joinMethod == CriteriaJoin.OR) {
			BasicDBList or = new BasicDBList();

			for (Criteria child: this.children) {
				BasicDBObject container = new BasicDBObject();
				child.addTo(container);
				or.add(container);
			}
			
			obj.put("$or", or);
		}
	}
	
	public CriteriaContainer and(Criteria... criteria) {
		CriteriaContainerImpl parent = new CriteriaContainerImpl(this.query, CriteriaJoin.AND);
		
		for (Criteria c: criteria) {
			parent.add(c);
		}
			
		this.add(parent);

		return parent;
	}
	
	
	public CriteriaContainer or(Criteria... criteria) {
		CriteriaContainerImpl parent = new CriteriaContainerImpl(this.query, CriteriaJoin.OR);
		
		for (Criteria c: criteria) {
			parent.add(c);
		}
		
		this.add(parent);
		
		return parent;
	}
	
	public FieldEnd<? extends CriteriaContainer> criteria(String name) {
		return this.criteria(name, this.query.isValidatingNames());
	}
	
	private FieldEnd<? extends CriteriaContainer> criteria(String field, boolean validateName) {
		return new FieldEndImpl<CriteriaContainerImpl>(this.query, field, this, validateName);
	}
}
