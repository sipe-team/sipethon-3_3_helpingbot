package com.sipe.slack.helping.sheets;

import static java.util.stream.Collectors.toMap;

import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.json.jackson2.JacksonFactory;
import com.google.api.services.sheets.v4.Sheets;
import com.google.api.services.sheets.v4.model.UpdateValuesResponse;
import com.google.api.services.sheets.v4.model.ValueRange;
import com.google.auth.http.HttpCredentialsAdapter;
import com.google.auth.oauth2.GoogleCredentials;
import com.sipe.slack.helping.attendance.FindMeSheetDto;
import com.sipe.slack.helping.sheets.dto.Attendance;
import com.sipe.slack.helping.sheets.dto.CrewMember;
import com.sipe.slack.helping.attendance.FindMeCrewMember;

import java.io.IOException;
import java.security.GeneralSecurityException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;

import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class SheetsService {

  private static final String APPLICATION_NAME = "Google Sheets Application";
  private static final String CREDENTIALS_FILE_PATH = "/googlesheet/google.json";
  private static final JacksonFactory JSON_FACTORY = JacksonFactory.getDefaultInstance();
  public static final int START_CREW_MEMBER_ROW = 2;
  public static final char START_WEEK_COLUMN = 'D';
  private static final String HANGOUT_SHEET_NAME = "Hangout";

  private final String GOOGLE_SHEET_ID = System.getenv("GOOGLE_SHEET_ID");

  private Sheets sheets;

  public void attendance(List<String> members, int week) {
    try {
      Sheets sheets = getSheets();
      Map<String, Integer> allCrewMember = findAllCrewMember(sheets, GOOGLE_SHEET_ID);
      for (String member : members) {
        Integer row = allCrewMember.get(member);
        if (row == null) {
          log.warn("없는 사람! " + member);
          continue;
        }
        String range = getWeekColumn(week) + row;
        writeToSheet(sheets, range, Attendance.ATTENDANCE.getScore(), GOOGLE_SHEET_ID);
      }
    } catch (Exception e) {
      log.error("Failed to write data to the spreadsheet", e);
      throw new RuntimeException("Failed to write data to the spreadsheet: " + e.getMessage(), e);
    }
  }

  private Map<String, Integer> findAllCrewMember(Sheets sheets, String spreadsheetId) throws IOException {
    // CrewMember의 이름은 B열에 기입된다.
    ValueRange response = sheets.spreadsheets().values()
        .get(spreadsheetId, "B2:B100")
        .execute();

    return getCrewMembers(response)
        .stream()
        .collect(toMap(CrewMember::name, CrewMember::row, (existing, replacement) -> existing));
  }

  private List<CrewMember> getCrewMembers(ValueRange response) {
    List<CrewMember> crewMembers = new ArrayList<>();
    List<List<Object>> values = response.getValues();
    for (int i = 0; i < values.size(); i++) {
      List<Object> objects = values.get(i);
      if (objects == null || objects.isEmpty()) {
        // 더 이상 스프레드시트에 CrewMember가 없을 때 조회를 멈춘다.
        break;
      }
      crewMembers.add(new CrewMember(i + START_CREW_MEMBER_ROW, (String) objects.getFirst()));
    }
    return crewMembers;
  }

  private String getWeekColumn(int week) {
    return Character.toString((char) (START_WEEK_COLUMN + week));
  }

  private void writeToSheet(Sheets sheets, String range, String word, String spreadsheetId) throws IOException {
    List<List<Object>> values = List.of(Collections.singletonList(word));
    ValueRange body = new ValueRange().setValues(values);
    UpdateValuesResponse result = sheets.spreadsheets().values()
        .update(spreadsheetId, range, body)
        .setValueInputOption("USER_ENTERED")
        .execute();
    log.info("Updated rows: {}", result.getUpdatedRows());
  }

  private Sheets getSheets() throws IOException, GeneralSecurityException {
    if (GOOGLE_SHEET_ID == null || GOOGLE_SHEET_ID.isBlank()) {
      log.error("GOOGLE_SHEET_ID 환경 변수가 설정되지 않았습니다.");
      throw new IllegalStateException("GOOGLE_SHEET_ID 환경 변수가 설정되지 않았습니다.");
    }

    if (sheets == null) {
      GoogleCredentials credentials = GoogleCredentials.fromStream(new ClassPathResource(CREDENTIALS_FILE_PATH).getInputStream())
          // Google API를 호출할 떄 필요한 권한을 지정하는 부분 , 읽기/쓰기 권한을 나타냄
          .createScoped(Collections.singletonList("https://www.googleapis.com/auth/spreadsheets"));
      sheets = new Sheets.Builder(GoogleNetHttpTransport.newTrustedTransport(), JSON_FACTORY, new HttpCredentialsAdapter(credentials))
          .setApplicationName(APPLICATION_NAME)
          .build();
    }
    return sheets;
  }

  public FindMeSheetDto findMeSheetAttendance(String member) {
    try {
      Sheets sheets = getSheets();
      Map<String, Integer> allCrewMember = findAllCrewMember(sheets, GOOGLE_SHEET_ID);
      Integer row = allCrewMember.get(member);
      if (row == null) {
        log.warn("없는 사람! " + member);
        return null;
      }
      ValueRange response = sheets.spreadsheets().values()
          .get(GOOGLE_SHEET_ID, "D" + row + ":NN" + row)
          .execute();

      List<String> values = response.getValues().getFirst().stream()
          .map(Object::toString)
          .toList();
      // find CrewMember by member
      // List<CrewMember> crewMembers = getCrewMembers(response);
      // CrewMember me = crewMembers.stream()
      //     .filter(crewMember -> crewMember.name().equals(member))
      //     .findFirst()
      //     .orElseThrow(() -> new RuntimeException("Failed to find me from the spreadsheet"));
      return new FindMeSheetDto(FindMeCrewMember.of(row, member, values));
    } catch (Exception e) {
      log.error("Failed to find data from the spreadsheet", e);
      throw new RuntimeException("Failed to find data from the spreadsheet: " + e.getMessage(), e);
    }
  }

  public void saveHangoutAttendance(final String activity, final String name, final String attendance) {
    if (GOOGLE_SHEET_ID == null || GOOGLE_SHEET_ID.isBlank()) {
      log.error("GOOGLE_SHEET_ID 값이 null 또는 비어 있습니다.");
      return;
    }

    if (name == null || name.isEmpty()) {
      log.warn("이름이 입력되지 않았습니다. 활동={}, 참석여부={}", activity, attendance);
      return;
    }
    try {
      Sheets sheets = getSheets();

      // 시트 존재 여부 확인
      ensureSheetExists(sheets, GOOGLE_SHEET_ID, HANGOUT_SHEET_NAME);

      Map<String, Integer> allCrewMember = findAllCrewMember(sheets, GOOGLE_SHEET_ID);

      Integer row = allCrewMember.get(name);
      if (row == null) {
        log.warn("해당 이름을 찾을 수 없습니다: {}", name);
        return;
      }

      // "Hangout" 시트의 위치 설정
      String range = String.format("%s!C%d:E%d", HANGOUT_SHEET_NAME, row, row);

      // 기록할 데이터 준비
      List<List<Object>> values = List.of(List.of(activity, name, attendance));

      ValueRange body = new ValueRange().setValues(values);
      UpdateValuesResponse result = sheets.spreadsheets().values().update(GOOGLE_SHEET_ID, range, body).setValueInputOption("USER_ENTERED").execute();

      log.info("뒷풀이 참석 정보가 기록되었습니다. Updated rows: {}", result.getUpdatedRows());
    } catch (Exception e) {
      log.error("Failed to save hangout attendance", e);
      throw new RuntimeException("Failed to save hangout attendance: " + e.getMessage(), e);
    }
  }

  private void ensureSheetExists(final Sheets sheets, final String spreadsheetId, final String sheetName) throws IOException {
    List<String> sheetNames = sheets.spreadsheets().get(spreadsheetId).execute().getSheets()
            .stream()
            .map(sheet -> sheet.getProperties().getTitle())
            .toList();

    if (!sheetNames.contains(sheetName)) {
      log.error("Sheet '{}' does not exist in spreadsheet '{}'.", sheetName, spreadsheetId);
      throw new IllegalStateException("Sheet '" + sheetName + "' does not exist.");
    }
    log.info("Sheet exist in spreadsheet '{}'.", sheetName);
  }
}