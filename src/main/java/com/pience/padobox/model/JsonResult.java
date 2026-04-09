package com.pience.padobox.model;

import java.util.ArrayList;

public class JsonResult<T> {
	
	private int res_code;
	private String res_value;
	
	private ArrayList<?> results;
	
	private T result;
	
	
	public int getRes_code() {
		return res_code;
	}
	public void setRes_code(int res_code) {
		this.res_code = res_code;
	}
	public String getRes_value() {
		return res_value;
	}
	public void setRes_value(String res_value) {
		this.res_value = res_value;
	}
	public ArrayList<?> getResults() {
		return results;
	}
	public void setResults(ArrayList<?> results) {
		this.results = results;
	}
	public T getResult() {
		return result;
	}
	public void setResult(T result) {
		this.result = result;
	}
	
}