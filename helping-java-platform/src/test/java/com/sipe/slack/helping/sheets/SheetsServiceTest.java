package com.sipe.slack.helping.sheets;

import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class SheetsServiceTest {


  public static final String SPREADSHEET_ID = "1xxCXFMNSWBQaw1OIgnNdIFfWYRRQTQiquj6jlpzjk44";
  private SheetsService sheetService;

  @BeforeEach
  void setUp() {
    sheetService = new SheetsService();
  }

  @Test
  public void 출석체크를_한다() {
    sheetService.attendance(List.of("허정화", "문준용", "차윤범", "메롱"), 5, SPREADSHEET_ID);
  }
}