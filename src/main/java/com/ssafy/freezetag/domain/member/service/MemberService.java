package com.ssafy.freezetag.domain.member.service;

import com.ssafy.freezetag.domain.exception.custom.InvalidMemberVisibilityException;
import com.ssafy.freezetag.domain.exception.custom.MemberNotFoundException;
import com.ssafy.freezetag.domain.exception.custom.TokenException;
import com.ssafy.freezetag.domain.member.entity.Member;
import com.ssafy.freezetag.domain.member.repository.MemberHistoryRepository;
import com.ssafy.freezetag.domain.member.repository.MemberMemoryboxRepository;
import com.ssafy.freezetag.domain.member.repository.MemberRepository;
import com.ssafy.freezetag.domain.member.service.response.MemberHistoryDto;
import com.ssafy.freezetag.domain.member.service.response.MemberMemoryboxDto;
import com.ssafy.freezetag.domain.member.service.response.MypageResponseDto;
import com.ssafy.freezetag.domain.member.service.response.MypageVisibilityResponseDto;
import com.ssafy.freezetag.domain.oauth2.TokenProvider;
import com.ssafy.freezetag.domain.oauth2.service.TokenService;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.util.List;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
@RequiredArgsConstructor
public class MemberService {

    private final TokenProvider tokenProvider;
    private final TokenService tokenService;
    @Autowired
    private MemberRepository memberRepository;

    @Autowired
    private MemberHistoryRepository memberHistoryRepository;

    @Autowired
    private MemberMemoryboxRepository memberMemoryboxRepository;

    /*
        member 찾는 부분 메소드화
     */
    private Member findMember(Long memberId) {
        return memberRepository
                .findById(memberId)
                .orElseThrow(() -> new MemberNotFoundException("멤버가 존재하지 않습니다."));
    }
    /*
        memberId를 통해서 ResponseDto 생성
     */
    public MypageResponseDto getmypage(Long memberId) {
        Member member = findMember(memberId);

        List<MemberHistoryDto> memberHistoryList = memberHistoryRepository.findByMemberId(memberId).stream()
                .map(history -> MemberHistoryDto.builder()
                        .memberHistoryDate(history.getMemberHistoryDate())
                        .memberHistoryContent(history.getMemberHistoryContent())
                        .build())
                .collect(Collectors.toList());

        List<MemberMemoryboxDto> memberMemoryboxList = memberMemoryboxRepository.findByMemberId(memberId).stream()
                .map(memorybox -> MemberMemoryboxDto.builder()
                        .memberHistoryDate(memorybox.getMemberHistoryDate())
                        .memberHistoryContent(memorybox.getMemberHistoryContent())
                        .thumbnail(memorybox.getThumbnail())
                        .photo(memorybox.getPhoto())
                        .build())
                .collect(Collectors.toList());

        return MypageResponseDto.builder()
                .memberName(member.getMemberName())
                .memberProviderEmail(member.getMemberProviderEmail())
                .memberProfileImageUrl(member.getMemberProfileImageUrl())
                .memberIntroduction(member.getMemberIntroduction())
                .memberHistoryList(memberHistoryList)
                .memberMemoryboxList(memberMemoryboxList)
                .build();
    }

    /*
        memberId를 통해서 마이페이지 프로필 공개, 비공개 설정
     */
    @Transactional
    public MypageVisibilityResponseDto setMypageVisibility(Long memberId, Boolean requestVisibility) {

        // DB에 있는 visibility 정보 로드
        Member member = findMember(memberId);
        Boolean memberVisibility = member.isMemberVisibility();

        // 만약 DB에 있는 정보랑 요청한 정보가 일치하다면
        if(!memberVisibility.equals(requestVisibility)) {
            throw new InvalidMemberVisibilityException("멤버 공개 정보가 일치하지 않습니다.");
        }

        // DB 반영
        member.setMemberVisibility(!memberVisibility);

        // Toggle 느낌으로 구현
        return MypageVisibilityResponseDto.builder()
                .visibility(!memberVisibility)
                .build();
    }

    /*
        로그아웃 => token redis에서 삭제
     */
    // TODO : OAuth2Controller과 겹친 부분 utils로 빼기
    public void checkAuthentication(HttpServletRequest request) {
        Cookie[] cookies = request.getCookies();

        // 쿠키가 아예 존재하지 않나 확인
        if(cookies == null) {
            throw new RuntimeException("쿠키가 존재하지 않습니다.");
        }

        // 쿠키 조회
        String refreshToken = getRefreshToken(cookies);
        if(!StringUtils.hasText(refreshToken)) {
            throw new TokenException("Refresh Token이 존재하지 않습니다.");
        }

        // accessToken 조회
        String accessToken = request.getHeader("Authorization");



        // 그리고 access, refresh간 id 불일치 발생하면 처리
        if(!tokenProvider.validateSameTokens(accessToken, refreshToken)) {
            throw new TokenException("Access Token과 Refresh Token 사용자 정보 불일치합니다.");
        }

        Long memberId = tokenProvider.getMemberIdFromToken(refreshToken);
        // 레디스에서 정보 삭제
        tokenService.deleteRefreshToken(memberId.toString());

    }

    /*
        레디스의 refreshToken, 세션 정보 삭제
     */
    public void deleteToken(HttpServletRequest request, HttpServletResponse response) {

        // refreshToken 쿠키 삭제
        Cookie refreshTokenCookie = new Cookie("refreshToken", null);
        refreshTokenCookie.setMaxAge(0); // 쿠키의 유효 기간을 0으로 설정하여 삭제
        refreshTokenCookie.setPath("/"); // 쿠키가 유효한 경로 설정
        response.addCookie(refreshTokenCookie);

    }

    /*
    쿠키에서 refreshToken 찾아주는 코드
 */
    public String getRefreshToken(Cookie[] cookies) {
        String refreshToken = "";
        for (Cookie cookie : cookies) {
            if ("refreshToken".equals(cookie.getName())) {
                refreshToken = cookie.getValue();
                break;
            }
        }
        return refreshToken;
    }
}
