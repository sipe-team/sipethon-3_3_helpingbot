package com.sipe.slack.helping.attendance;

public record FindMeSheetDto(
	FindMeCrewMember crewMember
) {
	// static method of
	private static FindMeSheetDto of(FindMeCrewMember crewMember) {
		return new FindMeSheetDto(crewMember);
	}
}
