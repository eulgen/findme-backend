package com.geolink.findme.service.otpService;

import com.geolink.findme.entity.OtpPurpose;
import com.geolink.findme.entity.User;

public interface OtpService {
    String generate(User user, OtpPurpose purpose);
    void verify(User user, OtpPurpose purpose, String rawCode);
}
