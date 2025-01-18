package com.sipe.slack.helping.attendance;

import java.util.List;

public record FindMeCrewMember(
	int row,
	String name,
	List<String> scores
) {
	public static FindMeCrewMember of(int row, String name, List<String> scores) {
		return new FindMeCrewMember(row, name, scores);
	}
}
