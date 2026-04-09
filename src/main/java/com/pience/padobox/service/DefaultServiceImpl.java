package com.pience.padobox.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.ui.Model;

import com.pience.padobox.mapper.DefaultMapper;
import com.pience.padobox.model.ErrorReturnDomain;

@Service
public class DefaultServiceImpl implements DefaultService{

	@Autowired
	DefaultMapper defaultMapper;

	@Override
	public ErrorReturnDomain getRestErrorSingle(Model model_error) {
		ErrorReturnDomain error_return_domain = new ErrorReturnDomain();
		error_return_domain = defaultMapper.getRestErrorSingle(model_error);
		return error_return_domain;
	}
	
	@Override
	public int insertErrorLog(Model model) {
		int iRow = 0;
		iRow = defaultMapper.insertErrorLog(model);
		return iRow;
	}
	
	
}
