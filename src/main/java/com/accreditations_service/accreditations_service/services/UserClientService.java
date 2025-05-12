package com.accreditations_service.accreditations_service.services;

import com.accreditations_service.accreditations_service.exceptions.UserException;

public interface UserClientService {
    Long getUserIdFromEmail(String email) throws UserException;
}
