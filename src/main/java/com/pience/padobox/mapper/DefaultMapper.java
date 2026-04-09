package com.pience.padobox.mapper;

import org.apache.ibatis.annotations.Mapper;
import org.springframework.ui.Model;

import com.pience.padobox.model.ErrorReturnDomain;

@Mapper
public interface DefaultMapper {
	
	ErrorReturnDomain getRestErrorSingle(Model model_error);
	
	int insertErrorLog(Model model);

}
