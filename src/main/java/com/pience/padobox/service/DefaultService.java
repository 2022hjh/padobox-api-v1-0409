package com.pience.padobox.service;

import org.springframework.ui.Model;

import com.pience.padobox.model.*;

public interface DefaultService {
	
	ErrorReturnDomain getRestErrorSingle(Model model_error);
	
	int insertErrorLog(Model model);

}
