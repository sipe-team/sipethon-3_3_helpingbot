package com.sipe.slack.helping.sheet.application;

import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.json.jackson2.JacksonFactory;
import com.google.api.services.sheets.v4.Sheets;
import com.google.api.services.sheets.v4.model.UpdateValuesResponse;
import com.google.api.services.sheets.v4.model.ValueRange;
import com.google.auth.http.HttpCredentialsAdapter;
import com.google.auth.oauth2.GoogleCredentials;
import java.io.IOException;
import java.security.GeneralSecurityException;
import java.util.Collections;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class SheetService {

  private static final String APPLICATION_NAME = "Google Sheets Application";
  private static final String CREDENTIALS_FILE_PATH = "/googlesheet/google.json";
  private static final JacksonFactory JSON_FACTORY = JacksonFactory.getDefaultInstance();

  private Sheets sheets;

  public void writeToSheet(String spreadsheetId, String range, List<List<Object>> values) {
    try {
      Sheets service = getSheets();
      ValueRange body = new ValueRange().setValues(values);
      UpdateValuesResponse result = service.spreadsheets().values()
          .update(spreadsheetId, range, body)
          .setValueInputOption("USER_ENTERED")
          .execute();
      log.info("Updated rows: {}", result.getUpdatedRows());
    } catch (Exception e) {
      log.error("Failed to write data to the spreadsheet", e);
      throw new RuntimeException("Failed to write data to the spreadsheet: " + e.getMessage(), e);
    }
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
}
