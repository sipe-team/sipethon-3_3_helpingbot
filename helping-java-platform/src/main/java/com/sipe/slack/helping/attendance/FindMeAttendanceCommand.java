package com.sipe.slack.helping.attendance;

import static com.slack.api.model.block.Blocks.*;
import static com.slack.api.model.block.composition.BlockCompositions.*;
import static com.slack.api.model.block.element.BlockElements.*;

import com.sipe.slack.helping.sheets.SheetsService;
import com.slack.api.bolt.handler.builtin.SlashCommandHandler;
import com.slack.api.methods.SlackApiException;
import com.slack.api.methods.response.users.UsersInfoResponse;
import com.slack.api.methods.response.users.profile.UsersProfileGetResponse;
import com.slack.api.model.block.LayoutBlock;
import com.slack.api.model.view.View;
import com.slack.api.model.view.*;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class FindMeAttendanceCommand {

	private final SheetsService sheetsService;

	// 본인 출석확인
	public SlashCommandHandler findMeAttendanceCommand(String botToken) {
		return (req, ctx) -> {
			try {
				// userId
				String userId = req.getPayload().getUserId();

				UsersInfoResponse response = ctx.client().usersInfo(r -> r.user(userId));
				String realName = response.getUser().getRealName();
				if (realName.contains("3기")) {
					realName = realName.substring(3);
				}

				// Find attendance information
				FindMeSheetDto findMeSheetDto = sheetsService.findMeSheetAttendance(realName);

				// Open a modal with the attendance information
				ctx.client().viewsOpen(r -> r
					.triggerId(req.getPayload().getTriggerId())
					.view(buildAttendanceView(findMeSheetDto))
				);
			} catch (IOException | SlackApiException e) {
				log.error("Error handling submit command", e);
			}
			return ctx.ack();
		};
	}

	private View buildAttendanceView(FindMeSheetDto findMeSheetDto) {
		List<LayoutBlock> blocks = new ArrayList<>();
		blocks.add(section(s -> s.text(markdownText("*" + findMeSheetDto.crewMember().name() + "* 님의 출석 여부입니다."))));
		blocks.add(divider());

		// Add total score section
		int exclusiveScore = findMeSheetDto.crewMember().scores().stream()
			.mapToInt(score -> {
				if (score.equals("5")) return 5;
				if (score.equals("0")) return 10;
				return 0;
			})
			.sum();

		// Add total score section
		int totalScore = findMeSheetDto.crewMember().scores().stream()
			.mapToInt(score -> {
				return switch (score) {
					case "10" -> 10;
					case "5" -> 5;
					default -> 0;
				};
			})
			.sum();

		blocks.add(section(s -> s.text(markdownText("현재 총 점수: " + totalScore))));

		// Calculate the total score for 지각 and 결석
		blocks.add(section(s -> s.text(markdownText("출석 횟수: " + findMeSheetDto.crewMember().scores().stream().filter(score -> score.equals("10")).count()))));
		blocks.add(section(s -> s.text(markdownText("지각 횟수: " + findMeSheetDto.crewMember().scores().stream().filter(score -> score.equals("5")).count()))));
		blocks.add(section(s -> s.text(markdownText("결석 횟수: " + findMeSheetDto.crewMember().scores().stream().filter(score -> score.equals("0")).count()))));


		// Determine if the total score is 30 or more
		String expulsionStatus = exclusiveScore >= 30 ? "Y" : "N";
		blocks.add(section(s -> s.text(markdownText("제명 대상 여부: " + expulsionStatus))));
		blocks.add(divider());

		// Add sections for each week
		for (int week = 1; week < findMeSheetDto.crewMember().scores().size(); week++) {
			blocks.add(createWeekSection(week, findMeSheetDto));
		}

		return View.builder()
			.type("modal")
			.title(ViewTitle.builder().type("plain_text").text("출석 여부 확인").build())
			.close(ViewClose.builder().type("plain_text").text("닫기").build())
			.blocks(blocks)
			.build();
	}

	private LayoutBlock createWeekSection(int week, FindMeSheetDto findMeSheetDto) {
		String attendanceStatus = getAttendanceStatus(findMeSheetDto.crewMember().scores().get(week));
		return section(s -> s.text(markdownText(week + "주차: " + attendanceStatus)));
	}

	private String getAttendanceStatus(String score) {
		if (score.equals("10")) {
			return "출석";
		} else if (score.equals("5")) {
			return "지각";
		} else {
			return "결석";
		}
	}
}