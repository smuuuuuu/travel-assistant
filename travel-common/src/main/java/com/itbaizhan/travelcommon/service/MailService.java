package com.itbaizhan.travelcommon.service;

import com.itbaizhan.travelcommon.result.BaseResult;

public interface MailService {

    BaseResult<?> sendMail(String to, String text, String title);
}
