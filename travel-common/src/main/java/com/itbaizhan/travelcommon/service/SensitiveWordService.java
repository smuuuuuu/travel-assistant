package com.itbaizhan.travelcommon.service;

import java.util.List;

public interface SensitiveWordService {

    void insert(List<String> word);

    void delete(List<String> word);

}
