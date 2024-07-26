package com.ssafy.freezetag.domain.result.service.response;

import lombok.Builder;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public class IntroSaveResponseDto {
    private final String id;

    private final Long roomId;

    private final Long memberRoomId;

    private final String content;
}
