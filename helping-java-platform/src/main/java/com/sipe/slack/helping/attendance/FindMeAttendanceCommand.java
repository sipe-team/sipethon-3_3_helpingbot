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

				// Get user profile info from Slack API
				UsersProfileGetResponse usersProfileGetResponse = ctx.client().usersProfileGet(r -> r.token(botToken).user(userId));
				if (!usersProfileGetResponse.isOk()) {
					log.error("Error fetching user profile: {}", usersProfileGetResponse.getError());
					return ctx.ack();
				}

				String displayName = usersProfileGetResponse.getProfile().getDisplayName();

				// displayName Example: 3기_차윤범, "3기_"를 삭제
				displayName = displayName.substring(3);
				// Find attendance information
				FindMeSheetDto findMeSheetDto = sheetsService.findMeSheetAttendance(displayName);

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

		// Add attendance count section
		blocks.add(section(s -> s.text(markdownText("출석 횟수: " + findMeSheetDto.crewMember().scores().stream().filter(score -> score.equals("10")).count()))));
		blocks.add(section(s -> s.text(markdownText("지각 횟수: " + findMeSheetDto.crewMember().scores().stream().filter(score -> score.equals("5")).count()))));
		blocks.add(section(s -> s.text(markdownText("결석 횟수: " + findMeSheetDto.crewMember().scores().stream().filter(score -> score.equals("0")).count()))));
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