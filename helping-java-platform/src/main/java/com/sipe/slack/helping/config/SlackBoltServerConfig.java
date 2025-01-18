package com.sipe.slack.helping.config;

import com.sipe.slack.helping.MissionSubmit;
import com.sipe.slack.helping.attendance.FindMeAttendanceCommand;
import com.slack.api.bolt.App;
import com.slack.api.bolt.AppConfig;
import com.slack.api.bolt.socket_mode.SocketModeApp;
import com.slack.api.methods.SlackApiException;
import com.slack.api.methods.response.users.UsersIdentityResponse;
import com.slack.api.socket_mode.SocketModeClient;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.event.ContextStartedEvent;
import org.springframework.context.event.EventListener;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;

@Slf4j
@Configuration
@RequiredArgsConstructor
public class SlackBoltServerConfig {

	private final MissionSubmit missionSubmit;
	private final FindMeAttendanceCommand findAttendanceCommand;

	@Bean
	@EventListener(ContextStartedEvent.class)
	public SocketModeApp startSocketModeApp() throws Exception {
		log.info("Start socket mode slack bolt app server.");
		String botToken = System.getenv("SLACK_BOT_TOKEN");
		String appToken = System.getenv("SLACK_APP_TOKEN");
		String signingSecret = System.getenv("SLACK_SIGNING_SECRET");
<<<<<<< Updated upstream
		log.info("Bot Token: {}", botToken);
		log.info("App Token: {}", appToken);
		log.info("Signing Secret: {}", signingSecret);
=======

>>>>>>> Stashed changes
		App app = new App(AppConfig.builder()
			.singleTeamBotToken(botToken)
			.signingSecret(signingSecret)
			.build());

		app.command("/문장제출", missionSubmit.handleSubmitCommand());
		app.viewSubmission("submit_view", missionSubmit.handleViewSubmission());
		app.command("/제출내역", missionSubmit.handleSubmissionHistoryCommand());
		app.command("/관리자", missionSubmit.handleAdminCommand());
		app.blockAction("fetch_all_submissions", missionSubmit.handleFetchAllSubmissions());
		app.command("/출석", missionSubmit.testHandler());

		// 본인 출석여부 확인 커맨드
		app.command("/출석여부", findAttendanceCommand.findMeAttendanceCommand(botToken));

		SocketModeApp socketModeApp = new SocketModeApp(appToken, SocketModeClient.Backend.JavaWebSocket, app);
		socketModeApp.startAsync();
		return socketModeApp;
	}

}