package com.pience.padobox.service;

import java.util.List;

import org.springframework.ui.Model;

import com.pience.padobox.model.AsyncDomain.*;

public interface AsyncService {
	
	public List<StatusProcessAsyncLog> getStatusProcessAsyncLog(Model model);
	
	public int insertStatusProcessAsyncLog(StatusProcessAsyncLog statusProcessAsyncLog);
	
	public int updateStatusProcessAsyncLog(Model model);

}
