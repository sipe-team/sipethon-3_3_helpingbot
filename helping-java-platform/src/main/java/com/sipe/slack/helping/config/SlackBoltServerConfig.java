package com.sipe.slack.helping.config;

import com.sipe.slack.helping.MissionSubmit;
import com.slack.api.bolt.App;
import com.slack.api.bolt.AppConfig;
import com.slack.api.bolt.socket_mode.SocketModeApp;
import com.slack.api.socket_mode.SocketModeClient;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.event.ContextStartedEvent;
import org.springframework.context.event.EventListener;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Configuration
public class SlackBoltServerConfig {

	private final MissionSubmit missionSubmit;

	public SlackBoltServerConfig(MissionSubmit missionSubmit) {
		this.missionSubmit = missionSubmit;
	}

	@Bean
	@EventListener(ContextStartedEvent.class)
	public SocketModeApp startSocketModeApp() throws Exception {
		log.info("Start socket mode slack bolt app server.");
		String botToken = System.getenv("SLACK_BOT_TOKEN");
		String appToken = System.getenv("SLACK_APP_TOKEN");
		String signingSecret = System.getenv("SLACK_SIGNING_SECRET");
		// log.info("Bot Token: {}", botToken);
		// log.info("App Token: {}", appToken);
		// log.info("Signing Secret: {}", signingSecret);
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

		SocketModeApp socketModeApp = new SocketModeApp(appToken, SocketModeClient.Backend.JavaWebSocket, app);
		socketModeApp.startAsync();
		return socketModeApp;
	}
}