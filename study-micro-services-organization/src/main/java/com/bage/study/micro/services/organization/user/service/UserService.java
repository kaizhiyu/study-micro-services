package com.bage.study.micro.services.organization.user.service;

import com.bage.study.micro.services.organization.user.domain.User;

public interface UserService {

	User getUserById(String id);

}