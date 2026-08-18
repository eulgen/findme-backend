package com.geolink.findme.authservice.service;

import com.geolink.findme.authservice.entity.OtpPurpose;
import com.geolink.findme.authservice.entity.User;

public interface OtpService {
    String generate(User user, OtpPurpose purpose);
    void verify(User user, OtpPurpose purpose, String rawCode);
}
