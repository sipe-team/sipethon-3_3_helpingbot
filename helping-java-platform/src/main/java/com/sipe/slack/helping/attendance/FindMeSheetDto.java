package com.sipe.slack.helping.attendance;

import java.util.List;

import com.sipe.slack.helping.sheets.dto.CrewMember;


public record FindMeSheetDto(
	CrewMember crewMember
) {
	// static method of
	private static FindMeSheetDto of(CrewMember crewMember) {
		return new FindMeSheetDto(crewMember);
	}
}
