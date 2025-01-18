package com.sipe.slack.helping.sheets.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

@AllArgsConstructor
public enum Attendance {
  ATTENDANCE("10"), PERCEPTION("5"), ABSENCE("0");

  @Getter
  final String score;
}
