package com.google.code.morphia.query;

import com.mongodb.WriteResult;

public class UpdateResults<T> {
	private WriteResult wr;
	
	public UpdateResults(WriteResult wr) {
		this.wr = wr;
	}
	
	public String getError() {
		return wr.getLastError().getErrorMessage();
	}
	public boolean getHadError() {
		String error = getError();
		return error != null && !error.isEmpty();
	}
	public boolean getUpdatedExisting() {
		return wr.getLastError().containsField("updatedExisting") ? (Boolean)wr.getLastError().get("updatedExisting") : false;
	}
	public int getUpdatedCount() {
		return getUpdatedExisting() ? getN() : 0;
	}
	protected int getN() {
		return wr.getLastError().containsField("n") ? ((Number)wr.getLastError().get("n")).intValue() : 0;
	}
	public int getInsertedCount() {
		return !getUpdatedExisting() ? getN() : 0;
	}
	public Object getNewId() {
		return getInsertedCount() == 1 && wr.getLastError().containsField("upserted") ? wr.getLastError().get("upserted") : null ;
	}
	public WriteResult getWriteResult() {return wr;}
} 
