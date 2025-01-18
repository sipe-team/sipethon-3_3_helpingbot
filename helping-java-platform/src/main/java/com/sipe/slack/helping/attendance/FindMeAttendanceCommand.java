package com.sipe.slack.helping.attendance;

import static com.slack.api.model.block.Blocks.*;
import static com.slack.api.model.block.composition.BlockCompositions.*;

import com.sipe.slack.helping.sheets.SheetsService;
import com.slack.api.bolt.handler.builtin.SlashCommandHandler;
import com.slack.api.methods.SlackApiException;
import com.slack.api.methods.response.users.UsersInfoResponse;
import com.slack.api.methods.response.users.profile.UsersProfileGetResponse;
import com.slack.api.model.view.View;
import com.slack.api.model.view.*;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.io.IOException;

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
				System.out.println("displayName: " + displayName);
				// Find attendance information
				FindMeSheetDto findMeSheetDto = sheetsService.findMeSheetAttendance(displayName);

				// Open a modal with the attendance information
				ctx.client().viewsOpen(r -> r
					.triggerId(req.getPayload().getTriggerId())
					.view(buildAttendanceView(req.getPayload().getChannelId(), findMeSheetDto))
				);
			} catch (IOException | SlackApiException e) {
				log.error("Error handling submit command", e);
			}
			return ctx.ack();
		};
	}

	private View buildAttendanceView(String channelId, FindMeSheetDto findMeSheetDto) {
		return View.builder()
			.type("modal")
			.title(ViewTitle.builder().type("plain_text").text("출석 여부 확인").build())
			.close(ViewClose.builder().type("plain_text").text("닫기").build())
			.blocks(asBlocks(
				section(s -> s.text(markdownText("*" + findMeSheetDto.crewMember().name() + "*님의 출석 여부입니다."))),
				divider(),
				section(s -> s.text(markdownText("이번 주 출석 여부: o")))
			))
			.build();
	}
}