package com.ssafy.freezetag.domain.result.service;

import com.ssafy.freezetag.domain.result.entity.OXResult;
import com.ssafy.freezetag.domain.result.entity.redis.OXRedis;
import com.ssafy.freezetag.domain.result.repository.OXRedisRepository;
import com.ssafy.freezetag.domain.result.repository.OXResultRepository;
import com.ssafy.freezetag.domain.result.service.request.OXModifyRequestDto;
import com.ssafy.freezetag.domain.result.service.request.OXSaveRequestDto;
import com.ssafy.freezetag.domain.result.service.request.RoomIdRequestDto;
import com.ssafy.freezetag.domain.room.entity.MemberRoom;
import com.ssafy.freezetag.domain.room.repository.MemberRoomRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class OXResultService {

    private final OXRedisRepository oxRedisRepository;
    private final OXResultRepository oxResultRepository;
    private final MemberRoomRepository memberRoomRepository;

    public List<OXRedis> save(List<OXSaveRequestDto> oxSaveRequestDtoList) {
        List<OXRedis> oxRedisList = oxSaveRequestDtoList.stream()
                .map(oxSaveRequestDto -> {
                    OXRedis oxRedis = new OXRedis(
                            oxSaveRequestDto.getRoomId(),
                            oxSaveRequestDto.getMemberRoomId(),
                            oxSaveRequestDto.getContent(),
                            oxSaveRequestDto.isAnswer()
                    );
                    return oxRedisRepository.save(oxRedis);
                }).toList();

        return oxRedisList;
    }

    public OXRedis findById(String id){
        return oxRedisRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 OX입니다."));
    }

    public List<OXRedis> modify(List<OXModifyRequestDto> oxModifyRequestDtoList){
        List<OXRedis> ModifiedOXRedisList = oxModifyRequestDtoList.stream()
                .map(oxModifyRequestDto -> {
                    OXRedis findOXRedis = findById(oxModifyRequestDto.getId());
                    findOXRedis.update(oxModifyRequestDto.getContent(), oxModifyRequestDto.isAnswer());
                    return oxRedisRepository.save(findOXRedis);
                }).toList();

        return ModifiedOXRedisList;
    }

    public List<OXRedis> findAllByRoomId(RoomIdRequestDto roomIdRequestDto){
        return oxRedisRepository.findAllByRoomId(roomIdRequestDto.getRoomId());
    }

    @Transactional
    public void deleteAll(RoomIdRequestDto roomIdRequestDto){
        // DB 저장하기
        List<OXRedis> oxRedisList = oxRedisRepository.findAllByRoomId(roomIdRequestDto.getRoomId());

        List<OXResult> oxResultList = oxRedisList.stream()
                .map(oxRedis -> {
                    MemberRoom memberRoom = memberRoomRepository.findById(oxRedis.getMemberRoomId())
                            .orElseThrow(() -> new IllegalArgumentException("해당 memberRoomId가 존재하지 않습니다."));
                    return new OXResult(memberRoom, oxRedis.getContent(), oxRedis.getAnswer());
                }).toList();

        oxResultRepository.saveAll(oxResultList);

        // Redis에서 데이터 삭제하기
        oxRedisRepository.deleteAll(oxRedisList);
    }


}
