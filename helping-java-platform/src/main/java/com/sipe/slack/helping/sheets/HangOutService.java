package com.sipe.slack.helping.sheets;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class HangOutService {

    private final SheetsService sheetsService;

    public void saveHangoutAttendance(final String activity, final String name, final String attendance) {
        try {
            // Google Sheets에 데이터 저장
            sheetsService.saveHangoutAttendance(activity, name, attendance);
            log.info("뒷풀이 참석 정보 저장 완료: 활동={}, 이름={}, 참석여부={}", activity, name, attendance);
        } catch (Exception e) {
            log.error("뒷풀이 참석 정보를 저장하는 중 오류 발생", e);
            throw new RuntimeException("뒷풀이 참석 정보를 저장하는 중 오류 발생: " + e.getMessage(), e);
        }
    }
}
