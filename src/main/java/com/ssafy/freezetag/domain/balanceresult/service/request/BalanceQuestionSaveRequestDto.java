package com.ssafy.freezetag.domain.balanceresult.service.request;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public class BalanceQuestionSaveRequestDto {
    private final Long roomId;

    private final String optionFirst;

    private final String optionSecond;
}