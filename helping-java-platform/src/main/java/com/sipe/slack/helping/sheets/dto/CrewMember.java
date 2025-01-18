package com.sipe.slack.helping.sheets.dto;

import java.util.List;

public record CrewMember(
    int row,
    String name
) {
	public static CrewMember of(int row, String name) {
		return new CrewMember(row, name);
	}
}
