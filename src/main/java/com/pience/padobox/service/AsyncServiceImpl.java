package com.pience.padobox.service;

import java.util.ArrayList;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.ui.Model;

import com.pience.padobox.mapper.AsyncMapper;
import com.pience.padobox.model.AsyncDomain.*;

@Service
public class AsyncServiceImpl implements AsyncService{

	@Autowired
	AsyncMapper asyncMapper;
	
	@Override
	public List<StatusProcessAsyncLog> getStatusProcessAsyncLog(Model model){
		List<StatusProcessAsyncLog> asyncDomain = new ArrayList<StatusProcessAsyncLog>();
		asyncDomain = asyncMapper.getStatusProcessAsyncLog(model);
		return asyncDomain;
	}
	
	@Override
	public int insertStatusProcessAsyncLog(StatusProcessAsyncLog statusProcessAsyncLog) {
		int iRow = 0;
		iRow = asyncMapper.insertStatusProcessAsyncLog(statusProcessAsyncLog);
		return iRow;
	}
	
	@Override
	public int updateStatusProcessAsyncLog(Model model) {
		int iRow = 0;
		iRow = asyncMapper.updateStatusProcessAsyncLog(model);
		return iRow;
	}
	

	
}
