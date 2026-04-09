package com.pience.padobox.mapper;

import java.util.List;

import org.apache.ibatis.annotations.Mapper;
import org.springframework.ui.Model;

import com.pience.padobox.model.AsyncDomain.*;

@Mapper
public interface AsyncMapper {
	
	List<StatusProcessAsyncLog> getStatusProcessAsyncLog(Model model);
	
	int insertStatusProcessAsyncLog(StatusProcessAsyncLog statusProcessAsyncLog);
	
	int updateStatusProcessAsyncLog(Model model);

}
