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
    log.info("Find me from the spreadsheet: {}", member);
    try {
      Sheets sheets = getSheets();
      Map<String, Integer> allCrewMember = findAllCrewMember(sheets, GOOGLE_SHEET_ID);
      Integer row = allCrewMember.get(member);
      if (row == null) {
        log.warn("없는 사람! " + member);
        return null;
      }
      ValueRange response = sheets.spreadsheets().values()
          .get(GOOGLE_SHEET_ID, "D" + row + ":ZZ" + row)
          .execute();
      // find CrewMember by member
      List<CrewMember> crewMembers = getCrewMembers(response);
      CrewMember me = crewMembers.stream()
          .filter(crewMember -> crewMember.name().equals(member))
          .findFirst()
          .orElseThrow(() -> new RuntimeException("Failed to find me from the spreadsheet"));
      return new FindMeSheetDto(me);
    } catch (Exception e) {
      log.error("Failed to find data from the spreadsheet", e);
      throw new RuntimeException("Failed to find data from the spreadsheet: " + e.getMessage(), e);
    }
  }
}